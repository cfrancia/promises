package io.github.cfrancia.promises;

import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Created by cfrancia on 14/08/16.
 */
class MutatingPromise<T, R> extends AbstractPromise<R> implements PromiseStep<T> {

    private final Function<T, R> mutatorFunction;

    MutatingPromise(Executor promiseExecutor, Function<T, R> mutatorFunction) {
        super(promiseExecutor);
        this.mutatorFunction = mutatorFunction;
    }

    @Override
    public void accept(T stepValue) {
        promiseExecutor.execute(buildFulfillmentRunnable(() -> mutatorFunction.apply(stepValue)));
    }

    @Override
    public void failed(Exception thrownException) {
        caughtException = thrownException;
        notifyNextStepIfPresentOfFailure(thrownException);
    }

}
