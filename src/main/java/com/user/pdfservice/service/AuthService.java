package com.user.pdfservice.service;

import com.user.pdfservice.model.entity.GlobalSettings;
import com.user.pdfservice.model.entity.User;
import com.user.pdfservice.repository.GlobalSettingsRepository;
import com.user.pdfservice.repository.UserRepository;
import com.user.pdfservice.security.JwtUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final GlobalSettingsRepository globalSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       GlobalSettingsRepository globalSettingsRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.globalSettingsRepository = globalSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Seeds initial Admin and default GlobalSettings if database is fresh.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDatabaseDefaults() {
        try {
            if (userRepository.countByRole("ADMIN") == 0) {
                User defaultAdmin = new User();
                defaultAdmin.setUsername("admin");
                defaultAdmin.setPassword(passwordEncoder.encode("Admin@12345"));
                defaultAdmin.setRole("ADMIN");
                defaultAdmin.setEnabled(true);
                defaultAdmin.setDisplayName("System Administrator");
                defaultAdmin.setCreatedAt(LocalDateTime.now());
                userRepository.save(defaultAdmin);
                System.out.println(">>> [RBAC] Initialized default admin account: 'admin'");
            }

            if (globalSettingsRepository.count() == 0) {
                GlobalSettings settings = new GlobalSettings(false, "SYSTEM");
                globalSettingsRepository.save(settings);
                System.out.println(">>> [RBAC] Initialized default global settings");
            }
        } catch (Exception e) {
            System.err.println(">>> [RBAC] Warning: Could not initialize database defaults: " + e.getMessage());
        }
    }

    /**
     * Authenticates user and enforces single-device login constraint for normal users.
     */
    public Map<String, Object> login(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password must be provided");
        }

        String cleanUsername = username.trim();
        Optional<User> userOpt = userRepository.findByUsername(cleanUsername);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Account Enabled Check
        if (!user.isEnabled()) {
            throw new IllegalStateException("Your account has been disabled. Please contact the administrator.");
        }

        // Single-Device Login Enforcement for Normal Users
        if ("USER".equalsIgnoreCase(user.getRole())) {
            if (user.getActiveSessionId() != null && !user.getActiveSessionId().trim().isEmpty()) {
                throw new IllegalStateException(
                        "This account is already logged in on another device. Please logout from that device first or contact administrator."
                );
            }
        }

        // Generate unique session token & ID
        String sessionId = UUID.randomUUID().toString();
        user.setActiveSessionId(sessionId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        response.put("role", user.getRole());
        response.put("sessionId", sessionId);

        return response;
    }

    /**
     * Logs out user by clearing the active session ID in MongoDB.
     */
    public void logout(String username) {
        if (username == null) return;
        userRepository.findByUsername(username.trim()).ifPresent(user -> {
            user.setActiveSessionId(null);
            userRepository.save(user);
        });
    }

    /**
     * Validates user and session token for active authorization.
     */
    public User validateSession(String token) {
        if (token == null || !jwtUtils.isTokenValid(token)) {
            return null;
        }

        String username = jwtUtils.getUsername(token);
        String sessionId = jwtUtils.getSessionId(token);

        if (username == null) return null;

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return null;

        User user = userOpt.get();
        if (!user.isEnabled()) return null;

        // Verify session ID matches DB for single device constraint
        if ("USER".equalsIgnoreCase(user.getRole())) {
            if (user.getActiveSessionId() == null || !user.getActiveSessionId().equals(sessionId)) {
                return null; // Invalidated because another device or logout cleared session
            }
        }

        return user;
    }
}

