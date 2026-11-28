package org.bitmagic.ifeed.config.security;

import org.bitmagic.ifeed.security.UserPrincipal;

import jakarta.servlet.Filter;
import org.bitmagic.ifeed.IFeedApplication;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.service.JwtTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the security chain with the application's actual CORS policy. */
class SecurityConfigTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private JwtTokenService tokens;
    private ProbeController endpoints;
    private static final String ID = "555f96cd-d551-4661-974c-af1737024a6a";

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        tokens = context.getBean(Config.class).tokens;
        endpoints = context.getBean(ProbeController.class);
    }

    @AfterEach
    void tearDown() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        context.close();
    }

    @Test
    void anonymousPublicQueriesAndLoginEndpointsRemainAvailable() throws Exception {
        for (String path : List.of("/api/articles", "/api/articles/recommendations", "/api/articles/" + ID,
                "/api/articles/" + ID + "/recommendations", "/api/feeds/" + ID,
                "/api/radar/digest", "/api/radar/topics/" + ID,
                "/api/discovery/categories", "/api/discovery/feeds", "/api/discovery/feeds/search",
                "/api/topic-tracks", "/api/topic-tracks/limits", "/api/topic-tracks/status", "/api/topic-tracks/summaries",
                "/api/topic-tracks/" + ID, "/api/topic-tracks/summaries/" + ID + "/articles",
                "/api/auth/linuxdo", "/api/auth/linuxdo/callback", "/actuator/health", "/actuator/health/readiness")) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        for (String path : List.of("/api/auth/login", "/api/auth/register", "/api/auth/logout")) {
            mvc.perform(post(path)).andExpect(status().isOk());
        }
        verifyNoInteractions(tokens);
    }

    @Test
    void anonymousPersonalRequestsStopBeforeController() throws Exception {
        for (String path : List.of("/api/subscriptions", "/api/subscriptions-extra", "/api/feeds/search", "/api/feeds/lookup",
                "/api/articles/insights", "/api/user", "/api/user/mcp-tokens", "/api/user/likes", "/api/user/collections",
                "/api/topic-tracks/suggestions", "/api/topic-tracks/following", "/api/auth/review-probe")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
        }
        mvc.perform(post("/api/subscriptions")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/subscriptions/" + ID)).andExpect(status().isUnauthorized());
        assertThat(endpoints.calls).hasValue(0);
    }

    @Test
    void publicReadPatternsDoNotAuthorizeAnonymousWrites() throws Exception {
        for (String path : List.of("/api/articles/" + ID + "/enrich", "/api/radar/digest", "/api/discovery/feeds",
                "/api/topic-tracks", "/api/user/mcp-tokens")) {
            mvc.perform(post(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(put("/api/feeds/" + ID)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/articles/" + ID)).andExpect(status().isUnauthorized());
        assertThat(endpoints.calls).hasValue(0);
    }

    @Test
    void webCredentialCanReachPersonalEndpointsButCannotInvokeMcp() throws Exception {
        mvc.perform(get("/api/subscriptions").header("Authorization", "Bearer web-user"))
                .andExpect(status().isOk()).andExpect(content().string("user-7"));
        mvc.perform(post("/api/user/mcp-tokens").header("Authorization", "bearer web-user"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/mcp").header("Authorization", "Bearer web-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mcpCredentialsAreConfinedToMcpEvenWhenUserIsAdmin() throws Exception {
        for (String token : List.of("mcp-user", "mcp-admin")) {
            mvc.perform(post("/api/mcp").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            for (String path : List.of("/api/articles", "/api/radar/digest", "/api/subscriptions",
                    "/api/user/mcp-tokens", "/actuator/health", "/actuator/metrics")) {
                mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            }
            mvc.perform(post("/api/auth/login").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/user/mcp-tokens").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        assertThat(endpoints.calls).hasValue(2);
    }

    @Test
    void onlyWebAdminsCanReadOtherActuatorEndpoints() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").header("Authorization", "Bearer web-user")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").header("Authorization", "Bearer web-admin")).andExpect(status().isOk());
        assertThat(endpoints.calls).hasValue(1);
    }

    @Test
    void invalidAndExpiredTokensNeverFallBackToAnonymous() throws Exception {
        for (String token : List.of("invalid", "expired")) {
            mvc.perform(get("/api/articles").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", containsString("invalid_token")));
            mvc.perform(get("/api/user").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/mcp").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
        assertThat(endpoints.calls).hasValue(0);
    }

    @Test
    void malformedBearerHeadersAreRejectedAndUrlTokensAreIgnored() throws Exception {
        for (String value : List.of("Bearer ", "Bearer", "Bearer two tokens", "Bearer invalid!")) {
            mvc.perform(get("/api/articles").header("Authorization", value)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/user").param("access_token", "web-user")).andExpect(status().isUnauthorized());
        assertThat(endpoints.calls).hasValue(0);
        verifyNoInteractions(tokens);
    }

    @Test
    void allowedCorsPreflightDoesNotRequireAuthenticationAndActualRequestsStillDo() throws Exception {
        for (String origin : List.of("http://localhost:5173", "https://www.ifeed.cc")) {
            mvc.perform(options("/api/user/mcp-tokens").header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
        mvc.perform(get("/api/user").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/api/user").header("Origin", "http://localhost:5173").header("Authorization", "Bearer web-user"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        verify(tokens).authenticate("web-user");
    }

    @Test
    void untrustedCorsOriginsAreRejectedBeforeAuthentication() throws Exception {
        mvc.perform(options("/api/user/mcp-tokens").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/api/user").header("Origin", "https://untrusted.example").header("Authorization", "Bearer web-user"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        verifyNoInteractions(tokens);
        assertThat(endpoints.calls).hasValue(0);
    }

    @Test
    void filterIsNotRegisteredTwiceAndNoHttpSessionIsCreated() throws Exception {
        assertThat(context.getBean(FilterRegistrationBean.class).isEnabled()).isFalse();
        var result = mvc.perform(get("/api/user").header("Authorization", "Bearer web-user")).andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        verify(tokens, times(1)).authenticate("web-user");
        mvc.perform(get("/api/user")).andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config implements WebMvcConfigurer {
        private JwtTokenService tokens;

        @Bean TokenAuthenticationFilter authenticationFilter() {
            tokens = mock(JwtTokenService.class);
            for (String token : List.of("web-user", "web-admin", "mcp-user", "mcp-admin")) {
                var user = new User();
                user.setId(7);
                user.setUsername(token);
                user.setRole(token.endsWith("admin") ? User.UserRole.ADMIN : User.UserRole.USER);
                when(tokens.authenticate(token)).thenReturn(new JwtTokenService.Identity(user, token.startsWith("mcp")));
            }
            when(tokens.authenticate("invalid")).thenThrow(new BadCredentialsException("invalid"));
            when(tokens.authenticate("expired")).thenThrow(new BadCredentialsException("expired"));
            return new TokenAuthenticationFilter(tokens);
        }

        @Bean ProbeController endpoints() { return new ProbeController(); }

        @Override public void addCorsMappings(CorsRegistry registry) {
            new IFeedApplication.IFeedMvcConfigurer().addCorsMappings(registry);
        }
    }

    @RestController
    static class ProbeController {
        final AtomicInteger calls = new AtomicInteger();

        @RequestMapping(value = {"/api/articles", "/api/articles/{id}", "/api/articles/{id}/recommendations",
                "/api/feeds/{id}", "/api/radar/digest", "/api/radar/topics/{id}", "/api/discovery/categories",
                "/api/discovery/feeds", "/api/discovery/feeds/search", "/api/topic-tracks", "/api/topic-tracks/{id}",
                "/api/topic-tracks/summaries/{id}/articles", "/api/auth/linuxdo", "/api/auth/linuxdo/callback",
                "/actuator/health", "/actuator/health/readiness", "/actuator/metrics"}, method = RequestMethod.GET)
        String query() { calls.incrementAndGet(); return "ok"; }

        @GetMapping({"/api/subscriptions", "/api/subscriptions-extra", "/api/feeds/search", "/api/feeds/lookup",
                "/api/articles/insights", "/api/user", "/api/user/mcp-tokens", "/api/user/likes", "/api/user/collections",
                "/api/topic-tracks/suggestions", "/api/topic-tracks/following", "/api/auth/review-probe"})
        String personal(@AuthenticationPrincipal UserPrincipal principal) {
            calls.incrementAndGet();
            return "user-" + principal.getId();
        }

        @RequestMapping(value = {"/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/mcp", "/api/subscriptions",
                "/api/subscriptions/{id}", "/api/articles/{id}/enrich", "/api/radar/digest", "/api/discovery/feeds",
                "/api/topic-tracks", "/api/user/mcp-tokens", "/api/feeds/{id}", "/api/articles/{id}"},
                method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
        String write() { calls.incrementAndGet(); return "ok"; }
    }
}
