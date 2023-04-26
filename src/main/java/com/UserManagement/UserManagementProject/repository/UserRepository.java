package com.UserManagement.UserManagementProject.repository;

import com.UserManagement.UserManagementProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {

    User findByEmail(String email);
}
