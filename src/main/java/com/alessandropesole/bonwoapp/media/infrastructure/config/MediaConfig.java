package com.alessandropesole.bonwoapp.media.infrastructure.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MediaConfig {

    @Bean
    public MediaProperties mediaProperties(Environment environment) {
        return Binder.get(environment)
                .bind("app.media", MediaProperties.class)
                .orElseThrow(() -> new IllegalStateException("Missing required 'app.media' configuration properties"));
    }
}
