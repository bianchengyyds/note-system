package com.ai.service;

import com.ai.dto.LoginResponseDTO;
import com.ai.dto.UserDTO;

public interface AuthService {
    UserDTO register(UserDTO userDTO);

    LoginResponseDTO login(UserDTO userDTO);
}