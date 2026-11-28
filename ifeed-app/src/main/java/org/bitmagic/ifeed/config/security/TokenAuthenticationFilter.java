package org.bitmagic.ifeed.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.service.JwtTokenService;
import org.bitmagic.ifeed.security.CredentialType;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();
    private final BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = tokenResolver.resolve(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var identity = jwtTokenService.authenticate(token);
                var principal = new UserPrincipal(identity.user());
                var type = identity.mcp() ? CredentialType.MCP : CredentialType.WEB;
                var authorities = new ArrayList<GrantedAuthority>(principal.getAuthorities());
                authorities.add(new SimpleGrantedAuthority(type.authority()));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, exception instanceof OAuth2AuthenticationException ? exception
                    : new InvalidBearerTokenException("Invalid bearer token", exception));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
