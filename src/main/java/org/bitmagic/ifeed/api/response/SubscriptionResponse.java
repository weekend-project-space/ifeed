package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubscriptionResponse(@JsonProperty("feed_id") String feedId,
                                    String title,
                                    String url) {
}
