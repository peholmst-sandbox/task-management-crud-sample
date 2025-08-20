package com.example.application.security;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EnableMethodSecurity
@Configuration
class SecurityConfig {

    @Bean
    UserInfoLookup userInfoLookup(Keycloak keycloak, @Value("${app.keycloak.realm}") String realm) {
        return new UserInfoLookup(keycloak, realm);
    }

    @Bean
    public GrantedAuthoritiesMapper authoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    List<String> roles = oidcUserAuthority.getUserInfo().getClaimAsStringList("roles");
                    if (roles != null) {
                        roles.stream().map(role -> "ROLE_" + role.toUpperCase()).map(SimpleGrantedAuthority::new)
                                .forEach(mappedAuthorities::add);
                    }
                }
                mappedAuthorities.add(authority);
            });

            return mappedAuthorities;
        };
    }

    @Bean
    @Profile("prod")
    Keycloak keycloakAdminClient(@Value("${app.keycloak.admin.client-id}") String clientId,
                                 @Value("${app.keycloak.admin.client-secret}") String clientSecret,
                                 @Value("${app.keycloak.admin.url}") String serverUrl,
                                 @Value("${app.keycloak.realm}") String realm) {
        return KeycloakBuilder.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType("client_credentials")
                .build();
    }
}
