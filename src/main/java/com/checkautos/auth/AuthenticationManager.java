package com.checkautos.auth;

import com.checkautos.models.User;
import com.checkautos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationManager {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.accounts:}") private String adminAccountsConfig;

    public AuthenticationManager(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @jakarta.annotation.PostConstruct
    private void initializeDefaultAccounts() {
        if (adminAccountsConfig == null || adminAccountsConfig.isBlank()) return;
        for (String entry : adminAccountsConfig.split(";")) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                crearSiNoExiste(parts[0].trim(), parts[1].trim(), parts[2].trim(), "ADMIN");
            }
        }
    }

    private void crearSiNoExiste(String nombre, String email, String password, String rol) {
        if (!userRepository.existsByEmail(email)) {
            User u = new User(nombre, email, passwordEncoder.encode(password), rol);
            userRepository.save(u);
        }
    }

    public boolean login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return false;
        Optional<User> found = userRepository.findByEmail(email.trim().toLowerCase());
        return found.isPresent() && passwordEncoder.matches(password.trim(), found.get().getPassword());
    }

    public boolean createAccount(String nombre, String email, String password) {
        return createAccount(nombre, email, password, "USUARIO");
    }

    public boolean createAccount(String nombre, String email, String password, String rol) {
        if (nombre == null || nombre.isBlank() ||
            email == null || email.isBlank() ||
            password == null || password.isBlank()) return false;
        if (password.length() < 8) return false;
        if (!email.contains("@") || !email.contains(".")) return false;
        if (userRepository.existsByEmail(email.trim().toLowerCase())) return false;

        User u = new User(nombre.trim(), email.trim().toLowerCase(),
                          passwordEncoder.encode(password.trim()), rol);
        userRepository.save(u);
        return true;
    }

    public void   logout()             {}
    public List<User> getAllAccounts() { return userRepository.findAll(); }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public Optional<User> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return userRepository.findById(id);
    }

    public boolean validatePassword(String password) {
        return password != null && password.length() >= 8;
    }

    public boolean usernameExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User guardarUsuario(User user) {
        return userRepository.save(user);
    }
}
