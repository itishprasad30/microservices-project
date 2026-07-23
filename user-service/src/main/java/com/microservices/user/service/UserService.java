package com.microservices.user.service;

import com.microservices.user.dto.UserRequest;
import com.microservices.user.dto.UserResponse;
import com.microservices.user.entity.User;
import com.microservices.user.exception.ResourceNotFoundException;
import com.microservices.user.repository.UserRepository;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.persistence.Cacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user with username : {}",request.getUsername());
//        if(userRepository.existsByUsername(request.getUsername())){
//            throw new DuplicateResourceException("Username already exists: "+ request.getUsername());
//        }
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
//        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
//                .password(request.getPassword()) // In production, use password encoder
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        // Cache the user
        cacheUser(savedUser);
        return mapToResponse(savedUser);
    }

//    @Cacheable(value = "users", key = "#id")
//    public UserResponse getUserById(Long id) {
//        log.info("Fetching user by ID: {}", id);
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
//        return mapToResponse(user);
//    }

//    @Cacheable(value = "users", key = "#username")
//    public UserResponse getUserByUsername(String username) {
//        log.info("Fetching user by username: {}", username);
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
//        return mapToResponse(user);
//    }

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
    }

//    @Transactional
//    @CacheEvict(value = "users", key = "#id")
//    public UserResponse updateUser(Long id, UserRequest request) {
//        log.info("Updating user with ID: {}", id);
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
//
//        if (!user.getUsername().equals(request.getUsername()) &&
//                userRepository.existsByUsername(request.getUsername())) {
//            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
//        }
//
//        if (!user.getEmail().equals(request.getEmail()) &&
//                userRepository.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
//        }
//
//        user.setUsername(request.getUsername());
//        user.setEmail(request.getEmail());
//        user.setFullName(request.getFullName());
//        user.setPhoneNumber(request.getPhoneNumber());
//
//        User updatedUser = userRepository.save(user);
//        log.info("User updated with ID: {}", updatedUser.getId());
//
//        // Update cache
//        cacheUser(updatedUser);
//
//        return mapToResponse(updatedUser);
//    }

//    @Transactional
//    @CacheEvict(value = "users", key = "#id")
//    public void deleteUser(Long id) {
//        log.info("Deleting user with ID: {}", id);
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
//        userRepository.delete(user);
//        log.info("User deleted with ID: {}", id);
//
//        // Remove from Redis cache
//        redisTemplate.delete("users::" + id);
//    }

    @Transactional
    public UserResponse updateUserStatus(Long id, User.UserStatus status) throws ResourceNotFoundException {
        log.info("Updating user status: {} for user ID: {}", status, id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        log.info("User status updated for ID: {}", updatedUser.getId());

        // Update cache
        cacheUser(updatedUser);

        return mapToResponse(updatedUser);
    }

    private void cacheUser(User user) {
        String key = "users::" + user.getId();
        redisTemplate.opsForValue().set(key, mapToResponse(user), 1, TimeUnit.HOURS);

        String usernameKey = "users::" + user.getUsername();
        redisTemplate.opsForValue().set(usernameKey, mapToResponse(user), 1, TimeUnit.HOURS);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }


    public void deleteUser(Long id) {
    }
}
