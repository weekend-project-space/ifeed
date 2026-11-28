package org.bitmagic.ifeed.security;

import org.springframework.security.core.Authentication;

/** Credential type is independent of the user's role and plan. */
public enum CredentialType {
    WEB, MCP;

    private final String authority = "CREDENTIAL_" + name();

    public String authority() {
        return authority;
    }

    public boolean matches(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(grant -> authority.equals(grant.getAuthority()));
    }
}
