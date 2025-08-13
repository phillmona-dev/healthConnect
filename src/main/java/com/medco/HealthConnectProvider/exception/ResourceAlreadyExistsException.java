package com.medco.HealthConnectProvider.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ResourceAlreadyExistsException extends RuntimeException {
    private String resourceName;
    private String fieldName;
    private Object fieldValue;
    private String secondFieldName;
    private Object secondFieldValue;

    public ResourceAlreadyExistsException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceAlreadyExistsException(String message, String fieldName, Object fieldValue,
                                          String secondFieldName, Object secondFieldValue) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.secondFieldName = secondFieldName;
        this.secondFieldValue = secondFieldValue;
    }
}
