package org.bitmagic.ifeed.api.controller;

import org.bitmagic.ifeed.api.request.CollectionRequest;
import org.bitmagic.ifeed.api.response.CollectionFolderResponse;
import org.bitmagic.ifeed.api.response.CollectionStateResponse;
import org.bitmagic.ifeed.api.response.LikeStateResponse;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.service.UserCollectionFolderService;
import org.bitmagic.ifeed.domain.service.UserCollectionService;
import org.bitmagic.ifeed.domain.service.UserLikeService;
import org.bitmagic.ifeed.exception.BehaviorRequestExceptionHandler;
import org.bitmagic.ifeed.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserBehaviorControllerTest {

    @Mock
    private UserCollectionService collections;
    @Mock
    private UserCollectionFolderService folders;
    @Mock
    private UserLikeService likes;

    private MockMvc mvc;
    private final UUID articleUid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setId(7);
        var principal = new UserPrincipal(user);
        mvc = MockMvcBuilders.standaloneSetup(new UserCollectionController(collections),
                        new UserCollectionFolderController(folders), new UserLikeController(likes))
                .setControllerAdvice(new BehaviorRequestExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == UserPrincipal.class;
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return principal;
                    }
                }).build();
    }

    @Test
    void noBodyCollectionRemainsCompatibleAndReturnsPublicIdentifiersOnly() throws Exception {
        when(collections.addToCollection(7, articleUid, null))
                .thenReturn(new CollectionStateResponse(articleUid, null, Instant.now()));

        mvc.perform(post("/api/user/collections/{articleId}", articleUid))
                .andExpect(status().isOk()).andExpect(jsonPath("$.articleId").value(articleUid.toString()))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void malformedFolderIsBadRequestAndExplicitNullReachesService() throws Exception {
        mvc.perform(post("/api/user/collections/{articleId}", articleUid)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"folderId\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(collections);

        mvc.perform(post("/api/user/collections/{articleId}", articleUid)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"folderId\":null}"))
                .andExpect(status().isOk());
        verify(collections).addToCollection(eq(7), eq(articleUid),
                argThat(request -> request.isFolderSpecified() && request.getFolderId() == null));
    }

    @Test
    void folderCreationUses201AndDoesNotExposeInternalId() throws Exception {
        var folderUid = UUID.randomUUID();
        when(folders.create(7, "Folder"))
                .thenReturn(new CollectionFolderResponse(folderUid, "Folder", Instant.now(), Instant.now()));

        mvc.perform(post("/api/user/collection-folders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Folder\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.folderId").value(folderUid.toString()))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void invalidPaginationIsRejectedBeforeCallingService() throws Exception {
        mvc.perform(get("/api/user/likes").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/user/collections").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/user/collection-folders").param("sort", "name,wrong"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(likes, collections, folders);
    }

    @Test
    void likesExposeOnlyAddDeleteAndPaginatedList() throws Exception {
        when(likes.add(7, articleUid)).thenReturn(new LikeStateResponse(articleUid, true, Instant.now()));
        when(likes.list(eq(7), any())).thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        mvc.perform(post("/api/user/likes/{articleId}", articleUid))
                .andExpect(status().isOk()).andExpect(jsonPath("$.liked").value(true));
        mvc.perform(delete("/api/user/likes/{articleId}", articleUid)).andExpect(status().isOk());
        mvc.perform(get("/api/user/likes").param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }
}
