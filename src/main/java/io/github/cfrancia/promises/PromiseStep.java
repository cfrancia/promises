package io.github.cfrancia.promises;

/**
 * Created by cfrancia on 14/08/16.
 */
interface PromiseStep<T> {

    void accept(T stepValue);

    void failed(Exception thrownException);

}
