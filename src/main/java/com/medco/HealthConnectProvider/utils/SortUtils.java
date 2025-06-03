package com.medco.HealthConnectProvider.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SortUtils {
    
    private static final Set<String> ALLOWED_PAYER_SORT_FIELDS = new HashSet<>(
            Arrays.asList("id", "payerName", "email", "telephone", "category", 
                         "tinNumber", "status", "registrationDate"));
    
    public static String validatePayerSortField(String sortField) {
        if (sortField != null && ALLOWED_PAYER_SORT_FIELDS.contains(sortField)) {
            return sortField;
        }
        return "id"; // Default sort field
    }
    
    public static String validateSortDirection(String sortDir) {
        if (sortDir != null && (sortDir.equalsIgnoreCase("asc") || sortDir.equalsIgnoreCase("desc"))) {
            return sortDir;
        }
        return "desc"; // Default sort direction
    }

    private static final Set<String> ALLOWED_PROVIDER_SORT_FIELDS = new HashSet<>(
            Arrays.asList("id", "providerName", "email", "telephone", "category",
                    "tinNumber", "status", "level", "registrationDate"));

    public static String validateProviderSortField(String sortField) {
        if (sortField != null && ALLOWED_PROVIDER_SORT_FIELDS.contains(sortField)) {
            return sortField;
        }
        return "id"; // Default sort field
    }
}