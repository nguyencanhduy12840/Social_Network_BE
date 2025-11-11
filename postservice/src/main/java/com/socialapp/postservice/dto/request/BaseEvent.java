package com.socialapp.postservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseEvent {
    private String eventType;    
    private String sourceService;
    private Object payload;
}
