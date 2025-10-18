package org.bitmagic.ifeed.domain.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ArticleDailyMetricId implements Serializable {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;
}
