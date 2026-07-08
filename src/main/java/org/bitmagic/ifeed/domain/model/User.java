package org.bitmagic.ifeed.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 64)
    private String linuxDoUserId;

    /**
     * 套餐
     */
    @Column(length = 16, nullable = false)
    @Enumerated(EnumType.STRING)
    private Plan currentPlan;

    private Instant currentPlanCreatedAt;

    {
        currentPlan = Plan.FREE;
    }

    @AllArgsConstructor
    @Getter
    public enum Plan {
        FREE(60, 0, 1), STANDARD(300, 2, 2), PRO(2000, 6, 6);
        private final int maxSubscriptions;
        private final int maxCreatedMixFeed;
        private final int maxWebhooks;
    }

    public void changePlan(Plan plan) {
        this.currentPlan = plan;
        this.currentPlanCreatedAt = Instant.now();
    }
}
