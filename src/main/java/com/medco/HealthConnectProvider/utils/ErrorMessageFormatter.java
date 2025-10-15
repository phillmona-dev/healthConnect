package com.medco.HealthConnectProvider.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Utility class to convert technical error messages into user-friendly messages
 * for failed external dispensing and claim logs
 */
@Slf4j
public class ErrorMessageFormatter {

    /**
     * Convert technical error message to user-friendly message
     * 
     * @param technicalError The raw technical error message
     * @param exception The exception object (optional)
     * @return User-friendly error message
     */
    public static String formatErrorMessage(String technicalError, Exception exception) {
        if (technicalError == null || technicalError.trim().isEmpty()) {
            return "An unknown error occurred while communicating with the insurance system.";
        }

        String errorLower = technicalError.toLowerCase();
        
        // Connection errors
        if (errorLower.contains("connection refused") || 
            errorLower.contains("connect timed out") ||
            exception instanceof ConnectException) {
            return "Unable to connect to the insurance system. The service may be temporarily unavailable. Please try again later.";
        }
        
        if (errorLower.contains("connection reset") || 
            errorLower.contains("broken pipe")) {
            return "Connection to the insurance system was interrupted. Please try again.";
        }
        
        if (errorLower.contains("timeout") || 
            errorLower.contains("timed out") ||
            exception instanceof SocketTimeoutException) {
            return "The insurance system is taking too long to respond. Please try again later.";
        }
        
        if (errorLower.contains("unknown host") || 
            errorLower.contains("nodename nor servname provided") ||
            exception instanceof UnknownHostException) {
            return "Cannot reach the insurance system. Please check your network connection.";
        }
        
        if (errorLower.contains("network is unreachable") ||
            exception instanceof ResourceAccessException) {
            return "Network connection to the insurance system is unavailable. Please check your internet connection.";
        }
        
        // HTTP Status errors
        if (errorLower.contains("400 bad request") || 
            errorLower.contains("http 400")) {
            return extractDetailedMessage(technicalError, 
                "The request was rejected by the insurance system due to invalid data. Please verify all information and try again.");
        }
        
        if (errorLower.contains("401 unauthorized") || 
            errorLower.contains("http 401")) {
            return "Authentication failed with the insurance system. Please contact system administrator.";
        }
        
        if (errorLower.contains("403 forbidden") || 
            errorLower.contains("http 403")) {
            return "Access denied by the insurance system. You may not have permission to perform this action.";
        }
        
        if (errorLower.contains("404 not found") || 
            errorLower.contains("http 404")) {
            return "The requested resource was not found in the insurance system. The record may have been deleted.";
        }
        
        if (errorLower.contains("409 conflict") || 
            errorLower.contains("http 409")) {
            return extractDetailedMessage(technicalError,
                "A conflict occurred. The record may already exist in the insurance system.");
        }
        
        if (errorLower.contains("422 unprocessable entity") || 
            errorLower.contains("http 422")) {
            return extractDetailedMessage(technicalError,
                "The insurance system could not process the request due to validation errors. Please check all required fields.");
        }
        
        if (errorLower.contains("500 internal server error") || 
            errorLower.contains("http 500")) {
            return extractDetailedMessage(technicalError,
                "The insurance system encountered an internal error. Please try again later or contact support.");
        }
        
        if (errorLower.contains("502 bad gateway") || 
            errorLower.contains("http 502")) {
            return "The insurance system gateway is unavailable. Please try again later.";
        }
        
        if (errorLower.contains("503 service unavailable") || 
            errorLower.contains("http 503")) {
            return "The insurance system is temporarily unavailable. Please try again later.";
        }
        
        if (errorLower.contains("504 gateway timeout") || 
            errorLower.contains("http 504")) {
            return "The insurance system gateway timed out. Please try again later.";
        }
        
        // Business logic errors
        if (errorLower.contains("duplicate") || 
            errorLower.contains("already exists")) {
            return extractDetailedMessage(technicalError,
                "This record already exists in the insurance system. Duplicate submissions are not allowed.");
        }
        
        if (errorLower.contains("service is only allowed once per claim")) {
            return "This service has already been submitted for this claim. Each service can only be submitted once per claim.";
        }
        
        if (errorLower.contains("not found")) {
            return extractDetailedMessage(technicalError,
                "The requested record was not found in the insurance system.");
        }
        
        if (errorLower.contains("invalid") || 
            errorLower.contains("validation failed")) {
            return extractDetailedMessage(technicalError,
                "The data validation failed. Please check all required fields and try again.");
        }
        
        if (errorLower.contains("contract") && errorLower.contains("not active")) {
            return "The contract is not active. Please verify the contract status before submitting.";
        }
        
        if (errorLower.contains("contract") && errorLower.contains("expired")) {
            return "The contract has expired. Please use an active contract.";
        }
        
        if (errorLower.contains("eligibility") || 
            errorLower.contains("not eligible")) {
            return "The patient is not eligible for this service under the current contract.";
        }
        
        if (errorLower.contains("coverage") || 
            errorLower.contains("not covered")) {
            return "This service is not covered under the patient's insurance plan.";
        }
        
        if (errorLower.contains("limit exceeded") || 
            errorLower.contains("maximum limit")) {
            return "The service limit has been exceeded for this patient or contract.";
        }
        
        // JSON/Data format errors
        if (errorLower.contains("json") || 
            errorLower.contains("parse") || 
            errorLower.contains("malformed")) {
            return "Data format error occurred while communicating with the insurance system. Please contact support.";
        }
        
        // Database errors
        if (errorLower.contains("database") || 
            errorLower.contains("sql")) {
            return "A database error occurred in the insurance system. Please try again later.";
        }
        
        // Generic fallback - try to extract meaningful message from response
        return extractDetailedMessage(technicalError,
            "An error occurred while communicating with the insurance system. Please try again or contact support.");
    }

    /**
     * Extract detailed message from error response if available
     * 
     * @param technicalError The full technical error
     * @param defaultMessage The default message to use if no details found
     * @return User-friendly message with details if available
     */
    private static String extractDetailedMessage(String technicalError, String defaultMessage) {
        try {
            // Try to extract message from common error response patterns
            
            // Pattern 1: "Error processing request: <message>"
            if (technicalError.contains("Error processing request:")) {
                String extracted = technicalError.substring(
                    technicalError.indexOf("Error processing request:") + 25
                ).trim();
                
                // Remove quotes and extra characters
                extracted = extracted.replaceAll("^\"|\"$", "").trim();
                
                if (!extracted.isEmpty() && extracted.length() < 200) {
                    return extracted;
                }
            }
            
            // Pattern 2: JSON error message {"message": "..."}
            if (technicalError.contains("\"message\"") || technicalError.contains("\"error\"")) {
                int messageStart = technicalError.indexOf("\"message\":");
                if (messageStart == -1) {
                    messageStart = technicalError.indexOf("\"error\":");
                }
                
                if (messageStart != -1) {
                    int valueStart = technicalError.indexOf("\"", messageStart + 10);
                    if (valueStart != -1) {
                        int valueEnd = technicalError.indexOf("\"", valueStart + 1);
                        if (valueEnd != -1) {
                            String extracted = technicalError.substring(valueStart + 1, valueEnd).trim();
                            if (!extracted.isEmpty() && extracted.length() < 200) {
                                return extracted;
                            }
                        }
                    }
                }
            }
            
            // Pattern 3: After last colon (common in exception messages)
            if (technicalError.contains(":")) {
                String[] parts = technicalError.split(":");
                String lastPart = parts[parts.length - 1].trim();
                
                // Remove quotes and extra characters
                lastPart = lastPart.replaceAll("^\"|\"$", "").trim();
                
                // Only use if it's not too long and doesn't look like a stack trace
                if (!lastPart.isEmpty() && 
                    lastPart.length() < 200 && 
                    !lastPart.contains("at ") && 
                    !lastPart.contains("Exception")) {
                    return lastPart;
                }
            }
            
        } catch (Exception e) {
            log.warn("Error extracting detailed message from: {}", technicalError, e);
        }
        
        return defaultMessage;
    }

    /**
     * Format error message specifically for HTTP exceptions
     * 
     * @param exception The HTTP exception
     * @return User-friendly error message
     */
    public static String formatHttpError(Exception exception) {
        if (exception instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) exception;
            String responseBody = clientError.getResponseBodyAsString();
            return formatErrorMessage(
                "HTTP " + clientError.getStatusCode() + ": " + responseBody, 
                exception
            );
        }
        
        if (exception instanceof HttpServerErrorException) {
            HttpServerErrorException serverError = (HttpServerErrorException) exception;
            String responseBody = serverError.getResponseBodyAsString();
            return formatErrorMessage(
                "HTTP " + serverError.getStatusCode() + ": " + responseBody, 
                exception
            );
        }
        
        return formatErrorMessage(exception.getMessage(), exception);
    }

    /**
     * Shorten error message if it's too long
     * 
     * @param message The error message
     * @param maxLength Maximum length
     * @return Shortened message
     */
    public static String shortenMessage(String message, int maxLength) {
        if (message == null || message.length() <= maxLength) {
            return message;
        }
        
        return message.substring(0, maxLength - 3) + "...";
    }
}

