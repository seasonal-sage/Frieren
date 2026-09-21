package com.timecapsule.auth;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt password hashing (Module: Auth & Identity). */
public final class PasswordUtil {

    private static final int COST = 12;

    private PasswordUtil() {
    }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST));
    }

    /** Never throws — malformed input simply does not verify. */
    public static boolean verify(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
