package com.example.ticket.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticket.dto.auth.AuthResponse;
import com.example.ticket.dto.auth.LoginRequest;
import com.example.ticket.dto.auth.RefreshTokenRequest;
import com.example.ticket.dto.auth.RegisterRequest;
import com.example.ticket.dto.auth.UserResponse;
import com.example.ticket.entity.user.UserEntity;
import com.example.ticket.enums.UserRole;
import com.example.ticket.exceptions.EmailAlreadyExistsException;
import com.example.ticket.exceptions.ItemNotFoundException;
import com.example.ticket.repository.user.UserRepository;
import com.example.ticket.security.JwtTokenProvider;
import com.example.ticket.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(UserRole.CUSTOMER)
                .build();

        user = userRepository.save(user);

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(principal.getId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .role(principal.getRole())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtTokenProvider.validateRefreshToken(token)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("User not found with id: " + userId));

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("User not found with id: " + userId));
        return new UserResponse(user);
    }

    public AuthResponse seedAdmin(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        UserEntity admin = userRepository.findByEmail(email).orElse(null);

        if (admin == null) {
            admin = UserEntity.builder()
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName().trim())
                    .role(UserRole.ADMIN)
                    .build();
            admin = userRepository.save(admin);
        }

        UserPrincipal principal = UserPrincipal.create(admin);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getRole())
                .build();
    }
}
