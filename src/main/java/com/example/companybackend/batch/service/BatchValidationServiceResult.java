package com.example.companybackend.batch.service;

import java.util.ArrayList;
import java.util.List;

public class BatchValidationServiceResult {
    private boolean valid = true;
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationWarning> warnings = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void addError(ValidationError error) {
        this.errors.add(error);
        this.valid = false;
    }

    public List<ValidationWarning> getWarnings() {
        return warnings;
    }

    public void addWarning(ValidationWarning warning) {
        this.warnings.add(warning);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public static class ValidationError {
        private String errorCode;
        private String message;

        public ValidationError(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ValidationWarning {
        private String warningCode;
        private String message;

        public ValidationWarning(String warningCode, String message) {
            this.warningCode = warningCode;
            this.message = message;
        }

        public String getWarningCode() {
            return warningCode;
        }

        public String getMessage() {
            return message;
        }
    }
}