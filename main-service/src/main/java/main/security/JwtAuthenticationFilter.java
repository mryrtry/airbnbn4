package main.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.camunda.bpm.engine.IdentityService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final FilterErrorWriter errorWriter;
    private final IdentityService identityService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlacklistService blacklistService,
                                   FilterErrorWriter errorWriter,
                                   IdentityService identityService) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.errorWriter = errorWriter;
        this.identityService = identityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String jti = jwtService.extractJti(token);
            if (jti == null || blacklistService.isBlacklisted(jti)) {
                errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED", "Token revoked");
                return;
            }

            String username = jwtService.extractUsername(token);
            List<String> groups = jwtService.extractGroups(token);

            var authorities = groups.stream()
                    .map(g -> new SimpleGrantedAuthority("ROLE_" + g))
                    .toList();

            var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            identityService.setAuthentication(username, groups);
        } catch (JwtException | IllegalArgumentException ex) {
            errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN", "Invalid or expired token");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            identityService.clearAuthentication();
        }
    }
}