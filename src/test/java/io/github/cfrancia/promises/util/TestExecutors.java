package io.github.cfrancia.promises.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * Created by cfrancia on 14/08/16.
 */
public class TestExecutors {

    public static SameThreadExecutor sameThreadExecutor() {
        return new SameThreadExecutor();
    }

    public static SteppingExecutor steppingExecutor() {
        return new SteppingExecutor();
    }

    public static class SameThreadExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    public static class SteppingExecutor implements Executor {

        private final Queue<Runnable> runnableQueue = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            runnableQueue.add(command);
        }

        public void step() {
            Runnable runnable = runnableQueue.poll();

            if (runnable != null) {
                runnable.run();
            }
        }
    }

}
