package com.onlinebidding.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
	
	@NotBlank(message = "Name is required")
	private String name;
	
	@Email(message = "Invalid email")
	@NotBlank(message = "Email is required")
	private String email;
	
	@Size(min = 8, message = "Password must contain at least 8 characters")
	private String password;
	
	@NotBlank(message = "Role is required")
	private String role;
	
}
