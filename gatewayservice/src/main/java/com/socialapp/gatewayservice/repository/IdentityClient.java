package com.socialapp.gatewayservice.repository;

import com.socialapp.gatewayservice.dto.request.ValidateRequest;
import com.socialapp.gatewayservice.dto.response.ApiResponse;
import com.socialapp.gatewayservice.dto.response.ValidateResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@Repository
public interface IdentityClient {

    @PostExchange(url = "/auth/validate", contentType = MediaType.APPLICATION_JSON_VALUE,
            accept = MediaType.ALL_VALUE)
    Mono<ApiResponse<ValidateResponse>> validateToken(@RequestBody ValidateRequest token);

}
