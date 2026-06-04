package com.example.trekking_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping
    public Map<String, String> debug(HttpServletRequest request) {

        Map<String, String> result = new HashMap<>();

        result.put("scheme", request.getScheme());
        result.put("serverName", request.getServerName());
        result.put("forwardedProto",
                request.getHeader("X-Forwarded-Proto"));

        return result;
    }
}
