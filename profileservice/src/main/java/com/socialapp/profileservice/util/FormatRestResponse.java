package com.socialapp.profileservice.util;


import com.socialapp.profileservice.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();
        ApiResponse<Object> res = new ApiResponse<>();
        res.setStatusCode(status);
        // case error
        if (body instanceof String) {
            // Gói string vào ApiResponse rồi convert sang JSON string thủ công
            ApiResponse<Object> res1 = new ApiResponse<>();
            res1.setStatusCode(((ServletServerHttpResponse) response).getServletResponse().getStatus());
            res1.setMessage("CALL API SUCCESS");
            res1.setData(body);
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(res1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (body instanceof Resource) {
            return body;
        }
        String path = request.getURI().getPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return body;
        }

        if (path.startsWith("/auth/validate")) {
            return body;
        }

        if (status >= 400) {
            return body;
        } else {
            res.setData(body);
            ApiMessage message = returnType.getMethodAnnotation(ApiMessage.class);
            res.setMessage(message != null ? message.value() : "CALL API SUCCESS");
        }
        return res;
    }

}
