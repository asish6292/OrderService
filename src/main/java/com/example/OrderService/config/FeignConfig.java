package com.example.OrderService.config;

import com.example.OrderService.external.error.CustomErrorDecoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;
import brave.Tracer;

@Configuration
public class FeignConfig {

    @Bean
    ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    //This bean is most as trace id not propagating automatically(NOTE: we are using micrometer trace brave not sleuth as outdated)
    @Bean
    public RequestInterceptor requestInterceptor(Tracer tracer) {
        return requestTemplate -> {
            if (tracer.currentSpan() != null) {
                // Add trace headers manually to the Feign request
                requestTemplate.header("X-B3-TraceId", tracer.currentSpan().context().traceIdString());
                requestTemplate.header("X-B3-SpanId", tracer.currentSpan().context().spanIdString());
            }
        };
    }
}
