package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClientRequest {

    @NotBlank
    @Size(min = 2, max = 200)
    private String name;

    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    @NotNull
    private Boolean active;
}