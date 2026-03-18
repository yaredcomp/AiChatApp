package com.lj.aichatapp.service.ai;

import com.lj.aichatapp.models.Message;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AIService {

    CompletableFuture<String> send(List<Message> messages, String model, Consumer<String> onChunk);
}