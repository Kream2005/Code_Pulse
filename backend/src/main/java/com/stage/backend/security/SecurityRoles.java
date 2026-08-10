package com.stage.backend.security;

public final class SecurityRoles {

    private SecurityRoles() {}

    public static final String USER = "hasRole('USER')";

    public static final String ADMIN_CHALLENGE = "hasAnyRole('ADMIN_CODING_CHALLENGE','ADMIN_CODEPULSE')";

    public static final String ADMIN_CODEPULSE = "hasRole('ADMIN_CODEPULSE')";

    public static final String READ_FEEDBACKS = "hasAnyRole('ADMIN_CODING_CHALLENGE','MANAGER_RH','ADMIN_CODEPULSE')";

    public static final String ANALYTICS = "hasAnyRole('MANAGER_RH','ADMIN_CODEPULSE')";

    public static final String READ_LOGS = "hasRole('ADMIN_CODEPULSE')";

    public static final String MANAGE_QUESTIONS = "hasRole('ADMIN_CODEPULSE')";

    public static final String USER_OR_READ_FEEDBACKS =
            "hasRole('USER') or hasAnyRole('ADMIN_CODING_CHALLENGE','MANAGER_RH','ADMIN_CODEPULSE')";

    public static final String AUTHENTICATED = "isAuthenticated()";
}
