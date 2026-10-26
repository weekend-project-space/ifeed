package org.bitmagic.ifeed.api.controller;

import org.bitmagic.ifeed.application.tracking.TopicTrackingService;
import org.bitmagic.ifeed.api.tracking.TopicTrackCursorPage;
import org.bitmagic.ifeed.config.security.SecurityConfig;
import org.bitmagic.ifeed.config.security.TokenAuthenticationFilter;
import org.bitmagic.ifeed.domain.repository.UserSessionRepository;
import org.bitmagic.ifeed.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TopicTrackingAccessTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private TopicTrackingService service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();
        service = context.getBean(TopicTrackingService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void anonymousUsersCanBrowsePublicTopicsSummariesAndLimits() throws Exception {
        when(service.listTracks(isNull(), any())).thenReturn(Page.empty(org.springframework.data.domain.PageRequest.of(0, 50)));
        when(service.listSummaries(isNull(), isNull(), eq(false), isNull(), eq(20)))
                .thenReturn(new TopicTrackCursorPage<>(List.of(), null, false));
        var summaryUid = java.util.UUID.randomUUID();
        when(service.listSummaryArticles(eq(summaryUid), isNull(), eq(30)))
                .thenReturn(new TopicTrackCursorPage<>(List.of(), null, false));
        when(service.limits()).thenReturn(Map.of("maxCategories", 3));
        mvc.perform(get("/api/topic-tracks")).andExpect(status().isOk());
        mvc.perform(get("/api/topic-tracks/summaries")).andExpect(status().isOk()).andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/topic-tracks/summaries/" + summaryUid + "/articles")).andExpect(status().isOk());
        mvc.perform(get("/api/topic-tracks/limits")).andExpect(status().isOk()).andExpect(jsonPath("$.maxCategories").value(3));
    }

    @Test
    void anonymousUsersCannotGenerateFollowWriteOrReadPersonalSummaries() throws Exception {
        mvc.perform(get("/api/topic-tracks/suggestions").param("keyword", "AI")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/topic-tracks/following")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/topic-tracks/summaries").param("following", "true")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/topic-tracks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/topic-tracks/555f96cd-d551-4661-974c-af1737024a6a/subscribe")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, TopicTrackingController.class, GlobalExceptionHandler.class})
    static class TestConfiguration implements WebMvcConfigurer {
        @Bean
        TopicTrackingService trackingService() { return mock(TopicTrackingService.class); }

        @Bean
        TokenAuthenticationFilter authenticationFilter() {
            return new TokenAuthenticationFilter(mock(UserSessionRepository.class));
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new PageableHandlerMethodArgumentResolver());
        }
    }
}
