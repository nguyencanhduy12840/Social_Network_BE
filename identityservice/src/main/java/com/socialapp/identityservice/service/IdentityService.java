package com.socialapp.identityservice.service;

import com.socialapp.identityservice.dto.request.ProfileCreationRequest;
import com.socialapp.identityservice.dto.request.ReqRegisterDTO;
import com.socialapp.identityservice.dto.response.ResRegisterDTO;
import com.socialapp.identityservice.entity.Identity;
import com.socialapp.identityservice.mapper.IdentityConverter;
import com.socialapp.identityservice.repository.IdentityRepository;
import com.socialapp.identityservice.repository.httpclient.ProfileClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class IdentityService {

    private final IdentityConverter identityConverter;
    private final IdentityRepository identityRepository;
    private final ProfileClient profileClient;
    private final PasswordEncoder passwordEncoder;
    public IdentityService(IdentityRepository identityRepository,
                           IdentityConverter identityConverter, ProfileClient profileClient, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.identityRepository = identityRepository;
        this.identityConverter = identityConverter;
        this.profileClient = profileClient;
    }

    public boolean isEmailExist(String email) {
        return identityRepository.existsByEmail(email);
    }

    public ResRegisterDTO register(ReqRegisterDTO reqRegisterDTO) {
        Identity identity = identityRepository.save(
                identityConverter.convertToEntity(reqRegisterDTO)
        );
        ProfileCreationRequest profileCreationRequest = identityConverter.convertToProfileCreationRequest(reqRegisterDTO);
        profileCreationRequest.setUserId(identity.getId());
        profileClient.createProfile(profileCreationRequest);
        return identityConverter.convertToDto(identity);
    }

    public Identity findIdentityByEmail(String email) {
        Identity identity = this.identityRepository.findByEmail(email);
        if(identity == null) {
            throw new RuntimeException("Identity not found with email: " + email);
        }
        return identity;
    }

    public void updateRefreshToken(String token, String email){
        Identity identity = this.identityRepository.findByEmail(email);
        if(identity != null){
            identity.setRefreshToken(token);
            this.identityRepository.save(identity);
        }
    }

    /**
     * Handles Google authentication - creates new user or updates existing user
     * @param googleId Google user ID (sub)
     * @param email User email
     * @param name User name
     * @param pictureUrl Profile picture URL
     * @param emailVerified Whether email is verified by Google
     * @return Identity entity
     */
    public Identity handleGoogleAuth(String googleId, String email, String name, 
                                       String pictureUrl, boolean emailVerified) {
        Identity identity = identityRepository.findByEmail(email);
        
        if (identity != null) {
            // User exists - update Google ID if not set and last login
            if (identity.getGoogleId() == null || identity.getGoogleId().isEmpty()) {
                identity.setGoogleId(googleId);
                identity.setAuthProvider("google");
            }
            identity.setAvatarUrl(pictureUrl);
            identity.setLastLogin(LocalDateTime.now());
            identity.setEmailVerified(emailVerified);
            
            return identityRepository.save(identity);
        } else {
            // Create new user with Google auth
            Identity newIdentity = new Identity();
            newIdentity.setEmail(email);
            newIdentity.setGoogleId(googleId);
            newIdentity.setAuthProvider("google");
            newIdentity.setAvatarUrl(pictureUrl);
            newIdentity.setEmailVerified(emailVerified);
            newIdentity.setLastLogin(LocalDateTime.now());
            newIdentity.setPassword(null); // No password for OAuth users
            
            Identity savedIdentity = identityRepository.save(newIdentity);
            
            // Create profile in profile service
            ProfileCreationRequest profileCreationRequest = new ProfileCreationRequest();
            profileCreationRequest.setUserId(savedIdentity.getId());
            profileCreationRequest.setEmail(email);
            
            // Extract username from email or name
            String username = name != null && !name.isEmpty() 
                ? name.replaceAll("\\s+", "").toLowerCase() 
                : email.split("@")[0];
            profileCreationRequest.setUsername(username);
            
            try {
                profileClient.createProfile(profileCreationRequest);
            } catch (Exception e) {
                // Log error but don't fail authentication if profile creation fails
                System.err.println("Failed to create profile for Google user: " + e.getMessage());
            }
            
            return savedIdentity;
        }
    }

    public Identity findById(String id) {
        return identityRepository.findById(id).orElse(null);
    }

    public String forgotPassword(String email) {
        Identity identity = this.identityRepository.findByEmail(email);
        if(identity != null){
            String newPassword = randomPassword();
            String hashedPassword = passwordEncoder.encode(newPassword);
            identity.setPassword(hashedPassword);
            this.identityRepository.save(identity);
            return newPassword;
        }
        return null;
    }

    public String randomPassword(){
        Random random = new Random();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int digit = random.nextInt(10);
            result.append(digit);
        }
        return result.toString();
    }
}

