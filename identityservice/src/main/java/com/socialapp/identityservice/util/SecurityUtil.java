package com.socialapp.identityservice.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.util.Base64;
import com.socialapp.identityservice.dto.request.RefreshTokenRequest;
import com.socialapp.identityservice.dto.request.ValidateRequest;
import com.socialapp.identityservice.dto.response.ResLoginDTO;
import com.socialapp.identityservice.dto.response.ValidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SecurityUtil {
    private final JwtEncoder jwtEncoder;

    public SecurityUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;
    @Value("${social.jwt.base64-secret}")
    private String jwtKey;

    @Value("${social.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    @Value("${social.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public String createAccessToken(String email, ResLoginDTO dto) {
        ResLoginDTO userToken = new ResLoginDTO();
        userToken.setId(dto.getId());
        userToken.setEmail(dto.getEmail());
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);
        // @formatter:off
        List<String> listAuthority = new ArrayList<>();
        listAuthority.add("ROLE_USER");
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken)
                .claim("permission", listAuthority)
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims)).getTokenValue();
    }

    public String createRefreshToken(String email, ResLoginDTO dto) {
        ResLoginDTO userToken = new ResLoginDTO();
        userToken.setId(dto.getId());
        userToken.setEmail(dto.getEmail());
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);
        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken)
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims)).getTokenValue();
    }

    public ValidateResponse validateToken(ValidateRequest request) {
        String token = request.getToken();
        boolean isValid = true;

        try {
            // Kiểm tra token và thời hạn
            verifyToken(token, true);
        } catch (JOSEException | ParseException e) {
            System.out.println(">>> Token validation failed: " + e.getMessage());
            isValid = false;
        } catch (Exception e) {
            System.out.println(">>> Unexpected error validating token: " + e.getMessage());
            isValid = false;
        }
        log.info(">>> Token: " + token + " isValid: " + isValid);
        return ValidateResponse.builder().valid(isValid).build();
    }

    public void verifyToken(String token, boolean checkExpiry) throws ParseException, JOSEException {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(SecurityUtil.JWT_ALGORITHM)
                .build();

        try {
            Jwt jwt = jwtDecoder.decode(token);

            if (checkExpiry) {
                Instant expiresAt = jwt.getExpiresAt();
                Instant now = Instant.now();

                if (expiresAt == null || expiresAt.isBefore(now)) {
                    System.out.println(">>> Token expired at: " + expiresAt);
                    throw new JOSEException("Token expired");
                }
            }

            System.out.println(">>> Token is valid Expires at: " + jwt.getExpiresAt());
        } catch (Exception e) {
            System.out.println(">>> Token verification error: " + e.getMessage());
            throw e;
        }
    }

    public Jwt checkValidToken(String token){
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        try {
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            System.out.println(">>> Refresh token error: " + e.getMessage());
            throw e;
        }
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    public String refreshAccessToken(RefreshTokenRequest refreshToken) {
    try {
        Jwt jwt = checkValidToken(refreshToken.getRefreshToken());
        Instant expiresAt = jwt.getExpiresAt();

        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            log.error("Refresh token expired");
            throw new RuntimeException("Refresh token expired");
        }

        String email = jwt.getSubject();
        Object userClaim = jwt.getClaims().get("user");

        ResLoginDTO user = new ResLoginDTO();
        if (userClaim instanceof Map<?, ?> userMap) {
            user.setId((String) userMap.get("id"));
            user.setEmail((String) userMap.get("email"));
        }

        String newAccessToken = createAccessToken(email, user);

        return newAccessToken;

    } catch (Exception e) {
        log.error("Error refreshing access token: {}", e.getMessage());
        throw new RuntimeException("Error refreshing access token", e);
    }
}

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user.
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    // public static boolean isAuthenticated() {
    //     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //     return authentication != null && getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
    // }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    // public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
    //     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //     return (
    //         authentication != null && getAuthorities(authentication).anyMatch(authority -> Arrays.asList(authorities).contains(authority))
    //     );
    // }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false otherwise.
     */
    // public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
    //     return !hasCurrentUserAnyOfAuthorities(authorities);
    // }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    // public static boolean hasCurrentUserThisAuthority(String authority) {
    //     return hasCurrentUserAnyOfAuthorities(authority);
    // }

    // private static Stream<String> getAuthorities(Authentication authentication) {
    //     return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    // }

}