package com.timecapsule.auth;

import com.timecapsule.audit.AuditLogger;
import com.timecapsule.security.CryptoService;
import com.timecapsule.storage.UserDao;

import javax.crypto.SecretKey;
import java.sql.SQLException;

/**
 * Registration, login, and the in-memory session (Module: Auth & Identity).
 * The session holds the signed-in keeper plus their AES key (derived from
 * the password, never written to disk). All failures speak keeper-voiced
 * {@link AuthException}s for the cover form.
 */
public final class AuthService {

    private static final UserDao USERS = new UserDao();

    private static User currentUser;
    private static SecretKey sessionKey;

    private AuthService() {
    }

    /** Registers a keeper and signs them straight in. */
    public static User register(String username, String password) {
        String name = validateUsername(username);
        validatePassword(password);
        try {
            if (USERS.findByUsername(name).isPresent()) {
                throw new AuthException("That keeper name is already bound to these pages.");
            }
            byte[] salt = CryptoService.newSalt();
            long id = USERS.create(name, PasswordUtil.hash(password), salt);
            User user = USERS.findById(id)
                    .orElseThrow(() -> new AuthException("The ink would not dry — try again."));
            openSession(user, password.toCharArray());
            AuditLogger.log(user.getId(), "register");
            return user;
        } catch (SQLException e) {
            throw new AuthException("The grimoire would not open its pages (database error).", e);
        }
    }

    /** Signs a keeper in and derives their session key. */
    public static User login(String username, String password) {
        String name = username == null ? "" : username.strip();
        try {
            User user = USERS.findByUsername(name)
                    .orElseThrow(() -> new AuthException("No keeper by that name is bound to this book."));
            if (password == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                throw new AuthException("That is not the secret word. The grimoire does not budge.");
            }
            openSession(user, password.toCharArray());
            AuditLogger.log(user.getId(), "login");
            return user;
        } catch (SQLException e) {
            throw new AuthException("The grimoire would not open its pages (database error).", e);
        }
    }

    /** Signs out and drops the session key from memory. */
    public static void logout() {
        if (currentUser != null) {
            AuditLogger.log(currentUser.getId(), "logout");
        }
        currentUser = null;
        sessionKey = null;
    }

    public static User currentUser() {
        return currentUser;
    }

    /** The signed-in keeper's AES key; throws when nobody is signed in. */
    public static SecretKey sessionKey() {
        if (sessionKey == null) {
            throw new AuthException("No keeper is signed in.");
        }
        return sessionKey;
    }

    private static void openSession(User user, char[] password) {
        try {
            currentUser = user;
            sessionKey = CryptoService.deriveKey(password, user.getEncKeySalt());
        } catch (Exception e) {
            currentUser = null;
            sessionKey = null;
            throw new AuthException("The wards rejected that secret word.", e);
        }
    }

    private static String validateUsername(String username) {
        String name = username == null ? "" : username.strip();
        if (name.length() < 3 || name.length() > 32) {
            throw new AuthException("Keeper names run 3 to 32 letters.");
        }
        return name;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException("Secret words need at least 6 characters.");
        }
    }
}
