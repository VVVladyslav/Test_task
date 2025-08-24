package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientRequest {

    @NotBlank
    @Size(min = 2, max = 200)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String address;
}