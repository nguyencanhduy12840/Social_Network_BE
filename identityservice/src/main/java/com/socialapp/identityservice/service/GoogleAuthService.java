package com.socialapp.identityservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class GoogleAuthService {

    @Value("${social.google.client-id}")
    private String googleClientId;

    private final NetHttpTransport transport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    /**
     * Verifies a Google ID token and returns the payload
     * @param idToken The ID token to verify
     * @return GoogleIdToken.Payload containing user information
     * @throws Exception if token is invalid or verification fails
     */
    public GoogleIdToken.Payload verifyGoogleToken(String idToken) throws Exception {
        log.info("Verifying Google ID token");
        
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken googleIdToken = verifier.verify(idToken);
        
        if (googleIdToken == null) {
            log.error("Invalid ID token");
            throw new IllegalArgumentException("Invalid ID token");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        log.info("Successfully verified Google token for user: {}", payload.getEmail());
        
        return payload;
    }

    /**
     * Extracts Google user ID (sub) from payload
     */
    public String getGoogleId(GoogleIdToken.Payload payload) {
        return payload.getSubject();
    }

    /**
     * Extracts email from payload
     */
    public String getEmail(GoogleIdToken.Payload payload) {
        return payload.getEmail();
    }

    /**
     * Extracts user's name from payload
     */
    public String getName(GoogleIdToken.Payload payload) {
        return (String) payload.get("name");
    }

    /**
     * Extracts profile picture URL from payload
     */
    public String getPictureUrl(GoogleIdToken.Payload payload) {
        return (String) payload.get("picture");
    }

    /**
     * Checks if email is verified
     */
    public Boolean isEmailVerified(GoogleIdToken.Payload payload) {
        return payload.getEmailVerified();
    }
}
