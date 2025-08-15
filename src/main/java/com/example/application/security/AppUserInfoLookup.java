package com.example.application.security;

import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;

import java.util.List;
import java.util.Optional;

public interface AppUserInfoLookup {

    Optional<StandardClaimAccessor> findUserInfo(String userId);

    List<StandardClaimAccessor> findUsers(String searchTerm, int limit, int offset);
}
