package net.runelite.client.plugins.microbot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlockingEventPriority {
    HIGH(10),
    NORMAL(0),
    LOW(-10);

    private final int level;
}
