package com.UserManagement.UserManagementProject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ApiKey {


    @Id
    private String apiKey;

    private int apiLimit;
}
