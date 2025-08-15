package com.example.application;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // TODO Configure your Testcontainers here.
    //  See https://docs.spring.io/spring-boot/reference/testing/testcontainers.html for details.

    @Bean
    @ServiceConnection
    public JdbcDatabaseContainer<?> postgresqlContainer() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    @Bean
    public KeycloakContainer keycloakContainer(@Value("${app.keycloak.realm}") String realmName) {
        return new KeycloakContainer("quay.io/keycloak/keycloak:26.3")
                .withRealmImportFile("/" + realmName + "-realm.json");
    }

    @Bean
    public DynamicPropertyRegistrar keycloakContainerRegistrar(KeycloakContainer keycloakContainer,
            @Value("${app.keycloak.realm}") String realmName) {
        return registry -> {
            registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri",
                    () -> keycloakContainer.getAuthServerUrl() + "/realms/" + realmName);
        };
    }

    @Bean
    public Keycloak keycloakAdminClient(KeycloakContainer keycloakContainer) {
        return keycloakContainer.getKeycloakAdminClient();
    }
}
