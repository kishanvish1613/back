package com.user.pdfservice.controller;

import com.user.pdfservice.model.entity.GlobalSettings;
import com.user.pdfservice.model.entity.StatementRecord;
import com.user.pdfservice.model.entity.User;
import com.user.pdfservice.security.JwtUtils;
import com.user.pdfservice.service.AdminService;
import com.user.pdfservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final JwtUtils jwtUtils;

    public AdminController(AdminService adminService, AuthService authService, JwtUtils jwtUtils) {
        this.adminService = adminService;
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    private boolean verifyAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.substring(7);
        User user = authService.validateSession(token);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private String getUsernameFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return "admin";
        String token = authHeader.substring(7);
        String u = jwtUtils.getUsername(token);
        return u != null ? u : "admin";
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }

        try {
            String username = request.get("username");
            String password = request.get("password");
            String displayName = request.get("displayName");

            User created = adminService.createNormalUser(username, password, displayName);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to create user: " + e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> toggleUserStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") String userId,
            @RequestBody Map<String, Boolean> request) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }

        try {
            boolean enabled = request.getOrDefault("enabled", true);
            User updated = adminService.toggleUserStatus(userId, enabled);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-session")
    public ResponseEntity<?> resetUserSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") String userId) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }

        try {
            adminService.resetUserSession(userId);
            return ResponseEntity.ok(Collections.singletonMap("message", "User device session reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") String userId) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }

        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok(Collections.singletonMap("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getSettings());
    }

    @PostMapping("/settings/toggle-store-data")
    public ResponseEntity<?> toggleStoreData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Boolean> request) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }

        boolean enabled = request.getOrDefault("storeUserDataEnabled", false);
        String adminUser = getUsernameFromAuth(authHeader);
        GlobalSettings updated = adminService.toggleStoreUserData(enabled, adminUser);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/statements")
    public ResponseEntity<?> getStatements(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Admin access required"));
        }
        List<StatementRecord> records = adminService.getAllStatementRecords();
        return ResponseEntity.ok(records);
    }
}

