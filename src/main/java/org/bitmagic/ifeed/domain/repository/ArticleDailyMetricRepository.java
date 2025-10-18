package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.ArticleDailyMetric;
import org.bitmagic.ifeed.domain.entity.id.ArticleDailyMetricId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ArticleDailyMetricRepository extends JpaRepository<ArticleDailyMetric, ArticleDailyMetricId> {

    List<ArticleDailyMetric> findByIdMetricDate(LocalDate metricDate);
}
