package com.UserManagement.UserManagementProject.service;

import com.UserManagement.UserManagementProject.dto.UserDto;
import com.UserManagement.UserManagementProject.entity.User;
import org.springframework.http.ResponseEntity;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface UserService {

    void saveUser(UserDto userDto);

    User findUserByEmail(String email);

    User getUser() throws ExecutionException, InterruptedException;

    String delete();

    ResponseEntity<String> getContent(String methodType, String para,String apiKey) throws ExecutionException, InterruptedException;
}
