package com.UserManagement.UserManagementProject.repository;

import com.UserManagement.UserManagementProject.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role,Long> {

    Role findByName(String name);
}
