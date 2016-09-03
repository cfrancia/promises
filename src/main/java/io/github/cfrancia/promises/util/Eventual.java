package io.github.cfrancia.promises.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

/**
 * Created by cfrancia on 14/08/16.
 */
public class Eventual<T> {
    private volatile T storedItem = null;

    private Lock eventualLock = new ReentrantLock();
    private Condition eventualCondition = eventualLock.newCondition();

    public void place(T item) {
        requireNonNull(item);

        eventualLock.lock();
        try {
            if (storedItem != null) {
                throw new IllegalStateException("Tried to place an additional item into an already filled Eventual");
            }

            storedItem = item;
            eventualCondition.signalAll();
        } finally {
            eventualLock.unlock();
        }
    }

    public T view() {
        return storedItem;
    }

    public T view(int timeout, TimeUnit timeUnit) throws InterruptedException {
        eventualLock.lock();
        try {
            if (storedItem != null) {
                return storedItem;
            }

            eventualCondition.await(timeout, timeUnit);
            return storedItem;
        } finally {
            eventualLock.unlock();
        }
    }
}
