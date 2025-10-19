package com.socialapp.identityservice.service;

import com.socialapp.identityservice.dto.request.ReqRegisterDTO;
import com.socialapp.identityservice.dto.response.ResRegisterDTO;
import com.socialapp.identityservice.entity.Identity;
import com.socialapp.identityservice.mapper.IdentityConverter;
import com.socialapp.identityservice.repository.IdentityRepository;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {

    private final IdentityConverter identityConverter;
    private final IdentityRepository identityRepository;
    public IdentityService(IdentityRepository identityRepository, IdentityConverter identityConverter) {
        this.identityRepository = identityRepository;
        this.identityConverter = identityConverter;
    }

    public boolean isEmailExist(String email) {
        return identityRepository.existsByEmail(email);
    }

    public ResRegisterDTO register(ReqRegisterDTO reqRegisterDTO) {
        return identityConverter.convertToDto(
                identityRepository.save(
                        identityConverter.convertToEntity(reqRegisterDTO)
                )
        );
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
}
