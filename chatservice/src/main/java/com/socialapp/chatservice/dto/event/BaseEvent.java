package com.socialapp.chatservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent {
    private String eventType;
    private String sourceService;
    private Object payload;
}
