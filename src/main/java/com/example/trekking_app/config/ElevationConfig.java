package com.example.trekking_app.config;
import com.example.trekking_app.dto.contour.ElevationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ElevationProperties.class)
class ElevationConfig {

    @Bean
    RestClient elevationRestClient(ElevationProperties p) {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(p.timeout());
        rf.setReadTimeout(p.timeout());
        return RestClient.builder()
                .baseUrl(p.baseUrl())
                .requestFactory(rf)
                .defaultHeaders(h -> {
                    if (p.apiKey() != null && !p.apiKey().isBlank()) h.set("X-API-Key", p.apiKey());
                })
                .build();
    }
}
