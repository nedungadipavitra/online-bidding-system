package com.onlinebidding.user_service.service;

import java.util.List;
import com.onlinebidding.user_service.dto.UserDto;

public interface UserService {
    List<UserDto> getAllUsers();
    UserDto getUserById(Long id);
    UserDto updateUser(Long id, UserDto userDto);
    void deleteUser(Long id);
}
