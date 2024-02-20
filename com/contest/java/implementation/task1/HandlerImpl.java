package com.contest.java.implementation.task1;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class HandlerImpl implements Handler {

    private final Client client;

    public HandlerImpl(Client client) {
        this.client = client;
    }

    private static class HandlerTask {

        private final Function<String, Response> getApplicationStatusFunction;
        private final String id;
        private final AtomicInteger retries;
        private final AtomicReference<Duration> lastRequestTime;

        public HandlerTask(Function<String, Response> getApplicationStatusFunction, String id, AtomicInteger counter, AtomicReference<Duration> lastRequestTime) {
            this.getApplicationStatusFunction = getApplicationStatusFunction;
            this.id = id;
            this.retries = counter;
            this.lastRequestTime = lastRequestTime;
        }

        private Response handleRetry(Response.RetryAfter response) {
            Executor executor = CompletableFuture.delayedExecutor(response.delay().toMillis(), TimeUnit.MILLISECONDS);
            retries.incrementAndGet();
            lastRequestTime.set(Duration.ofNanos(System.nanoTime()));
            return CompletableFuture.supplyAsync(() -> getApplicationStatusFunction.apply(id), executor).join();
        }

        private Response handleResponse(Response response) {
            boolean isFinished = false;
            while (!isFinished) {
                switch (response) {
                    case Response.Success r -> isFinished = true;
                    case Response.Failure r -> isFinished = true;
                    case Response.RetryAfter r -> response = handleRetry(r);
                }
            }
            return response;
        }

        public Response run() {
            lastRequestTime.set(Duration.ofNanos(System.nanoTime()));
            return handleResponse(getApplicationStatusFunction.apply(id));
        }
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        AtomicInteger retries = new AtomicInteger(0);
        AtomicReference<Duration> lastRequestTime = new AtomicReference<>();
        HandlerTask handlerTask1 = new HandlerTask(client::getApplicationStatus1, id, retries, lastRequestTime);
        HandlerTask handlerTask2 = new HandlerTask(client::getApplicationStatus2, id, retries, lastRequestTime);

        Object result;
        CompletableFuture<Object> anyResult = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(handlerTask1::run),
                CompletableFuture.supplyAsync(handlerTask2::run)
        );
        try {
            result = anyResult.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new Response.Failure(e);
        }

        if (result instanceof Response.Failure) {
            return new ApplicationStatusResponse.Failure(lastRequestTime.get(), retries.get());
        } else {
            Response.Success success = (Response.Success) result;
            return new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus());
        }
    }
}
