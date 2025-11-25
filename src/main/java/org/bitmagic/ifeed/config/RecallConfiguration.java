package org.bitmagic.ifeed.config;

import org.bitmagic.ifeed.application.recommendation.recall.core.DefaultRecallFusion;
import org.bitmagic.ifeed.application.recommendation.recall.core.DefaultRecallPlanner;
import org.bitmagic.ifeed.application.recommendation.recall.core.DefaultUserContextFactory;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallEngine;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallFusion;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallPlanner;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.core.StrategyRegistry;
import org.bitmagic.ifeed.application.recommendation.recall.core.UserContextFactory;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemFreshnessProvider;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.time.Duration;

/**
 * 召回子系统的Spring装配，统一注册策略、引擎以及线程池。
 */
@Configuration
public class RecallConfiguration {

    @Bean
    public StrategyRegistry strategyRegistry(List<RecallStrategy> strategies) {
        // 将所有策略实例注入注册表，便于运行期按ID访问
        return new StrategyRegistry(strategies);
    }

    @Bean
    public RecallPlanner recallPlanner() {
        return new DefaultRecallPlanner();
    }

    @Bean
    public RecallFusion recallFusion(ItemFreshnessProvider freshnessProvider,
                                     @Value("${recall.fusion.freshness-weight:0.25}") double freshnessWeight,
                                     @Value("${recall.fusion.freshness-half-life-hours:168}") long halfLifeHours) {
        Duration halfLife = Duration.ofHours(Math.max(1L, halfLifeHours));
        return new DefaultRecallFusion(freshnessProvider, freshnessWeight,  halfLife);
    }

    @Bean
    public UserContextFactory userContextFactory(SequenceStore sequenceStore) {
        return new DefaultUserContextFactory(sequenceStore);
    }

    @Bean
    @Qualifier("recallExecutor")
    public Executor recallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("recall-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }

    @Bean
    public RecallEngine recallEngine(StrategyRegistry registry,
                                     RecallPlanner planner,
                                     RecallFusion fusion,
                                     UserContextFactory contextFactory,
                                     @Qualifier("recallExecutor") Executor executor, ArticleRepository articleRepository) {
        // 构建多路召回引擎，对外提供统一服务
        return new RecallEngine(registry, planner, fusion, contextFactory, executor,articleRepository);
    }
}
