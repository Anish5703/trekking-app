package com.example.trekking_app.service;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
public class GeoOfflineException extends ResponseStatusException {
    private final String code;
    public GeoOfflineException(int status, String code, String message) { super(HttpStatusCode.valueOf(status), message); this.code=code; }
    public String getCode(){return code;}
}
