package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to register a new user.
 * Represents the intent to create a user account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserCommand {
    
    private String email;
    private String password;
}
