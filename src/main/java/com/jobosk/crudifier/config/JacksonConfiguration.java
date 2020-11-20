package com.jobosk.crudifier.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addCustomSerializationFeatures() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.featuresToEnable(
                SerializationFeature.HANDLE_CIRCULAR_REFERENCE_INDIVIDUALLY_FOR_COLLECTIONS
        );
    }
}
