package com.UserManagement.UserManagementProject.service;

import com.UserManagement.UserManagementProject.dto.UserDto;
import com.UserManagement.UserManagementProject.entity.User;

import java.util.List;

public interface UserService {

    void saveUser(UserDto userDto);

    User findUserByEmail(String email);

    List<UserDto> findAllUsers();
}
