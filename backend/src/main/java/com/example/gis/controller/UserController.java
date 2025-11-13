package com.example.gis.controller;

import com.example.gis.dto.ChangePasswordRequest;
import com.example.gis.dto.CreateUserRequest;
import com.example.gis.dto.UpdateUserRequest;
import com.example.gis.dto.UserDto;
import com.example.gis.entity.User;
import com.example.gis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management APIs (ADMIN only)")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "Get paginated list of all users (ADMIN only)")
    public ResponseEntity<Page<UserDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Get user details by ID (ADMIN only)")
    public ResponseEntity<UserDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by username", description = "Get user details by username (ADMIN only)")
    public ResponseEntity<UserDto> findByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (ADMIN only)")
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user details (ADMIN only)")
    public ResponseEntity<UserDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role", description = "Change user role (ADMIN only)")
    public ResponseEntity<UserDto> updateRole(
            @PathVariable UUID id,
            @RequestParam User.UserRole role) {
        userService.updateRole(id, role);
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user password (Admin)", description = "Admin can change any user's password (ADMIN only)")
    public ResponseEntity<Void> changePasswordByAdmin(
            @PathVariable UUID id,
            @RequestParam String newPassword) {
        userService.changePasswordByAdmin(id, newPassword);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Change own password", description = "Change current user's password")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findUserEntityByUsername(auth.getName());
        userService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Get current user", description = "Get current authenticated user's details")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(userService.findByUsername(auth.getName()));
    }
}

