package com.sentinel.surveillance.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple authentication system with hardcoded users and SHA-256 password hashing.
 * Supports two roles: ADMIN (full access) and OPERATOR (view-only).
 */
public class UserAuth {

    /** User roles defining UI access levels */
    public enum Role {
        ADMIN,      // Full access: all panels, report generation, configuration
        OPERATOR    // Limited: view alerts and camera feeds only
    }

    /** Internal user record */
    private static class UserRecord {
        final String username;
        final String passwordHash;
        final Role role;

        UserRecord(String username, String password, Role role) {
            this.username = username;
            this.passwordHash = hashPassword(password);
            this.role = role;
        }
    }

    // Hardcoded user database
    private static final Map<String, UserRecord> USERS = new HashMap<>();
    static {
        USERS.put("admin", new UserRecord("admin", "admin", Role.ADMIN));
        USERS.put("operator", new UserRecord("operator", "operator", Role.OPERATOR));
        USERS.put("commander", new UserRecord("commander", "sentinel2024", Role.ADMIN));
    }

    // Current session
    private static String currentUser = null;
    private static Role currentRole = null;

    /**
     * Authenticates a user against the hardcoded database.
     * @return true if credentials are valid, false otherwise
     */
    public static boolean authenticate(String username, String password) {
        UserRecord record = USERS.get(username.toLowerCase().trim());
        if (record == null) return false;

        String inputHash = hashPassword(password);
        if (record.passwordHash.equals(inputHash)) {
            currentUser = record.username;
            currentRole = record.role;
            System.out.println("[AUTH] User '" + username + "' authenticated as " + record.role);
            return true;
        }
        System.out.println("[AUTH] Failed login attempt for user: " + username);
        return false;
    }

    /** Get currently logged-in username */
    public static String getCurrentUser() { return currentUser; }

    /** Get current user's role */
    public static Role getCurrentRole() { return currentRole; }

    /** Check if current user is admin */
    public static boolean isAdmin() { return currentRole == Role.ADMIN; }

    /** Logout current user */
    public static void logout() {
        System.out.println("[AUTH] User '" + currentUser + "' logged out");
        currentUser = null;
        currentRole = null;
    }

    /**
     * SHA-256 password hashing.
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
