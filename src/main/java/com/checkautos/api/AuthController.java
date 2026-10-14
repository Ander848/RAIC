package com.checkautos.api;

import com.checkautos.AppContext;
import com.checkautos.dto.LoginRequest;
import com.checkautos.dto.RegistroRequest;
import com.checkautos.models.User;
import com.checkautos.security.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.lang.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppContext      appContext;
    private final JwtTokenService jwtTokenService;
    private final GoogleIdTokenVerifier googleVerifier;

    public AuthController(AppContext appContext,
                          JwtTokenService jwtTokenService,
                          @Nullable GoogleIdTokenVerifier googleVerifier) {
        this.appContext      = appContext;
        this.jwtTokenService = jwtTokenService;
        this.googleVerifier  = googleVerifier;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token de Google requerido"));
        }
        if (googleVerifier == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Google OAuth no configurado en el servidor"));
        }

        GoogleIdToken idToken;
        try {
            idToken = googleVerifier.verify(credential);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token de Google inválido"));
        }

        if (idToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No se pudo verificar el token de Google"));
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email    = payload.getEmail().toLowerCase();
        String nombre   = (String) payload.get("name");
        if (nombre == null || nombre.isBlank()) nombre = email.split("@")[0];

        // Buscar usuario por email o crearlo
        User user = appContext.getAuthManager().findByEmail(email).orElse(null);

        if (user == null) {
            // Crear cuenta nueva sin password (solo Google)
            user = new User(nombre, email, null, "USUARIO");
            user.setGoogleId(googleId);
            user.setProvider("google");
            user = appContext.getAuthManager().guardarUsuario(user);
        } else {
            // Actualizar googleId si es la primera vez que usa Google
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                if ("local".equals(user.getProvider()) || user.getProvider() == null) {
                    // mantener proveedor local si ya tenía contraseña
                }
                user = appContext.getAuthManager().guardarUsuario(user);
            }
        }

        String token = jwtTokenService.generateToken(email, user.getRol());
        Date expira  = jwtTokenService.getExpirationDate(token);

        return ResponseEntity.ok(Map.of(
                "token",   token,
                "tipo",    "Bearer",
                "expira",  expira.toString(),
                "usuario", Map.of(
                        "nombre",    user.getNombre(),
                        "email",     user.getEmail(),
                        "username",  user.getEmail(),
                        "iniciales", user.getInitials(),
                        "rol",       user.getRol()
                )
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String email = req.getEmail();
        String password = req.getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email y contraseña son requeridos"));
        }

        boolean ok = appContext.login(email.trim().toLowerCase(), password.trim());
        if (!ok) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Email o contraseña incorrectos"));
        }

        String emailNorm = email.trim().toLowerCase();
        User user = appContext.getAuthManager().findByEmail(emailNorm).orElse(null);
        if (user == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno"));
        }
        String token  = jwtTokenService.generateToken(emailNorm, user.getRol());
        Date   expira = jwtTokenService.getExpirationDate(token);

        return ResponseEntity.ok(Map.of(
                "token",   token,
                "tipo",    "Bearer",
                "expira",  expira.toString(),
                "usuario", Map.of(
                        "nombre",    user.getNombre(),
                        "email",     user.getEmail(),
                        "username",  user.getEmail(),
                        "iniciales", user.getInitials(),
                        "rol",       user.getRol()
                )
        ));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequest req) {
        String nombre   = req.getNombre();
        String email    = req.getEmail();
        String password = req.getPassword();

        if (nombre == null || nombre.isBlank() ||
            email == null || email.isBlank() ||
            password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "nombre, email y password son requeridos"));
        }

        boolean ok = appContext.createAccount(nombre.trim(), email.trim().toLowerCase(), password.trim());
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se pudo crear la cuenta. Verifica que el email no esté registrado y que la contraseña tenga al menos 8 caracteres"));
        }

        return ResponseEntity.status(201)
                .body(Map.of("mensaje", "Cuenta creada. Inicia sesión con tu email."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String email = authentication.getName();
        User user = appContext.getAuthManager().findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }

        return ResponseEntity.ok(Map.of(
                "nombre",    user.getNombre(),
                "email",     user.getEmail(),
                "username",  user.getEmail(),
                "iniciales", user.getInitials(),
                "rol",       user.getRol()
        ));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(Authentication authentication) {
        String email = authentication.getName();
        User solicitante = appContext.getAuthManager().findByEmail(email).orElse(null);

        if (solicitante == null || !solicitante.isAdmin()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Solo los administradores pueden ver la lista de usuarios"));
        }

        List<Map<String, String>> usuarios = appContext.getAuthManager().getAllAccounts()
                .stream()
                .map(u -> Map.of(
                        "id",       u.getId() != null ? u.getId() : "",
                        "nombre",   u.getNombre(),
                        "email",    u.getEmail(),
                        "rol",      u.getRol(),
                        "initials", u.getInitials()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(usuarios);
    }

    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable String id,
                                         @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        String emailAdmin = authentication.getName();
        User admin = appContext.getAuthManager().findByEmail(emailAdmin).orElse(null);

        if (admin == null || !admin.isAdmin()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Solo los administradores pueden cambiar roles"));
        }

        String nuevoRol = body.get("rol");
        if (!"ADMIN".equals(nuevoRol) && !"USUARIO".equals(nuevoRol)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rol inválido. Usa ADMIN o USUARIO"));
        }

        User target = appContext.getAuthManager().findById(id).orElse(null);

        if (target == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }

        target.setRol(nuevoRol);
        appContext.getAuthManager().guardarUsuario(target);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Rol actualizado a " + nuevoRol,
                "email",   target.getEmail(),
                "rol",     target.getRol()
        ));
    }
}
