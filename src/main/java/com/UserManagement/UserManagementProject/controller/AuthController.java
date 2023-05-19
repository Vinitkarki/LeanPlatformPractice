package com.UserManagement.UserManagementProject.controller;

import com.UserManagement.UserManagementProject.dto.UserDto;
import com.UserManagement.UserManagementProject.entity.User;
import com.UserManagement.UserManagementProject.service.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.ExecutionException;


@Controller
public class AuthController {

    @Autowired
    private RedisTemplate<String,Long> redisTemplate;

    private UserService userService;

    public AuthController(UserService userService){
        this.userService=userService;
    }

    @GetMapping("/")
    @RateLimiter(name="my-rate-limiter")
    public String home(){
        return "index";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model){

        UserDto user=new UserDto();
        model.addAttribute("user",user);
        return "register";
    }


    @PostMapping("/register/save")
    public String registration(@ModelAttribute("user") UserDto userDto, BindingResult result,Model model){

        User existingUser=userService.findUserByEmail(userDto.getEmail());

        if(existingUser!=null && existingUser.getEmail()!=null && !existingUser.getEmail().isEmpty()){
            result.rejectValue("email",null,"There is already an account registered with the same email");
        }

        if(result.hasErrors()){
            model.addAttribute("user",userDto);
            return "/register";
        }
        userService.saveUser(userDto);
        return "redirect:/register?success";
    }

    @GetMapping("/users")
    public String users(Model model) throws ExecutionException, InterruptedException {
//        List<UserDto> users=userService.findAllUsers();
        User user=userService.getUser();
        model.addAttribute("user",user);
        return "users";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/deleteUser")
    public String delete(){
        return userService.delete();
    }


    @PostMapping("/cleanContent")
    public ResponseEntity<String> getContent(@RequestParam("methodType")String methodType, @RequestParam("para") String para,@RequestParam("apiKey") String apiKey) throws ExecutionException, InterruptedException {
        return userService.getContent(methodType,para,apiKey);
    }



    @PostMapping("/clearCache")
    public ResponseEntity<?> clearCache(){

        try{
            redisTemplate.delete("maxApiLimit");
            redisTemplate.delete("maxParaLength");
            redisTemplate.delete("minParaLength");
            return new ResponseEntity<>("Cache cleared successfully", HttpStatus.OK);
        }
        catch (Exception e){
            System.out.println(e);
            return new ResponseEntity<>(e,HttpStatus.REQUEST_TIMEOUT);
        }

    }
}
