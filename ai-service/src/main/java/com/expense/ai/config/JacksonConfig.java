package com.expense.ai.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Jackson configuration to prevent scientific notation in JSON responses
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Disable scientific notation for numbers
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

        // Custom serializer to write double as plain number (not scientific notation)
        SimpleModule module = new SimpleModule();
        module.addSerializer(Double.class, new PlainDoubleSerializer());
        module.addSerializer(double.class, new PlainDoubleSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }

    /**
     * Custom serializer that writes double values without scientific notation
     */
    private static class PlainDoubleSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                // Write as plain number without scientific notation
                DecimalFormat df = new DecimalFormat("#");
                df.setMaximumFractionDigits(8);
                gen.writeNumber(df.format(value));
            }
        }
    }
}
