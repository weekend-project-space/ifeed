package org.bitmagic.ifeed.api.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public class CollectionRequest {

    private UUID folderId;
    private boolean folderSpecified;

    @JsonSetter("folderId")
    public void setFolderId(String folderId) {
        this.folderId = folderId == null ? null : UUID.fromString(folderId);
        this.folderSpecified = true;
    }

    public UUID getFolderId() {
        return folderId;
    }

    @JsonIgnore
    public boolean isFolderSpecified() {
        return folderSpecified;
    }
}
