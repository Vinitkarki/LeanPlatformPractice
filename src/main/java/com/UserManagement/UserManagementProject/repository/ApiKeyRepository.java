package com.UserManagement.UserManagementProject.repository;

import com.UserManagement.UserManagementProject.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey,Integer> {

    ApiKey findByApiKey(String apiKey);
}
