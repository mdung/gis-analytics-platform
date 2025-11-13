package com.example.gis.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Email
    private String email;

    private com.example.gis.entity.User.UserRole role;
}

