package org.bitmagic.ifeed.config.security;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.security.CredentialType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final AuthorizationManager<RequestAuthorizationContext> NON_MCP = AuthorizationManagers.not(
            AuthorityAuthorizationManager.hasAuthority(CredentialType.MCP.authority()));

    private static final AuthorizationManager<RequestAuthorizationContext> WEB_ADMIN = AuthorizationManagers.allOf(
            AuthorityAuthorizationManager.hasAuthority(CredentialType.WEB.authority()),
            AuthorityAuthorizationManager.hasRole("ADMIN"));

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {

        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/mcp").hasAuthority(CredentialType.MCP.authority())
                        // These reserved paths must precede the public detail patterns below.
                        .requestMatchers("/api/feeds/search", "/api/feeds/lookup", "/api/articles/insights",
                                "/api/topic-tracks/suggestions", "/api/topic-tracks/following")
                        .hasAuthority(CredentialType.WEB.authority())
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/logout")
                        .access(NON_MCP)
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/linuxdo", "/api/auth/linuxdo/callback",
                                "/api/feeds/{feedId}",
                                "/api/articles", "/api/articles/recommendations", "/api/articles/{articleId}",
                                "/api/articles/{articleId}/recommendations",
                                "/api/radar/digest", "/api/radar/topics/{topicId}",
                                "/api/discovery/categories", "/api/discovery/feeds", "/api/discovery/feeds/search",
                                "/api/topic-tracks", "/api/topic-tracks/summaries", "/api/topic-tracks/limits",
                                "/api/topic-tracks/status", "/api/topic-tracks/{topicId}",
                                "/api/topic-tracks/summaries/{summaryId}/articles",
                                "/actuator/health", "/actuator/health/**")
                        .access(NON_MCP)
                        .requestMatchers("/actuator/**").access(WEB_ADMIN)
                        .requestMatchers("/api/**").hasAuthority(CredentialType.WEB.authority())
                        .anyRequest().denyAll()
                )
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .build();
    }

    @Bean
    FilterRegistrationBean<TokenAuthenticationFilter> tokenAuthenticationFilterRegistration() {
        var registration = new FilterRegistrationBean<>(tokenAuthenticationFilter);
        registration.setEnabled(false); // Run only inside the security chain, after its CORS filter.
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
