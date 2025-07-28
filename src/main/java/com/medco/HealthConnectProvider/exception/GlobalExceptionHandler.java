//package com.medco.HealthConnectProvider.exception;
//
//import com.medco.HealthConnectProvider.ui.response.ApiErrorResponse;
//import org.springframework.http.*;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.HttpMediaTypeNotAcceptableException;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.security.SignatureException;
//
//@ControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler({UnauthorizedException.class, BadRequestException.class, ExpiredJwtException.class,
//            AccessDeniedException.class, SignatureException.class, HttpMediaTypeNotAcceptableException.class})
//    public ResponseEntity<ApiErrorResponse> handleCommonExceptions(Exception ex) {
//        HttpStatus status;
//        String message;
//
//        if (ex instanceof UnauthorizedException) {
//            status = HttpStatus.UNAUTHORIZED;
//            message = ex.getMessage();
//        } else if (ex instanceof BadRequestException || ex instanceof ExpiredJwtException) {
//            status = HttpStatus.BAD_REQUEST;
//            message = ex.getMessage();
//        } else if (ex instanceof AccessDeniedException) {
//            status = HttpStatus.FORBIDDEN;
//            message = "You are not authorized to view these resources";
//        } else if (ex instanceof SignatureException) {
//            status = HttpStatus.FORBIDDEN;
//            message = "JWT Signature is not valid";
//        } else if (ex instanceof HttpMediaTypeNotAcceptableException) {
//            status = HttpStatus.NOT_ACCEPTABLE;
//            message = "Acceptable MIME type: " + MediaType.APPLICATION_JSON_VALUE;
//        } else {
//            status = HttpStatus.INTERNAL_SERVER_ERROR;
//            message = "An unexpected error occurred";
//        }
//
//        ApiErrorResponse errorResponse = new ApiErrorResponse(message);
//        return new ResponseEntity<>(errorResponse, status);
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
//        FieldError fieldError = ex.getBindingResult().getFieldError();
//        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
//        ApiErrorResponse errorResponse = new ApiErrorResponse(errorMessage);
//        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(ResponseStatusException.class)
//    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
//        ApiErrorResponse errorResponse = new ApiErrorResponse(ex.getReason());
//        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiErrorResponse> handleAllExceptions(Exception ex) {
//        String errorMessage = "An unexpected error occurred: " + ex.getMessage();
//        Throwable cause = ex.getCause();
//        while (cause != null) {
//            errorMessage += " Caused by: " + cause.getMessage();
//            cause = cause.getCause();
//        }
//        ApiErrorResponse errorResponse = new ApiErrorResponse(errorMessage);
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//}