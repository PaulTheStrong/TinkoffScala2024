package com.contest.java.implementation.task2;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HandlerImpl implements Handler {

    private final Client client;
    private final Duration timeout;

    public HandlerImpl(final Client client, final Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    @Override
    public Duration timeout() {
        return timeout;
    }

    @Override
    public void performOperation() {
        CompletableFuture.supplyAsync(client::readData)
                .thenAccept(event -> event.recipients().forEach(recipient -> sendDataToRecipient(event, recipient)));

    }

    private void sendDataToRecipient(final Event event, final Address recipient) {
        Result result;
        boolean isFinished = false;
        while (!isFinished) {
            result = CompletableFuture.supplyAsync(() -> client.sendData(recipient, event.payload())).join();
            switch (result) {
                case ACCEPTED -> isFinished = true;
                case REJECTED -> {
                    try {
                        Thread.sleep(timeout());
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Thread interrupter", e);
                    }
                }
            }
        }
    }

}
