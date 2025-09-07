package com.medco.HealthConnectProvider.shared.error;

import java.util.Map;

public final class StringPatternFormatException extends AssertionException {

    private final String regexPattern;
    private final String value;

    private StringPatternFormatException(StringPatternFormatExceptionBuilder builder) {
        super(builder.regexPattern, builder.message());
        regexPattern = builder.regexPattern;
        value = builder.value;
    }

    public static StringPatternFormatExceptionBuilder builder() {
        return new StringPatternFormatExceptionBuilder();
    }

    @Override
    public AssertionErrorType type() {
        return AssertionErrorType.STRING_TOO_SHORT;
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of("regexPattern", regexPattern, "value", value);
    }

    public static final class StringPatternFormatExceptionBuilder {

        private String field;
        private String value;
        private String regexPattern;

        private StringPatternFormatExceptionBuilder() {
        }

        StringPatternFormatExceptionBuilder field(String field) {
            this.field = field;

            return this;
        }

        StringPatternFormatExceptionBuilder value(String value) {
            this.value = value;
            return this;
        }

        StringPatternFormatExceptionBuilder regexPattern(String regexPattern) {
            this.regexPattern = regexPattern;
            return this;
        }

        private String message() {
            return "The value \"%s\" in field \"%s\" must be comply to regex patter %s.".formatted(value, field, regexPattern);
        }

        public StringPatternFormatException build() {
            return new StringPatternFormatException(this);
        }
    }
}

