package com.contest.java.implementation.task2;

import java.time.Duration;

public interface Handler {
    Duration timeout();

    void performOperation();
}
