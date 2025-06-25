package com.atlassian.plugins.slack.util;

import com.atlassian.event.api.EventPublisher;
import lombok.RequiredArgsConstructor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@RequiredArgsConstructor
public class AutoSubscribingEventListener {
    protected final EventPublisher eventPublisher;

    @PostConstruct
    public void enable() {
        eventPublisher.register(this);
    }

    @PreDestroy
    public void disable() {
        eventPublisher.unregister(this);
    }
}
