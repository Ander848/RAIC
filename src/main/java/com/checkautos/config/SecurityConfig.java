package com.checkautos.config;

import com.checkautos.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── Públicas (sin token) ──────────────────────────────
                .requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/google").permitAll()
                .requestMatchers("/api/config/public").permitAll()
                .requestMatchers("/", "/index.html", "/css/**", "/js/**",
                                 "/favicon.ico", "/*.html", "/*.js", "/*.css").permitAll()

                // ── Solo ADMIN ────────────────────────────────────────
                // Registrar auto (stateless REST)
                .requestMatchers(HttpMethod.POST,  "/api/autos").hasRole("ADMIN")
                // Registrar nuevo auto (iniciar proceso - retrocompatibilidad)
                .requestMatchers(HttpMethod.POST,  "/api/autos/nuevo").hasRole("ADMIN")
                // Actualizar datos del auto en edición
                .requestMatchers(HttpMethod.PUT,   "/api/autos/actual").hasRole("ADMIN")
                // Publicar auto
                .requestMatchers(HttpMethod.POST,  "/api/autos/publicar").hasRole("ADMIN")
                // Cambiar estado de un auto
                .requestMatchers(HttpMethod.PATCH, "/api/autos/*/estado").hasRole("ADMIN")
                // Archivar auto (soft delete)
                .requestMatchers(HttpMethod.PATCH, "/api/autos/*/archivar").hasRole("ADMIN")
                // Restaurar auto archivado
                .requestMatchers(HttpMethod.PATCH, "/api/autos/*/restaurar").hasRole("ADMIN")
                // Gestión de propietarios (crear/actualizar)
                .requestMatchers(HttpMethod.POST,  "/api/propietarios").hasRole("ADMIN")
                // Gestión de usuarios (listar y cambiar roles)
                .requestMatchers("/api/auth/usuarios/**").hasRole("ADMIN")
                // Valuación (calcular precio)
                .requestMatchers(HttpMethod.POST,  "/api/valuacion/**").hasRole("ADMIN")
                // Predicción — entrenar y reentrenar solo ADMIN
                .requestMatchers(HttpMethod.POST, "/api/prediccion/entrenar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/prediccion/reentrenar").hasRole("ADMIN")

                // ── ADMIN y USUARIO (solo lectura) ────────────────────
                // Ver todos los autos
                .requestMatchers(HttpMethod.GET, "/api/autos").hasAnyRole("ADMIN", "USUARIO")
                // Ver un auto por ID
                .requestMatchers(HttpMethod.GET, "/api/autos/*").hasAnyRole("ADMIN", "USUARIO")
                // Buscar autos
                .requestMatchers(HttpMethod.GET, "/api/autos/buscar").hasAnyRole("ADMIN", "USUARIO")
                // Estadísticas
                .requestMatchers(HttpMethod.GET, "/api/autos/stats").hasAnyRole("ADMIN", "USUARIO")
                // Autos archivados (solo lectura)
                .requestMatchers(HttpMethod.GET, "/api/autos/archivados").hasAnyRole("ADMIN", "USUARIO")
                // Buscar propietario por cédula (autocompletar)
                .requestMatchers(HttpMethod.GET, "/api/propietarios/**").hasAnyRole("ADMIN", "USUARIO")
                // Precio base por año
                .requestMatchers(HttpMethod.GET, "/api/valuacion/**").hasAnyRole("ADMIN", "USUARIO")
                // Predicción — predecir, estado y pesos para ADMIN y USUARIO
                .requestMatchers(HttpMethod.POST, "/api/prediccion/predecir").hasAnyRole("ADMIN", "USUARIO")
                .requestMatchers(HttpMethod.GET,  "/api/prediccion/**").hasAnyRole("ADMIN", "USUARIO")
                // Info del usuario autenticado
                .requestMatchers("/api/auth/me").hasAnyRole("ADMIN", "USUARIO")

                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
