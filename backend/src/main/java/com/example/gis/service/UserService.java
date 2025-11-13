package com.example.gis.service;

import com.example.gis.dto.CreateUserRequest;
import com.example.gis.dto.UpdateUserRequest;
import com.example.gis.dto.UserDto;
import com.example.gis.entity.User;
import com.example.gis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findByDeletedAtIsNull(pageable)
                .map(this::toDto);
    }

    public UserDto findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    public UserDto findByUsername(String username) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    public User findUserEntityByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole())
                .createdBy(getCurrentUser())
                .build();

        user = userRepository.save(user);
        log.info("Created user: {}", user.getUsername());
        return toDto(user);
    }

    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        user.setUpdatedBy(getCurrentUser());

        user = userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
        return toDto(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent self-deletion
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new RuntimeException("Cannot delete your own account");
        }

        user.setDeletedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);
        log.info("Soft deleted user: {}", user.getUsername());
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedBy(getCurrentUser());
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    @Transactional
    public void changePasswordByAdmin(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedBy(getCurrentUser());
        userRepository.save(user);
        log.info("Password changed by admin for user: {}", user.getUsername());
    }

    @Transactional
    public void updateRole(UUID id, User.UserRole newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent changing own role
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new RuntimeException("Cannot change your own role");
        }

        user.setRole(newRole);
        user.setUpdatedBy(getCurrentUser());
        userRepository.save(user);
        log.info("Role updated for user {}: {}", user.getUsername(), newRole);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return userRepository.findByUsernameAndDeletedAtIsNull(auth.getName())
                    .orElse(null);
        }
        return null;
    }
}

