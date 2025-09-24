package com.lj.aichatapp.services;

import com.lj.aichatapp.models.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generic AI provider interface. Implementations should perform inference and
 * return assistant messages as a CompletableFuture.
 */
public interface AIProvider {

    /**
     * Send conversation messages to model and get assistant reply(s). The
     * method should be non-blocking and return a CompletableFuture.
     * @param conversation
     * @param model
     * @return 
     */
    CompletableFuture<String> sendMessage(List<Message> conversation, String model);
}
