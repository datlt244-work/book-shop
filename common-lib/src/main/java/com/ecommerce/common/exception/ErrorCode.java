package com.ecommerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
        // System errors (9xxx)
        UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
        INVALID_KEY(9998, "Invalid error key", HttpStatus.BAD_REQUEST),

        // Validation errors - Generic (1000-1099)
        VALIDATION_ERROR(1000, "Validation error", HttpStatus.BAD_REQUEST),

        // Validation errors - Username (1100-1119)
        USERNAME_REQUIRED(1100, "Username is required", HttpStatus.BAD_REQUEST),
        USERNAME_INVALID(1101, "Username must be between 3 and 50 characters", HttpStatus.BAD_REQUEST),
        USERNAME_FORMAT_INVALID(1102, "Username can only contain letters, numbers, and underscores",
                        HttpStatus.BAD_REQUEST),

        // Validation errors - Password (1120-1139)
        PASSWORD_REQUIRED(1120, "Password is required", HttpStatus.BAD_REQUEST),
        PASSWORD_INVALID(1121, "Password must be between 8 and 100 characters", HttpStatus.BAD_REQUEST),
        PASSWORD_WEAK(1122, "Password must contain at least one uppercase letter, one lowercase letter, and one digit",
                        HttpStatus.BAD_REQUEST),

        // Validation errors - Name (1140-1159)
        FIRST_NAME_REQUIRED(1140, "First name is required", HttpStatus.BAD_REQUEST),
        FIRST_NAME_INVALID(1141, "First name must be between 1 and 100 characters", HttpStatus.BAD_REQUEST),
        LAST_NAME_REQUIRED(1142, "Last name is required", HttpStatus.BAD_REQUEST),
        LAST_NAME_INVALID(1143, "Last name must be between 1 and 100 characters", HttpStatus.BAD_REQUEST),

        // Validation errors - Date of birth (1160-1179)
        DOB_REQUIRED(1160, "Date of birth is required", HttpStatus.BAD_REQUEST),
        DOB_INVALID(1161, "Date of birth must be in the past", HttpStatus.BAD_REQUEST),

        // Validation errors - Token (1180-1199)
        TOKEN_REQUIRED(1180, "Token is required", HttpStatus.BAD_REQUEST),
        TOKEN_INVALID(1181, "Token is invalid", HttpStatus.BAD_REQUEST),
        TOKEN_EXPIRED(1182, "Token has expired", HttpStatus.UNAUTHORIZED),

        // Business errors - User (1200-1299)
        USER_EXISTED(1200, "User already exists", HttpStatus.CONFLICT),
        USER_NOT_EXISTED(1201, "User not found", HttpStatus.NOT_FOUND),
        USER_CREATION_FAILED(1202, "Failed to create user profile", HttpStatus.INTERNAL_SERVER_ERROR),
        
        // Business errors - Address (1250-1269)
        ADDRESS_NOT_FOUND(1250, "Address not found", HttpStatus.NOT_FOUND),
        ADDRESS_LIMIT_EXCEEDED(1251, "Maximum number of addresses reached", HttpStatus.BAD_REQUEST),

        // Authentication errors (1300-1399)
        UNAUTHENTICATED(1300, "Invalid email or password", HttpStatus.UNAUTHORIZED),
        UNAUTHORIZED(1301, "You do not have permission", HttpStatus.FORBIDDEN),
        INVALID_CREDENTIALS(1302, "Invalid email or password", HttpStatus.UNAUTHORIZED),
        ACCOUNT_NOT_ACTIVE(1303, "Account is not active. Please verify your email or contact support.",
                        HttpStatus.FORBIDDEN),
        ACCOUNT_BLOCKED(1304, "Your account has been blocked. Please contact support.", HttpStatus.FORBIDDEN),
        EMAIL_NOT_VERIFIED(1305, "Please verify your email before logging in", HttpStatus.FORBIDDEN),
        RATE_LIMITED(1306, "Too many login attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),
        EMAIL_ALREADY_VERIFIED(1307, "Email is already verified", HttpStatus.BAD_REQUEST),
        EMAIL_RESEND_COOLDOWN(1308, "Please wait before resending verification email", HttpStatus.TOO_MANY_REQUESTS),
        PASSWORD_RESET_COOLDOWN(1309, "Please wait before requesting password reset again",
                        HttpStatus.TOO_MANY_REQUESTS),
        PASSWORD_MISMATCH(1310, "New password and confirm password do not match", HttpStatus.BAD_REQUEST),
        INVALID_RESET_TOKEN(1311, "Invalid or expired password reset token", HttpStatus.BAD_REQUEST),
        INCORRECT_PASSWORD(1312, "Current password is incorrect", HttpStatus.BAD_REQUEST),
        SAME_PASSWORD(1313, "New password must be different from current password", HttpStatus.BAD_REQUEST),
        ;

        ErrorCode(int code, String message, HttpStatus statusCode) {
                this.code = code;
                this.message = message;
                this.statusCode = statusCode;
        }

        private final int code;
        private final String message;
        private final HttpStatus statusCode;
}
