package com.onlinebidding.user_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlinebidding.user_service.dto.UserDto;
import com.onlinebidding.user_service.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(@RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") Long id,
            @RequestBody UserDto userDto) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") Long id) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).build();
        }
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully.");
    }

    @GetMapping("/delivery")
    public ResponseEntity<List<UserDto>> getDeliveryPartners() {
        List<UserDto> deliveryPartners = userService.getAllUsers().stream()
                .filter(u -> com.onlinebidding.user_service.entity.Role.DELIVERY == u.getRole())
                .toList();
        return ResponseEntity.ok(deliveryPartners);
    }
}
