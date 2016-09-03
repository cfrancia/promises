package io.github.cfrancia.promises;

import io.github.cfrancia.promises.exception.FailedPromiseException;
import io.github.cfrancia.promises.exception.UnfulfilledPromiseException;
import io.github.cfrancia.promises.util.Eventual;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by cfrancia on 14/08/16.
 */
abstract class AbstractPromise<T> implements Promise<T> {

    final Executor promiseExecutor;

    Eventual<T> eventualValue = new Eventual<>();
    Exception caughtException = null;

    private PromiseStep<T> nextStep = null;

    AbstractPromise(Executor promiseExecutor) {
        this.promiseExecutor = promiseExecutor;
    }

    @Override
    public T resolve(int timeout, TimeUnit timeUnit) {
        assertNoCaughtException();

        try {
            T resolvedValue = eventualValue.view(timeout, timeUnit);
            if (resolvedValue == null) {
                throw new UnfulfilledPromiseException();
            }

            return resolvedValue;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnfulfilledPromiseException(e);
        }
    }

    @Override
    public Optional<T> maybeResolve() {
        assertNoCaughtException();
        return Optional.ofNullable(eventualValue.view());
    }

    @Override
    public void consume(Consumer<PromiseResult<T>> promiseConsumer) {
        assertNoCurrentNextStep();
        setAsNextStepAndNotifyIfRequired(new ForwardingStep<>(promiseConsumer));
    }

    @Override
    public <R> Promise<R> then(Function<T, R> mutatorFunction) {
        assertNoCurrentNextStep();

        MutatingPromise<T, R> mutatingPromise = new MutatingPromise<>(promiseExecutor, mutatorFunction);
        setAsNextStepAndNotifyIfRequired(mutatingPromise);

        return mutatingPromise;
    }

    @Override
    public Promise<T> or(Function<Exception, T> alternateSupplier) {
        assertNoCurrentNextStep();

        ShortcircuitingPromise<T> shortcircuitingPromise = new ShortcircuitingPromise<>(promiseExecutor, alternateSupplier);
        setAsNextStepAndNotifyIfRequired(shortcircuitingPromise);

        return shortcircuitingPromise;
    }

    @Override
    public <R> Promise<R> combine(Supplier<T> additionalSupplier, BiFunction<T, T, R> combiner) {
        assertNoCurrentNextStep();

        JoiningPromise<T, R> joiningPromise = new JoiningPromise<>(promiseExecutor, combiner);
        setAsNextStepAndNotifyIfRequired(joiningPromise);

        AbstractPromise<T> concurrentPromise = new StandalonePromise<>(additionalSupplier, promiseExecutor);
        concurrentPromise.setAsNextStepAndNotifyIfRequired(joiningPromise);

        return joiningPromise;
    }

    Runnable buildFulfillmentRunnable(Supplier<T> valueSupplier) {
        return () -> {
            try {
                T suppliedValue = valueSupplier.get();

                eventualValue.place(suppliedValue);
                notifyNextStepIfPresentOfSuccess(suppliedValue);
            } catch (Exception e) {
                caughtException = e;
                notifyNextStepIfPresentOfFailure(e);
            }
        };
    }

    void notifyNextStepIfPresentOfSuccess(T suppliedValue) {
        if (nextStep != null) {
            nextStep.accept(suppliedValue);
        }
    }

    void notifyNextStepIfPresentOfFailure(Exception thrownException) {
        if (nextStep != null) {
            nextStep.failed(thrownException);
        }
    }

    private void setAsNextStepAndNotifyIfRequired(PromiseStep<T> step) {
        nextStep = step;

        notifyForEarlySuccess();
        notifyForEarlyFailure();
    }

    private void notifyForEarlySuccess() {
        T currentResolvedValue = eventualValue.view();
        if (currentResolvedValue != null) {
            nextStep.accept(currentResolvedValue);
        }
    }

    private void notifyForEarlyFailure() {
        if (caughtException != null) {
            nextStep.failed(caughtException);
        }
    }

    private void assertNoCurrentNextStep() {
        if (nextStep != null) {
            throw new IllegalStateException("Promise already has already been chained");
        }
    }

    private void assertNoCaughtException() {
        if (caughtException != null) {
            throw new FailedPromiseException(caughtException);
        }
    }

    private static class ForwardingStep<T> implements PromiseStep<T> {

        private final Consumer<PromiseResult<T>> forwardedConsumer;

        private ForwardingStep(Consumer<PromiseResult<T>> forwardedConsumer) {
            this.forwardedConsumer = forwardedConsumer;
        }

        @Override
        public void accept(T stepValue) {
            forwardedConsumer.accept(PromiseResult.ok(stepValue));
        }

        @Override
        public void failed(Exception thrownException) {
            forwardedConsumer.accept(PromiseResult.error(thrownException));
        }
    }

}
