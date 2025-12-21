package com.socialapp.identityservice.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.socialapp.identityservice.dto.request.*;
import com.socialapp.identityservice.dto.response.ResLoginDTO;
import com.socialapp.identityservice.dto.response.ResRegisterDTO;
import com.socialapp.identityservice.dto.response.ValidateResponse;
import com.socialapp.identityservice.entity.Identity;
import com.socialapp.identityservice.exception.ExistException;
import com.socialapp.identityservice.exception.GoogleAuthException;
import com.socialapp.identityservice.service.EmailService;
import com.socialapp.identityservice.service.GoogleAuthService;
import com.socialapp.identityservice.service.IdentityService;
import com.socialapp.identityservice.util.ApiMessage;
import com.socialapp.identityservice.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
public class IdentityController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final IdentityService identityService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final GoogleAuthService googleAuthService;
    private final EmailService emailService;

    @Value("${social.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public IdentityController(IdentityService identityService, PasswordEncoder passwordEncoder,
            AuthenticationManagerBuilder authenticationManagerBuilder,
            SecurityUtil securityUtil, GoogleAuthService googleAuthService,
            EmailService emailService) {
        this.emailService = emailService;
        this.securityUtil = securityUtil;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.identityService = identityService;
        this.passwordEncoder = passwordEncoder;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResRegisterDTO> register(@RequestBody @Valid ReqRegisterDTO registerDTO) {
        if (identityService.isEmailExist(registerDTO.getEmail())) {
            throw new ExistException("Email already exist");
        }
        String passwordHash = passwordEncoder.encode(registerDTO.getPassword());
        registerDTO.setPassword(passwordHash);
        ResRegisterDTO response = identityService.register(registerDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@RequestBody @Valid ReqLoginDTO loginDTO) {
        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getEmail(), loginDTO.getPassword());
        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // nạp thông tin (nếu xử lý thành công) vào SecurityContext

        SecurityContextHolder.getContext().setAuthentication(authentication);
        Identity user = this.identityService.findIdentityByEmail(loginDTO.getEmail());

        ResLoginDTO res = ResLoginDTO.builder().id(user.getId()).email(user.getEmail()).build();

        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(access_token);
        String refresh_token = this.securityUtil.createRefreshToken(loginDTO.getEmail(), res);
        res.setRefreshToken(refresh_token);
        this.identityService.updateRefreshToken(refresh_token, loginDTO.getEmail());

        // set cookies
        ResponseCookie responseCookie = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(res);
    }

    @PostMapping("/google")
    @ApiMessage("Đăng nhập thành công")
    public ResponseEntity<ResLoginDTO> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        try {
            log.info("Processing Google login request");

            // Verify Google ID token
            GoogleIdToken.Payload payload = googleAuthService.verifyGoogleToken(request.getIdToken());

            // Extract user information from Google
            String googleId = googleAuthService.getGoogleId(payload);
            String email = googleAuthService.getEmail(payload);
            String name = googleAuthService.getName(payload);
            String pictureUrl = googleAuthService.getPictureUrl(payload);
            boolean emailVerified = googleAuthService.isEmailVerified(payload);

            log.info("Google token verified for email: {}", email);

            // Handle user creation or login
            Identity identity = identityService.handleGoogleAuth(googleId, email, name, pictureUrl, emailVerified);

            // Build response
            ResLoginDTO res = ResLoginDTO.builder()
                    .id(identity.getId())
                    .email(identity.getEmail())
                    .build();

            // Generate JWT tokens
            String accessToken = securityUtil.createAccessToken(identity.getEmail(), res);
            String refreshToken = securityUtil.createRefreshToken(identity.getEmail(), res);

            res.setAccessToken(accessToken);
            res.setRefreshToken(refreshToken);

            // Update refresh token in database
            identityService.updateRefreshToken(refreshToken, identity.getEmail());

            // Set refresh token cookie
            ResponseCookie responseCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration)
                    .build();

            log.info("Google login successful for user: {}", email);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                    .body(res);

        } catch (IllegalArgumentException e) {
            log.error("Invalid Google token: {}", e.getMessage());
            throw new GoogleAuthException("Invalid ID token");
        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage(), e);
            throw new GoogleAuthException("Google authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ValidateResponse validateToken(@RequestBody ValidateRequest request) {
        log.info("Validate token: {}", request.getToken());
        var result = securityUtil.validateToken(request);
        log.info("Token at controller identityservice: {}", result.isValid());
        return result;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<String> refreshToken(@RequestBody RefreshTokenRequest refreshToken) {
        String response = this.securityUtil.refreshAccessToken(refreshToken);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String email) {
        this.identityService.updateRefreshToken("", email);
        ResponseCookie responseCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body("Logout successful");
    }

    @GetMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        this.emailService.sendPasswordFromTemplateSync(email, "Password Recovery", "forgotpassword");
        return ResponseEntity.ok().body("Password recovery email sent");
    }

    @PutMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            this.identityService.changePassword(request);
            return ResponseEntity.ok().body("Password has been reset successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
