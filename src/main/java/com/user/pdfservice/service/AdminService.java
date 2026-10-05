package com.user.pdfservice.service;

import com.user.pdfservice.model.entity.GlobalSettings;
import com.user.pdfservice.model.entity.StatementRecord;
import com.user.pdfservice.model.entity.User;
import com.user.pdfservice.repository.GlobalSettingsRepository;
import com.user.pdfservice.repository.StatementRecordRepository;
import com.user.pdfservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final GlobalSettingsRepository globalSettingsRepository;
    private final StatementRecordRepository statementRecordRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        GlobalSettingsRepository globalSettingsRepository,
                        StatementRecordRepository statementRecordRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.globalSettingsRepository = globalSettingsRepository;
        this.statementRecordRepository = statementRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Hide password hashes before sending
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public User createNormalUser(String username, String rawPassword, String displayName) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (rawPassword == null || rawPassword.trim().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }

        String cleanUsername = username.trim();
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("User with username '" + cleanUsername + "' already exists");
        }

        User user = new User();
        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(rawPassword.trim()));
        user.setRole("USER");
        user.setEnabled(true);
        user.setDisplayName(displayName != null && !displayName.trim().isEmpty() ? displayName.trim() : cleanUsername);
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

    public User toggleUserStatus(String userId, boolean enabled) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        user.setEnabled(enabled);

        // If disabling user, immediately kick out active session
        if (!enabled) {
            user.setActiveSessionId(null);
        }

        User saved = userRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

    public void resetUserSession(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();
        user.setActiveSessionId(null);
        userRepository.save(user);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public GlobalSettings getSettings() {
        return globalSettingsRepository.findById("global_settings")
                .orElseGet(() -> {
                    GlobalSettings s = new GlobalSettings(false, "SYSTEM");
                    return globalSettingsRepository.save(s);
                });
    }

    public GlobalSettings toggleStoreUserData(boolean enabled, String adminUsername) {
        GlobalSettings settings = getSettings();
        settings.setStoreUserDataEnabled(enabled);
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(adminUsername);
        return globalSettingsRepository.save(settings);
    }

    public boolean isStoreUserDataEnabled() {
        return getSettings().isStoreUserDataEnabled();
    }

    public StatementRecord saveStatementRecord(StatementRecord record) {
        return statementRecordRepository.save(record);
    }

    public List<StatementRecord> getAllStatementRecords() {
        return statementRecordRepository.findAllByOrderByGeneratedAtDesc();
    }
}

