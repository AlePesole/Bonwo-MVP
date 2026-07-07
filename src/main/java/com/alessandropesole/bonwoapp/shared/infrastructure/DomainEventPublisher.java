package com.alessandropesole.bonwoapp.shared.infrastructure;

import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishEventsFrom(AggregateRoot aggregate) {
        aggregate.pullDomainEvents().forEach(publisher::publishEvent);
    }
}
