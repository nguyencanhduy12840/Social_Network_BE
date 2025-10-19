package com.socialapp.identityservice.mapper;

import com.socialapp.identityservice.dto.request.ReqRegisterDTO;
import com.socialapp.identityservice.dto.response.ResRegisterDTO;
import com.socialapp.identityservice.entity.Identity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class IdentityConverter {

    private final ModelMapper modelMapper;
    public IdentityConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Identity convertToEntity(ReqRegisterDTO reqRegisterDTO) {
        Identity identity = modelMapper.map(reqRegisterDTO, Identity.class);
        return identity;
    }

    public ResRegisterDTO convertToDto(Identity identity) {
        ResRegisterDTO resRegisterDTO = modelMapper.map(identity, ResRegisterDTO.class);
        return resRegisterDTO;
    }
}
