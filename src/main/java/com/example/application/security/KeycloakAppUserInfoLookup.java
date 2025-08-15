package com.example.application.security;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * For this to work, the application's client account must:
 * <ul>
 * <li>Be a <strong>service account</strong></li>
 * <li>Have the following service account roles: {@code view-users}, {@code query-users}</li>
 * </ul>
 */
class KeycloakAppUserInfoLookup implements AppUserInfoLookup, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAppUserInfoLookup.class);

    private final Keycloak keycloak;
    private final String realm;

    KeycloakAppUserInfoLookup(Keycloak keycloak, String realm) {
        this.keycloak = requireNonNull(keycloak, "keycloak must not be null");
        this.realm = requireNonNull(realm, "realm must not be null");
    }

    @Override
    public Optional<StandardClaimAccessor> findUserInfo(String userId) {
        try {
            log.debug("Looking up user info for userId: {}", userId);
            var user = keycloak.realm(realm).users().get(userId).toRepresentation();
            return Optional.of(new KeycloakUserInfo(user));
        } catch (NotFoundException ex) {
            log.debug("User not found in Keycloak: {}", userId);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to lookup user info for userId: {}", userId, ex);
            throw new RuntimeException("Failed to retrieve user information from Keycloak", ex);
        }
    }

    @Override
    public List<StandardClaimAccessor> findUsers(String searchTerm, int limit, int offset) {
        log.debug("Looking up users from searchTerm: {} (limit: {}, offset: {})", searchTerm, limit, offset);
        var users = keycloak.realm(realm).users().search(searchTerm, offset, limit);
        try {
            log.debug("Found {} users from Keycloak", users.size());
            return users.stream().map(KeycloakUserInfo::new).collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to lookup users from searchTerm: {}", searchTerm, ex);
            throw new RuntimeException("Failed to retrieve users from Keycloak", ex);
        }
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        keycloak.close();
    }

    private static class KeycloakUserInfo implements StandardClaimAccessor {

        private final Map<String, Object> claims;

        KeycloakUserInfo(UserRepresentation user) {
            var claims = new HashMap<String, Object>();
            // Required claims
            claims.put(StandardClaimNames.SUB, requireNonNull(user.getId()));
            claims.put(StandardClaimNames.PREFERRED_USERNAME, requireNonNull(user.getUsername()));
            claims.put(StandardClaimNames.NAME, buildFullName(user));

            // Optional claims
            Optional.ofNullable(user.getEmail()).ifPresent(email -> claims.put(StandardClaimNames.EMAIL, email));
            Optional.ofNullable(user.firstAttribute(StandardClaimNames.PROFILE))
                    .ifPresent(profile -> claims.put(StandardClaimNames.PROFILE, profile));
            Optional.ofNullable(user.firstAttribute(StandardClaimNames.PICTURE))
                    .ifPresent(profile -> claims.put(StandardClaimNames.PICTURE, profile));

            this.claims = Collections.unmodifiableMap(claims);
        }

        private static String buildFullName(UserRepresentation user) {
            var firstName = user.getFirstName();
            var lastName = user.getLastName();

            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            } else {
                return user.getUsername(); // Fallback to username
            }
        }

        @Override
        public Map<String, Object> getClaims() {
            return claims;
        }
    }
}
