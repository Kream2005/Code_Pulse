package com.stage.backend.util;

import java.time.ZonedDateTime;

/** Shared setup-token lifetime for account activation links. */
public final class SetupTokenConstants {

    /** How long a setup link stays valid (sliding window on new notifications). */
    public static final int VALIDITY_HOURS = 168;

    private SetupTokenConstants() {
    }

    public static ZonedDateTime expiresAtFromNow() {
        return ZonedDateTime.now().plusHours(VALIDITY_HOURS);
    }

    public static boolean isExpiredOrMissing(String token, ZonedDateTime expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null) {
            return true;
        }
        return expiresAt.isBefore(ZonedDateTime.now());
    }
}
