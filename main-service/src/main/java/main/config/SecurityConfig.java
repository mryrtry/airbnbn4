package main.config;

import jakarta.servlet.http.HttpServletResponse;
import main.security.FilterErrorWriter;
import main.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final FilterErrorWriter errorWriter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, FilterErrorWriter errorWriter) {
        this.jwtFilter = jwtFilter;
        this.errorWriter = errorWriter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/ping").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/camunda/**").permitAll()
                        .requestMatchers("/forms/**").permitAll()
                        .requestMatchers("/lib/**", "/app/**", "/assets/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/", "/*.html").permitAll()
                        .requestMatchers("/engine-rest/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                errorWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN", "Access denied"))
                        .authenticationEntryPoint((request, response, authException) ->
                                errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "Authentication required"))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}