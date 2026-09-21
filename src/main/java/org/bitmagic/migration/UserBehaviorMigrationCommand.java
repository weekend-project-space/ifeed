package org.bitmagic.migration;

import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.infrastructure.migration.UserBehaviorMigration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

public class UserBehaviorMigrationCommand {

    public static void main(String[] args) {
        try (var context = new SpringApplicationBuilder(MigrationConfiguration.class)
                .web(WebApplicationType.NONE)
                .properties("ifeed.migration.user-behavior.enabled=true")
                .run(args)) {
            if (context.getBeansOfType(UserBehaviorMigration.class).isEmpty()) {
                throw new IllegalStateException("User behavior migration is disabled");
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Article.class)
    @EnableJpaRepositories(basePackageClasses = ArticleRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {ArticleRepository.class, UserRepository.class}))
    @Import({UserBehaviorMigration.class, UserReadHistoryRepository.class, UserCollectionRepository.class})
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class,
            MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, FlywayAutoConfiguration.class})
    static class MigrationConfiguration {
    }
}
