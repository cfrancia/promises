package io.github.cfrancia.promises;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/**
 * Created by cfrancia on 14/08/16.
 */
class JoiningPromise<T, R> extends AbstractPromise<R> implements PromiseStep<T> {

    private final BiFunction<T, T, R> combiner;

    private final Lock combinerLock = new ReentrantLock();
    private volatile T heldValue = null;

    JoiningPromise(Executor promiseExecutor, BiFunction<T, T, R> combiner) {
        super(promiseExecutor);
        this.combiner = combiner;
    }

    @Override
    public void accept(T stepValue) {
        combinerLock.lock();
        try {
            if (heldValue == null) {
                heldValue = stepValue;
            } else {
                promiseExecutor.execute(buildFulfillmentRunnable(() -> combiner.apply(heldValue, stepValue)));
            }
        } finally {
            combinerLock.unlock();
        }
    }

    @Override
    public void failed(Exception thrownException) {
        caughtException = thrownException;
        notifyNextStepIfPresentOfFailure(thrownException);
    }

    Promise<R> associate(Promise<T> firstPromise, Promise<T> secondPromise) {
        consumePromise(firstPromise);
        consumePromise(secondPromise);

        return this;
    }

    private void consumePromise(Promise<T> promise) {
        promise.consume((result) -> {
            if (result.isOk()) {
                accept(result.getValue());
            } else {
                failed(result.getException());
            }
        });
    }

}
