package io.github.cfrancia.promises;

import io.github.cfrancia.promises.exception.FailedPromiseException;
import io.github.cfrancia.promises.exception.UnfulfilledPromiseException;
import io.github.cfrancia.promises.util.TestExecutors;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by cfrancia on 14/08/16.
 */
public class PromiseTest {

    @Test
    public void shouldBeAbleToResolvePromise() {
        assertThat(Promise.promise(() -> "Hello!", TestExecutors.sameThreadExecutor()), resolvesTo("Hello!"));
    }

    @Test
    public void shouldThrowUnfulfilledPromiseExceptionIfResolveTimesOut() {
        Promise<String> promise = Promise.promise(() -> "Hello!", TestExecutors.steppingExecutor());

        try {
            promise.resolve(100, TimeUnit.MILLISECONDS);
            fail("Should have thrown an UnfulfilledPromiseException");
        } catch (UnfulfilledPromiseException e) {
            // expected
        }
    }

    @Test
    public void shouldRethrowExceptionsThrownBySupplierWhenResolvingPromise() {
        try {
            Promise.promise(buildThrowingSupplier(), TestExecutors.sameThreadExecutor()).resolve(1, TimeUnit.SECONDS);

            fail("Should have thrown FailedPromiseException");
        } catch (FailedPromiseException e) {
            assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    public void shouldReturnEmptyOptionalForMaybeResolveIfPromiseNotYetResolved() {
        Promise<String> promise = Promise.promise(() -> "Hello!", TestExecutors.steppingExecutor());
        assertThat(promise.maybeResolve(), is(emptyOptional()));
    }

    @Test
    public void shouldReturnPresentOptionalForMaybeResolveIfPromiseIsResolved() {
        Promise<String> promise = Promise.promise(() -> "Hello!", TestExecutors.sameThreadExecutor());
        assertThat(promise.maybeResolve(), is(optionalOf("Hello!")));
    }

    @Test
    public void shouldRethrowExceptionsThrownBySupplierWhenMaybeResolvingPromise() {
        try {
            Promise.promise(buildThrowingSupplier(), TestExecutors.sameThreadExecutor()).maybeResolve();

            fail("Should have thrown FailedPromiseException");
        } catch (FailedPromiseException e) {
            assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    public void shouldCallConsumeWithOkResultOncePromiseIsFulfilled() {
        TestExecutors.SteppingExecutor steppingExecutor = TestExecutors.steppingExecutor();
        StoringConsumer<PromiseResult<String>> storingConsumer = new StoringConsumer<>();

        Promise.promise(() -> "Hello", steppingExecutor)
                .consume(storingConsumer);

        assertThat(storingConsumer.getValue(), is(nullValue()));

        steppingExecutor.step();

        assertThat(storingConsumer.getValue(), Is.is(okResultOf("Hello")));
    }

    @Test
    public void shouldCallConsumeWithErrorResultOncePromiseHasThrown() {
        TestExecutors.SteppingExecutor steppingExecutor = TestExecutors.steppingExecutor();
        StoringConsumer<PromiseResult<String>> storingConsumer = new StoringConsumer<>();

        Promise.promise(buildThrowingSupplier(), steppingExecutor)
                .consume(storingConsumer);

        assertThat(storingConsumer.getValue(), is(nullValue()));
        steppingExecutor.step();

        assertThat(storingConsumer.getValue(), Is.is(errorResultOf(IllegalArgumentException.class)));
    }

    @Test
    public void shouldBeAbleToChainPromises() {
        Promise<String> promise = Promise.promise(() -> "Hello!", TestExecutors.sameThreadExecutor())
                .then(String::toUpperCase);

        assertThat(promise, resolvesTo("HELLO!"));
    }


    @Test
    public void shouldNotBeAbleToChainSamePromiseMultipleTimes() {
        Promise<String> originalPromise = Promise.promise(() -> "Hello!", TestExecutors.sameThreadExecutor());
        originalPromise.then(String::toUpperCase);

        try {
            originalPromise.then(String::toLowerCase);
            fail("Should have thrown an IllegalStateException here");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void shouldBeAbleToProvideAlternateSupplier() {
        Promise<String> promise = Promise.promise(buildThrowingSupplier(), TestExecutors.sameThreadExecutor())
                .or((ignored) -> "Hi!");

        assertThat(promise, resolvesTo("Hi!"));
    }

    @Test
    public void shouldPassthroughExceptionWhenFailingOverToAlternateSupplier() {
        StoringConsumer<Exception> exceptionStore = new StoringConsumer<>();

        Promise.promise(buildThrowingSupplier(), TestExecutors.sameThreadExecutor())
                .or((exception) -> {
                    exceptionStore.accept(exception);
                    return "Hi!";
                });

        assertThat(exceptionStore.getValue(), is(instanceOf(IllegalArgumentException.class)));
    }


    @Test
    public void shouldBeAbleToCombineSuppliers() {
        Promise<String> promise = Promise.promise(() -> "Hello", TestExecutors.sameThreadExecutor())
                .combine(() -> "World", (first, second) -> first + " " + second);

        assertThat(promise, resolvesTo("Hello World"));
    }

    @Test
    public void shouldBeAbleToCombineTwoSeparatePromises() {
        Promise<String> hello = Promise.promise(() -> "Hello")
                .then(String::toUpperCase);

        Promise<String> world = Promise.promise(() -> "World")
                .then(String::toUpperCase);

        Promise<String> combinedPromise = Promise.combine(hello, world, (left, right) -> left + " " + right);
        assertThat(combinedPromise,  CoreMatchers.anyOf(resolvesTo("HELLO WORLD"), resolvesTo("WORLD HELLO")));
    }

    @Test
    public void shouldBeAbleToUseActualThreads() {
        assertThat(Promise.promise(() -> "Hello!", newSingleThreadExecutor()), resolvesTo("Hello!"));

        Promise<String> chainedPromise = Promise.promise(() -> "Hello!", newSingleThreadExecutor())
                .then(String::toUpperCase);
        assertThat(chainedPromise, resolvesTo("HELLO!"));
    }

    private Supplier<String> buildThrowingSupplier() {
        return () -> {
            throw new IllegalArgumentException();
        };
    }

    private static <T> TypeSafeDiagnosingMatcher<Promise<T>> resolvesTo(T promisedValue) {
        return new TypeSafeDiagnosingMatcher<Promise<T>>() {
            @Override
            protected boolean matchesSafely(Promise<T> item, Description mismatchDescription) {
                T resolvedValue = item.resolve(1, TimeUnit.SECONDS);

                boolean matches = is(promisedValue).matches(resolvedValue);

                if (!matches) {
                    mismatchDescription.appendText(format("Promise resolved to [%s]", resolvedValue));
                }

                return matches;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("Resolved promise of [%s]", promisedValue));
            }
        };
    }

    private static TypeSafeDiagnosingMatcher<Optional<?>> emptyOptional() {
        //noinspection OptionalUsedAsFieldOrParameterType
        return new TypeSafeDiagnosingMatcher<Optional<?>>() {
            @Override
            protected boolean matchesSafely(Optional<?> item, Description mismatchDescription) {
                boolean matches = !item.isPresent();

                if (!matches) {
                    //noinspection OptionalGetWithoutIsPresent
                    mismatchDescription.appendValue(item);
                }

                return matches;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Optional.empty");
            }
        };
    }

    private static <T> TypeSafeDiagnosingMatcher<Optional<T>> optionalOf(T value) {
        //noinspection OptionalUsedAsFieldOrParameterType
        return new TypeSafeDiagnosingMatcher<Optional<T>>() {
            @Override
            protected boolean matchesSafely(Optional<T> item, Description mismatchDescription) {
                boolean matches = item.isPresent() && is(value).matches(item.get());

                if (!matches) {
                    mismatchDescription.appendValue(item);
                }

                return matches;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("Optional[%s]", value));
            }
        };
    }

    private static <T> TypeSafeDiagnosingMatcher<PromiseResult<T>> okResultOf(T value) {
        //noinspection OptionalUsedAsFieldOrParameterType
        return new TypeSafeDiagnosingMatcher<PromiseResult<T>>() {
            @Override
            protected boolean matchesSafely(PromiseResult<T> item, Description mismatchDescription) {
                boolean matches = item.isOk() && is(value).matches(item.getValue());

                if (!matches) {
                    mismatchDescription.appendValue(item);
                }

                return matches;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("PromiseResult[OK]: %s", value));
            }
        };
    }

    private static <T> TypeSafeDiagnosingMatcher<PromiseResult<T>> errorResultOf(Class<?> type) {
        //noinspection OptionalUsedAsFieldOrParameterType
        return new TypeSafeDiagnosingMatcher<PromiseResult<T>>() {
            @Override
            protected boolean matchesSafely(PromiseResult<T> item, Description mismatchDescription) {
                boolean matches = item.isError() && instanceOf(type).matches(item.getException());

                if (!matches) {
                    mismatchDescription.appendValue(item);
                }

                return matches;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("PromiseResult[ERROR]: <typeof> %s", type));
            }
        };
    }

    private static class StoringConsumer<T> implements Consumer<T> {

        private T value = null;

        @Override
        public void accept(T t) {
            value = t;
        }

        public T getValue() {
            return value;
        }
    }

}