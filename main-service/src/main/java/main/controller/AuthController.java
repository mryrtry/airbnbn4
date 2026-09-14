package main.controller;

import jakarta.validation.Valid;
import main.dto.*;
import main.security.JwtService;
import main.security.TokenBlacklistService;
import main.service.CamundaUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CamundaUserService userService;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    public AuthController(CamundaUserService userService,
                          JwtService jwtService,
                          TokenBlacklistService blacklistService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserInfo> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(userService.register(
                req.username(), req.password(), req.email(),
                req.firstName(), req.lastName()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        if (!userService.checkPassword(req.username(), req.password())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        List<String> groups = userService.getGroups(req.username());
        return ResponseEntity.ok(buildAuthResponse(req.username(), groups));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String username = jwtService.extractUsername(req.refreshToken());
        List<String> groups = jwtService.extractGroups(req.refreshToken());
        return ResponseEntity.ok(buildAuthResponse(username, groups));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String jti = jwtService.extractJti(token);
        Date exp = jwtService.extractExpiration(token);
        blacklistService.blacklist(jti, exp.toInstant());
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfo> me(Authentication auth) {
        return ResponseEntity.ok(userService.getInfo(auth.getName()));
    }

    @PostMapping("/become-owner")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfo> becomeOwner(Authentication auth) {
        return ResponseEntity.ok(userService.becomeOwner(auth.getName()));
    }

    private AuthResponse buildAuthResponse(String username, List<String> groups) {
        return new AuthResponse(
                jwtService.generateAccessToken(username, groups),
                jwtService.generateRefreshToken(username, groups),
                jwtService.getAccessTtlMs()
        );
    }
}