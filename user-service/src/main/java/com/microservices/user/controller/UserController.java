package com.microservices.user.controller;

import com.microservices.user.dto.UserRequest;
import com.microservices.user.dto.UserResponse;
import com.microservices.user.entity.User;
import com.microservices.user.exception.ResourceNotFoundException;
import com.microservices.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

// Some changes Added

public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

//    @GetMapping("/{id}")
//    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
//        UserResponse response = userService.getUserById(id);
//        return ResponseEntity.ok(response);
//
//    }
//
//    @GetMapping("/username/{username}")
//    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
//        UserResponse response = userService.getUserByUsername(username);
//        return ResponseEntity.ok(response);
//    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

//    @PutMapping("/{id}")
//    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
//                                                   @Valid @RequestBody UserRequest request) {
//        UserResponse response = userService.updateUser(id, request);
//        return ResponseEntity.ok(response);
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id,
                                                         @RequestParam User.UserStatus status) throws ResourceNotFoundException {
        UserResponse response = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(response);
    }


}
