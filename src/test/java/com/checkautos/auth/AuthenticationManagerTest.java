package com.checkautos.auth;

import com.checkautos.models.User;
import com.checkautos.repository.local.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationManagerTest {

    private InMemoryUserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authManager;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        authManager = new AuthenticationManager(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Debe autenticar correctamente un usuario precargado con BCrypt")
    void testLoginSuccess() {
        // "ander@checkautos.com" con contraseña "1234"
        boolean loginOk = authManager.login("ander@checkautos.com", "1234");
        assertTrue(loginOk, "Debe iniciar sesión correctamente con credenciales válidas");
    }

    @Test
    @DisplayName("Debe rechazar login con contraseña incorrecta")
    void testLoginFailure() {
        boolean loginFail = authManager.login("ander@checkautos.com", "wrongpassword");
        assertFalse(loginFail, "No debe autenticar si la contraseña es incorrecta");
    }

    @Test
    @DisplayName("Debe rechazar la creación de cuenta con contraseñas menores a 8 caracteres")
    void testPasswordLengthValidation() {
        boolean shortPass = authManager.createAccount("Carlos", "carlos@checkautos.com", "1234567");
        assertFalse(shortPass, "No debe permitir contraseñas de menos de 8 caracteres");

        boolean validPass = authManager.createAccount("Carlos", "carlos@checkautos.com", "12345678");
        assertTrue(validPass, "Debe permitir contraseñas de 8 o más caracteres");
    }

    @Test
    @DisplayName("Debe rechazar la creación de cuenta con email ya registrado")
    void testDuplicateEmail() {
        boolean duplicate = authManager.createAccount("Ander Clone", "ander@checkautos.com", "password123");
        assertFalse(duplicate, "No debe permitir registrar un email existente");
    }

    @Test
    @DisplayName("Debe buscar usuario por email indexado usando findByEmail")
    void testFindByEmail() {
        Optional<User> found = authManager.findByEmail("ander@checkautos.com");
        assertTrue(found.isPresent());
        assertEquals("Ander", found.get().getNombre());

        Optional<User> notFound = authManager.findByEmail("noexiste@checkautos.com");
        assertTrue(notFound.isEmpty());
    }
}
