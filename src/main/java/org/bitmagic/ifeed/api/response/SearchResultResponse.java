package org.bitmagic.ifeed.api.response;

public record SearchResultResponse(String id, String title, String summary, String thumbnail, String feedTitle, String timeAgo, Double score) {
}
