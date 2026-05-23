package com.renuka.notification_backend.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
public class DatabaseMigrationConfig {

    @Bean("flywayMigrationExecutor")
    InitializingBean flywayMigrationExecutor(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String[] locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:1}") String baselineVersion) {
        return () -> Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
                .load()
                .migrate();
    }

    @Configuration
    static class JpaDependsOnMigration extends AbstractDependsOnBeanFactoryPostProcessor {

        JpaDependsOnMigration() {
            super(LocalContainerEntityManagerFactoryBean.class, "flywayMigrationExecutor");
        }
    }
}
