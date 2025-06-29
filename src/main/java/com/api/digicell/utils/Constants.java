package com.api.digicell.utils;

public class Constants {
    // JWT Token Types
    public static final String TOKEN_TYPE = "tokenType"; // -- claims data
	public static final String USER_TYPE = "userType";
	public static final String USER_ID = "userId";
	public static final String TYPE_REFRESH_TOKEN = "refresh";
	public static final String TYPE_AUTH_TOKEN = "auth";
	public static final String TYPE_EMAIL_TOKEN = "email";
	public static final String TYPE_HE_TOKEN = "he";
    public static final String TYPE_OTP_TOKEN = "otp";
    public static final String TENANT_ID = "tenantId";
    // Authorization
    public static final String BEARER = "Bearer ";

    // Claims
    public static final String IP_ADDRESS = "ipAddress";

    // Response Status
    public static final String UNSUCCESSFUL = "UNSUCCESSFUL";

    // Error Codes
    public static final int UNAUTHORIZED_JWT_CODE = 401;
    public static final int EXPIRED_JWT_CODE = 403;
    public static final int UNAUTHORIZED_CODE = 401;

    // Error Messages
    public static final String INVALID_JWT_TOKEN = "INVALID_JWT_TOKEN";
    public static final String JWT_TOKEN_EXPIRED = "JWT_TOKEN_EXPIRED";
    public static final String UNAUTHORIZED_REQUEST = "UNAUTHORIZED_REQUEST";

    // Configuration
    public static final String DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE";
} 