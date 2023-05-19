package com.UserManagement.UserManagementProject.service;

import com.UserManagement.UserManagementProject.dto.UserDto;
import com.UserManagement.UserManagementProject.entity.ApiKey;
import com.UserManagement.UserManagementProject.entity.Role;
import com.UserManagement.UserManagementProject.entity.User;
import com.UserManagement.UserManagementProject.repository.ApiKeyRepository;
import com.UserManagement.UserManagementProject.repository.RoleRepository;
import com.UserManagement.UserManagementProject.repository.UserRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@EnableCaching
public class UserServiceImpl implements UserService{

    @Autowired
    private RedisTemplate<String,Long> redisTemplate;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public UserServiceImpl(UserRepository userRepository,RoleRepository roleRepository,PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.roleRepository=roleRepository;
        this.passwordEncoder=passwordEncoder;
    }
    @Override
    public void saveUser(UserDto userDto) {
        User user=new User();
        user.setName(userDto.getFirstName()+" "+userDto.getLastName());
        user.setEmail(userDto.getEmail());

        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        String apikey=generateApiKey();


        ApiKey keyExist=apiKeyRepository.findByApiKey(apikey);
        if(keyExist!=null){
            boolean flag=true;
            while(flag){
                keyExist=apiKeyRepository.findByApiKey(apikey);
                if(keyExist!=null){
                    apikey=generateApiKey();
                }
                else {
                    flag=false;
                }
            }
        }

//        boolean flag=apiKeyRepository.existByApiKey(apikey);
//        if(flag){
//            while(flag){
//                if(apiKeyRepository.existByApiKey(apikey)){
//                    apikey=generateApiKey();
//                }
//                else{
//                    flag=false;
//                }
//            }
//        }


        user.setApiKey(apikey);

        ApiKey apiKeyObj=ApiKey.builder().apiKey(apikey).apiLimit(0).build();
        apiKeyRepository.save(apiKeyObj);

        Role role=roleRepository.findByName("ROLE_ADMIN");
        if(role==null){
            role=checkRoleExist();
        }
        user.setRoles(Arrays.asList(role));
        userRepository.save(user);
    }
    private String generateApiKey(){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 11;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    private Role checkRoleExist(){
        Role role=new Role();
        role.setName("ROLE_ADMIN");

        return roleRepository.save(role);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getUser() throws ExecutionException, InterruptedException {
//        List<User> users=userRepository.findAll();
//        return users.stream().map((user)->mapToUserDto(user)).collect(Collectors.toList());


        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
        String username=auth.getName();
        System.out.println(username);
        User user=userRepository.findByEmail(username);


        return user;
    }

    @Override
    public String delete() {

        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
        String username=auth.getName();
        System.out.println(username);

        User user=userRepository.findByEmail(username);
        String userApiKey=user.getApiKey();
        ApiKey apiKey=apiKeyRepository.findByApiKey(userApiKey);
        apiKeyRepository.delete(apiKey);

        for(Role role:user.getRoles()){
            role.getUsers().remove(user);
        }
        user.getRoles().clear();
        //Long id=user.getId();
        userRepository.delete(user);

        return "redirect:/login";
    }

    @Override
    public ResponseEntity<String> getContent(String methodType,String para,String apiKey) throws ExecutionException, InterruptedException {



        //getting the details of the currently logged in user
        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
        String username=auth.getName();
        //System.out.println(username);
        User user=userRepository.findByEmail(username);


        //matching the api key with the user
        if(!user.getApiKey().equals(apiKey)){
            return new ResponseEntity<String>("Invalid Api Key",HttpStatus.BAD_REQUEST);
        }

        Long maxApiLimit=redisTemplate.opsForValue().get("maxApiLimit");
        Long maxParaLength=redisTemplate.opsForValue().get("maxParaLength");
        Long minParaLength=redisTemplate.opsForValue().get("minParaLength");

        //fetching data from the firebase database
        Firestore dbFirestore= FirestoreClient.getFirestore();
        DocumentReference documentReference=dbFirestore.collection("firstFirebaseDb").document("data");
        ApiFuture<DocumentSnapshot> future=documentReference.get();
        DocumentSnapshot document=future.get();

        if(maxApiLimit==null){
            maxApiLimit=(long)document.get("maxApiLimit");
            redisTemplate.opsForValue().set("maxApiLimit",maxApiLimit);
        }
        if(maxParaLength==null){
            maxParaLength=(long)document.get("maxWordLength");
            redisTemplate.opsForValue().set("maxParaLength",maxParaLength);
        }
        if(minParaLength==null){
            minParaLength=(long)document.get("minWordLength");
            redisTemplate.opsForValue().set("minParaLength",minParaLength);
        }


//        long maxApiLimit=(long)document.get("maxApiLimit");
//        long maxParaLength=(long)document.get("maxWordLength");
//        long minParaLength=(long)document.get("minWordLength");


        //checking the number of the api hits by the user
        ApiKey userApiKey=apiKeyRepository.findByApiKey(apiKey);
        if(userApiKey.getApiLimit()>maxApiLimit){
            return new ResponseEntity<String>("You have reached maximum number of allowed api hits",HttpStatus.BAD_REQUEST);
        }

        List<String> words=(List<String>)document.get("abusiveWords");
        HashSet<String> set=new HashSet<>();

        for(int i=0;i<words.size();i++){
            set.add(words.get(i));
        }



        List<String> abusivePosition=new ArrayList<>();
        List<String> numberPosition=new ArrayList<>();

        String arr[]=para.split("[\\s,]+");


        //checking the max and min allowed length of the paragraph
        if(arr.length>maxParaLength || arr.length<minParaLength){
            return new ResponseEntity<String>("Invalid Para Length!",HttpStatus.BAD_REQUEST);
        }


        StringBuilder wholeContentWithAbusiveHighlight=new StringBuilder();
        StringBuilder contentWithNumber=new StringBuilder();

        for(int i=0;i<arr.length;i++){
            String word=arr[i];
            String wordLower=word.toLowerCase();
            if(!set.contains(wordLower)){
                wholeContentWithAbusiveHighlight.append(word);
                wholeContentWithAbusiveHighlight.append(" ");
            }
            else{
                String curr=word+","+String.valueOf(i+1);
                abusivePosition.add(curr);

                String abusiveHighLight=word+"(A)";
                wholeContentWithAbusiveHighlight.append(abusiveHighLight);
                wholeContentWithAbusiveHighlight.append(" ");
            }

            if(word.contains("0") || word.contains("1") || word.contains("2") || word.contains("3") || word.contains("4") || word.contains("5") || word.contains("6") || word.contains("7") || word.contains("8") || word.contains("9")){
                String numberPositionWord=word+","+String.valueOf(i+1);
                numberPosition.add(numberPositionWord);

                String numberHighlight=word+"(N)";
                contentWithNumber.append(numberHighlight);
                contentWithNumber.append(" ");
            }
            else{
                contentWithNumber.append(word);
                contentWithNumber.append(" ");
            }

        }




        switch (methodType){
            case "1":
                userApiKey.setApiLimit(userApiKey.getApiLimit()+1);
                apiKeyRepository.save(userApiKey);
                return new ResponseEntity<String>(abusivePosition.toString(), HttpStatus.OK);
            case "2":
                userApiKey.setApiLimit(userApiKey.getApiLimit()+1);
                apiKeyRepository.save(userApiKey);
                return new ResponseEntity<String>(wholeContentWithAbusiveHighlight.toString(), HttpStatus.OK);
            case  "3":
                userApiKey.setApiLimit(userApiKey.getApiLimit()+1);
                apiKeyRepository.save(userApiKey);
                return new ResponseEntity<String>(numberPosition.toString(), HttpStatus.OK);
            case "4":
                userApiKey.setApiLimit(userApiKey.getApiLimit()+1);
                apiKeyRepository.save(userApiKey);
                return new ResponseEntity<String>(contentWithNumber.toString(), HttpStatus.OK);
            default:
                throw new IllegalArgumentException("Invaid method type "+methodType);
        }
    }





//    private UserDto mapToUserDto(User user){
//        UserDto userDto=new UserDto();
//        String[] str=user.getName().split(" ");
//        userDto.setFirstName(str[0]);
//        userDto.setLastName(str[1]);
//        userDto.setEmail(user.getEmail());
//        userDto.setApiKey(user.getApiKey());
//        return userDto;
//    }
}
