package edu.du.et.chatapp.services.ai;

import edu.du.et.chatapp.models.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AIService {

  CompletableFuture<String> send(List<Message> messages, String model, Consumer<String> onChunk);

  default CompletableFuture<List<String>> fetchModels() {
    return CompletableFuture.completedFuture(List.of());
  }
}