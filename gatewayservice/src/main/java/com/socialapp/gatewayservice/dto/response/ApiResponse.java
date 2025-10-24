package com.socialapp.gatewayservice.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {
    private int statusCode;
    private String error;

    private Object message;
    private T data;

    @Override
    public String toString() {
        return "ApiResponse{" +
                "statusCode=" + statusCode +
                ", error='" + error + '\'' +
                ", message=" + message +
                ", data=" + data +
                '}';
    }
}
