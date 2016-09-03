package io.github.cfrancia.promises;

import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Created by cfrancia on 14/08/16.
 */
class ShortcircuitingPromise<T> extends AbstractPromise<T> implements PromiseStep<T> {

    private final Function<Exception, T> alternateSupplier;

    ShortcircuitingPromise(Executor promiseExecutor, Function<Exception, T> alternateSupplier) {
        super(promiseExecutor);
        this.alternateSupplier = alternateSupplier;
    }

    @Override
    public void accept(T stepValue) {
        eventualValue.place(stepValue);
        notifyNextStepIfPresentOfSuccess(stepValue);
    }

    @Override
    public void failed(Exception thrownException) {
        promiseExecutor.execute(buildFulfillmentRunnable(() -> alternateSupplier.apply(thrownException)));
    }

}
