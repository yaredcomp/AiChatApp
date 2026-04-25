package edu.du.et.chatapp.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

public abstract class BaseAIService implements AIService {

    protected final HttpClient client;
    protected final ObjectMapper mapper;

    protected BaseAIService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }
    
    protected BaseAIService(ObjectMapper mapper) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = mapper;
    }
}
