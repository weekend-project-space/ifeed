package org.bitmagic.ifeed.domain.model.value;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.UserSubscriptionId;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_subscriptions")
public class UserSubscription {

    @EmbeddedId
    private UserSubscriptionId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("feedId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @CreationTimestamp
    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @PrePersist
    void onCreate() {
        if (id == null && user != null && feed != null) {
            id = new UserSubscriptionId(user.getId(), feed.getId());
        }
    }
}
