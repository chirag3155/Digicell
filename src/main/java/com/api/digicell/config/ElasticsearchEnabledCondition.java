package com.api.digicell.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ElasticsearchEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Read the 'elasticsearch.enabled' property from the application configuration
        String elasticsearchEnabled = context.getEnvironment().getProperty("elasticsearch.enabled", "true");

        // If 'elasticsearch.enabled' is 'true', we will proceed with the bean creation, otherwise, it will be skipped
        return Boolean.parseBoolean(elasticsearchEnabled);
    }
}
