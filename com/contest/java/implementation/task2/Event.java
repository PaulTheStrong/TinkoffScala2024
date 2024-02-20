package com.contest.java.implementation.task2;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {
}
