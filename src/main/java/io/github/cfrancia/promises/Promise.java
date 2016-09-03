package io.github.cfrancia.promises;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by cfrancia on 14/08/16.
 */
public interface Promise<T> {

    Executor DEFAULT_EXECUTOR = new ThreadPoolExecutor(
            0, Runtime.getRuntime().availableProcessors(),
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(Integer.MAX_VALUE)
    );

    static <T> Promise<T> promise(Supplier<T> promiseSupplier, Executor promiseExecutor) {
        return new StandalonePromise<>(promiseSupplier, promiseExecutor);
    }

    static <T> Promise<T> promise(Supplier<T> promiseSupplier) {
        return promise(promiseSupplier, DEFAULT_EXECUTOR);
    }

    T resolve(int timeout, TimeUnit timeUnit);

    Optional<T> maybeResolve();

    void consume(Consumer<PromiseResult<T>> promiseConsumer);

    <R> Promise<R> then(Function<T, R> mutatorFunction);

    Promise<T> or(Function<Exception, T> alternateSupplier);

    default Promise<T> or(Supplier<T> alternateSupplier) {
        return or((ignored) -> alternateSupplier.get());
    }

    <R> Promise<R> combine(Supplier<T> additionalSupplier, BiFunction<T, T, R> combiner);

    static <T, R> Promise<R> combine(Promise<T> firstPromise, Promise<T> secondPromise, BiFunction<T, T, R> combiner,
                                     Executor promiseExecutor) {
        return new JoiningPromise<>(promiseExecutor, combiner).associate(firstPromise, secondPromise);
    }

    static <T, R> Promise<R> combine(Promise<T> firstPromise, Promise<T> secondPromise, BiFunction<T, T, R> combiner) {
        return combine(firstPromise, secondPromise, combiner, DEFAULT_EXECUTOR);
    }

}
