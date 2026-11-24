package org.bitmagic.ifeed.api.util;

import lombok.experimental.UtilityClass;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@UtilityClass
public class IdentifierUtils {

    public UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName);
        }
    }
}
