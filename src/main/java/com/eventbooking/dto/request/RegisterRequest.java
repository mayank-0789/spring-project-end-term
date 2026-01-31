package com.eventbooking.dto.request;

import com.eventbooking.model.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    @Pattern(regexp = "^[0-9]{10}$")
    private String phone;

    @NotNull
    private UserRole role;
}
