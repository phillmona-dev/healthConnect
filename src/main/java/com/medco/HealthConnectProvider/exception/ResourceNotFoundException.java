package com.medco.HealthConnectProvider.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@Setter
@Getter
@NoArgsConstructor
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException{

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 5003833611366908113L;
    private String resourceName;
    private String fieldName;
    private String fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s with %s %s is not found", resourceName, fieldName, fieldValue));

    }

}
