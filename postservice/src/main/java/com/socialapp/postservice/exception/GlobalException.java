package com.socialapp.postservice.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.socialapp.postservice.dto.response.ApiResponse;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(value = {
            NotFoundException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleNotFoundException(NotFoundException ex) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError("Exception occurs...");
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> validationError(MethodArgumentNotValidException Exception) {
        BindingResult result = Exception.getBindingResult();
        final List<FieldError> FieldError = result.getFieldErrors();
        ApiResponse<Object> res = new ApiResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError(Exception.getBody().getDetail());
        List<String> errors = new ArrayList<>();
        for (FieldError fError : FieldError) {
            errors.add(fError.getDefaultMessage());
        }
        if (errors.size() > 1) {
            res.setMessage(errors);
        } else {
            res.setMessage(errors.get(0));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }
}
