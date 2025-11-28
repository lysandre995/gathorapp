package com.alfano.gathorapp.user;

/**
 * User role enumeration.
 * Defines the different user types in the system with varying privileges.
 */
public enum Role {
    /** Standard user with basic features */
    USER,
    /** Premium user with extended outing limits */
    PREMIUM,
    /** Business owner who can create events and rewards */
    BUSINESS,
    /** Administrator with full system access */
    ADMIN
}
