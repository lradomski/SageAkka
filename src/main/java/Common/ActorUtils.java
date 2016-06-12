package common;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class ActorUtils {

    public static class TestManualExecutor implements ExecutorService
    {
        boolean isShutdown = false;

        final LinkedList<Runnable> runnables = new LinkedList<>();

        @Override
        public void shutdown() {
            isShutdown = true;

        }

        @Override
        public List<Runnable> shutdownNow() {
            return runnables;
        }

        @Override
        public boolean isShutdown() {
            return isShutdown;
        }

        @Override
        public boolean isTerminated() {
            return isShutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return isShutdown;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return null;
        }

        @Override
        public Future<?> submit(Runnable task) {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        @Override
        public void execute(Runnable command) {

        }
    }

    static private final Pattern actorNameIllegalChars = Pattern.compile("[^a-zA-Z0-9-_\\.\\*\\$\\+:@&=,!~']");

    public static String makeActorName(String name)
    {
        return actorNameIllegalChars.matcher(name).replaceAll("");
    }
}
