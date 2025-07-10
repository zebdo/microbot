package net.runelite.client.plugins.microbot.util.containers;
import java.util.LinkedList;

public class FixedSizeQueue<E> extends LinkedList<E> {
    private final int maxSize;

    public FixedSizeQueue(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be greater than 0");
        }
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(E element) {
        if (super.size() == maxSize) {
            super.removeFirst();
        }
        return super.add(element);
    }

    @Deprecated
    public LinkedList<E> getAll() {
        return this;
    }
}