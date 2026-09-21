package org.bitmagic.ifeed.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.bitmagic.ifeed.api.controller.UserCollectionController;
import org.bitmagic.ifeed.api.controller.UserCollectionFolderController;
import org.bitmagic.ifeed.api.controller.UserHistoryController;
import org.bitmagic.ifeed.api.controller.UserLikeController;
import org.bitmagic.ifeed.api.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {UserCollectionController.class, UserCollectionFolderController.class,
        UserHistoryController.class, UserLikeController.class})
public class BehaviorRequestExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> invalidBody(HttpServletRequest request) {
        var status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), "Invalid request body", request.getRequestURI()));
    }
}
