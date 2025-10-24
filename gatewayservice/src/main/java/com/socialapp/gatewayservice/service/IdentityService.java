package com.socialapp.gatewayservice.service;

import com.socialapp.gatewayservice.dto.request.ValidateRequest;
import com.socialapp.gatewayservice.dto.response.ApiResponse;
import com.socialapp.gatewayservice.dto.response.ValidateResponse;
import com.socialapp.gatewayservice.repository.IdentityClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class IdentityService {

    private final IdentityClient identityClient;

    public IdentityService(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    public Mono<ApiResponse<ValidateResponse>> validateToken(String token) {
        return identityClient.validateToken(ValidateRequest.builder().token(token).build());
    }
}
