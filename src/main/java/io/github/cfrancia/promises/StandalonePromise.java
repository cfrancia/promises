package io.github.cfrancia.promises;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Created by cfrancia on 14/08/16.
 */
class StandalonePromise<T> extends AbstractPromise<T> {

    StandalonePromise(Supplier<T> promiseSupplier, Executor promiseExecutor) {
        super(promiseExecutor);
        this.promiseExecutor.execute(buildFulfillmentRunnable(promiseSupplier));
    }

}
