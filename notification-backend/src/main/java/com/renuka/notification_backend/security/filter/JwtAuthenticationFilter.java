package com.renuka.notification_backend.security.filter;

import com.renuka.notification_backend.security.jwt.JwtClaims;
import com.renuka.notification_backend.security.jwt.JwtCookieService;
import com.renuka.notification_backend.security.jwt.JwtService;
import com.renuka.notification_backend.security.jwt.JwtTokenType;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtCookieService jwtCookieService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtCookieService jwtCookieService,
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtCookieService = jwtCookieService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromCookie(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromCookie(HttpServletRequest request) {
        jwtCookieService.getAccessToken(request)
                .flatMap(token -> jwtService.validateToken(token, JwtTokenType.ACCESS))
                .ifPresent(this::setAuthentication);
    }

    private void setAuthentication(JwtClaims claims) {
        userRepository.findByEmail(claims.subject())
                .filter(User::isActive)
                .ifPresent(user -> {
                    List<SimpleGrantedAuthority> authorities = claims.roles()
                            .stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
    }
}
