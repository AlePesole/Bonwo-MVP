package com.alessandropesole.bonwoapp.shared.domain;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}