package com.socialapp.identityservice.controller;

import com.socialapp.identityservice.dto.request.ReqLoginDTO;
import com.socialapp.identityservice.dto.request.ReqRegisterDTO;
import com.socialapp.identityservice.dto.request.ValidateRequest;
import com.socialapp.identityservice.dto.response.ApiResponse;
import com.socialapp.identityservice.dto.response.ResLoginDTO;
import com.socialapp.identityservice.dto.response.ResRegisterDTO;
import com.socialapp.identityservice.dto.response.ValidateResponse;
import com.socialapp.identityservice.entity.Identity;
import com.socialapp.identityservice.exception.ExistException;
import com.socialapp.identityservice.service.IdentityService;
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

@Slf4j
@RestController
@RequestMapping("/auth")
public class IdentityController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final IdentityService identityService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Value("${social.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;
    public IdentityController(IdentityService identityService, PasswordEncoder passwordEncoder,
                              AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.identityService = identityService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<ResRegisterDTO> register(@RequestBody @Valid ReqRegisterDTO registerDTO){
        if(identityService.isEmailExist(registerDTO.getEmail())){
            throw new ExistException("Email already exist");
        }
        String passwordHash = passwordEncoder.encode(registerDTO.getPassword());
        registerDTO.setPassword(passwordHash);
        ResRegisterDTO response = identityService.register(registerDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@RequestBody @Valid ReqLoginDTO loginDTO){
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

    @PostMapping("/validate")
    public ValidateResponse validateToken(@RequestBody ValidateRequest request){
        log.info("Validate token: {}", request.getToken());
        var result = securityUtil.validateToken(request);
        log.info("Token at controller identityservice: {}", result.isValid());
        return result;
    }

}
