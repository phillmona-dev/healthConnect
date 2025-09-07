package com.medco.HealthConnectProvider.shared.error;

import java.util.Map;

public class LessElementsException extends AssertionException {

    private final String minSize;
    private final String currentSize;

    public LessElementsException(LessElementsExceptionBuilder builder) {
        super(builder.field, builder.message());
        minSize = String.valueOf(builder.minSize);
        currentSize = String.valueOf(builder.size);
    }

    public static LessElementsExceptionBuilder builder() {
        return new LessElementsExceptionBuilder();
    }

    @Override
    public AssertionErrorType type() {
        return AssertionErrorType.TOO_MANY_ELEMENTS;
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of("minSize", minSize, "currentSize", currentSize);
    }

    public static class LessElementsExceptionBuilder {

        private String field;
        private int minSize;
        private int size;

        public LessElementsExceptionBuilder field(String field) {
            this.field = field;

            return this;
        }

        public LessElementsExceptionBuilder minSize(int minSize) {
            this.minSize = minSize;

            return this;
        }

        public LessElementsExceptionBuilder size(int size) {
            this.size = size;

            return this;
        }

        private String message() {
            return "Size of collection \"" +
                    field +
                    "\" must be at least " +
                    minSize +
                    " but was " +
                    size;
        }

        public LessElementsException build() {
            return new LessElementsException(this);
        }
    }
}
