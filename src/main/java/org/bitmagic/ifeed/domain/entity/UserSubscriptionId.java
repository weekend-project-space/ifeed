package org.bitmagic.ifeed.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserSubscriptionId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "feed_id", nullable = false)
    private Integer feedId;
}
