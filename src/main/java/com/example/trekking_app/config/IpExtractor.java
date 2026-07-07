package com.example.trekking_app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class IpExtractor {
    private static final List<String> HEADERS = List.of(
            "X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP", "Proxy-Client-IP"
    );
    public String extract(HttpServletRequest request) {
        for (String h : HEADERS) {
            String val = request.getHeader(h);
            if (val != null && !val.isBlank() && !"unknown".equalsIgnoreCase(val))
                return val.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}