package com.medco.HealthConnectProvider.utils.NullHandler;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NullChecker {

        /**
         * Checks if an object is null and throws a custom exception if it is.
         *
         * @param object      The object to check.
         * @param objectName  The name of the object (used in the exception message).
         * @throws IllegalArgumentException if the object is null.
         */
        public static void requireNonNull(Object object, String objectName) {
            if (object == null) {
                throw new IllegalArgumentException(objectName + " cannot be null.");
            }
        }



        /**
         * Checks if a string is null or empty and throws a custom exception if it is.
         *
         * @param string      The string to check.
         * @param fieldName   The name of the field (used in the exception message).
         * @throws IllegalArgumentException if the string is null or empty.
         */
        public static void requireNonEmpty(String string, String fieldName) {
            if (string == null || string.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + " cannot be null or empty.");
            }
        }

        /**
         * Checks if a collection is null or empty and throws a custom exception if it is.
         *
         * @param collection  The collection to check.
         * @param collectionName The name of the collection (used in the exception message).
         * @throws IllegalArgumentException if the collection is null or empty.
         */
        public static void requireNonEmpty(Collection<?> collection, String collectionName) {
            if (collection == null || collection.isEmpty()) {
                throw new IllegalArgumentException(collectionName + " cannot be null or empty.");
            }
        }

        /**
         * Checks if a map is null or empty and throws a custom exception if it is.
         *
         * @param map         The map to check.
         * @param mapName     The name of the map (used in the exception message).
         * @throws IllegalArgumentException if the map is null or empty.
         */
        public static void requireNonEmpty(Map<?, ?> map, String mapName) {
            if (map == null || map.isEmpty()) {
                throw new IllegalArgumentException(mapName + " cannot be null or empty.");
            }
        }

        /**
         * Safely retrieves a value from a nested object using a getter method.
         * Throws an exception if any intermediate object is null.
         *
         * @param object       The root object.
         * @param getterChain  The chain of getter methods to access the nested field.
         * @param fieldName    The name of the field (used in the exception message).
         * @return The value of the nested field.
         * @throws IllegalArgumentException if any intermediate object is null.
         */
        public static <T> T getNestedValue(Object object, String[] getterChain, String fieldName) {
            Objects.requireNonNull(object, "Root object cannot be null.");
            Objects.requireNonNull(getterChain, "Getter chain cannot be null.");

            try {
                Object currentValue = object;
                for (String getter : getterChain) {
                    currentValue = currentValue.getClass()
                            .getMethod(getter)
                            .invoke(currentValue);
                    if (currentValue == null) {
                        throw new IllegalArgumentException("Intermediate field in chain for " + fieldName + " is null.");
                    }
                }
                return (T) currentValue;
            } catch (Exception e) {
                throw new IllegalArgumentException("Error accessing nested field " + fieldName + ": " + e.getMessage());
            }
        }

        /**
         * Safely checks if a nested object is null using a getter method chain.
         *
         * @param object       The root object.
         * @param getterChain  The chain of getter methods to access the nested field.
         * @return True if the nested object is null, false otherwise.
         */
        public static boolean isNestedNull(Object object, String[] getterChain) {
            if (object == null || getterChain == null) {
                return true;
            }

            try {
                Object currentValue = object;
                for (String getter : getterChain) {
                    currentValue = currentValue.getClass()
                            .getMethod(getter)
                            .invoke(currentValue);
                    if (currentValue == null) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                return true; // Treat exceptions as null for safety.
            }
        }

        /**
         * Returns the object if it is not null, otherwise returns a default value.
         *
         * @param object        The object to check.
         * @param defaultValue  The default value to return if the object is null.
         * @return The object if not null, otherwise the default value.
         */
        public static <T> T getOrDefault(@Nullable T object, T defaultValue) {
            return Optional.ofNullable(object).orElse(defaultValue);
        }
}
