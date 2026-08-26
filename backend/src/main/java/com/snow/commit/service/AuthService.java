package com.snow.commit.service;

import com.snow.commit.dto.AuthResponse;
import com.snow.commit.dto.LoginRequest;
import com.snow.commit.dto.RegisterRequest;
import com.snow.commit.dto.UserDto;
import com.snow.commit.entity.AppUser;
import com.snow.commit.exception.EmailAlreadyExistsException;
import com.snow.commit.exception.InvalidCredentialsException;
import com.snow.commit.repository.AppUserRepository;
import com.snow.commit.security.JwtUtil;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setCreatedAt(LocalDateTime.now());
        AppUser saved = userRepository.save(user);
        return new AuthResponse(jwtUtil.generateToken(saved), toUserDto(saved));
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponse(jwtUtil.generateToken(user), toUserDto(user));
    }

    public UserDto toUserDto(AppUser user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
