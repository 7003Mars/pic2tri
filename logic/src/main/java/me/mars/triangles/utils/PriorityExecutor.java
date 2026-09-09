package me.mars.triangles.utils;

import arc.util.Log;

import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityExecutor {
    private static AtomicInteger threadCount = new AtomicInteger();

    public static ThreadPoolExecutor getExecutor(int threads, String name) {
        return new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(11, new PriorityFutureComparator()),
                r -> newThread(r, name + "-" + threadCount.getAndIncrement())) {
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityFuture<>(newTaskFor, ((PriorityCallable<?>) callable).priority);
            }
        };
    }

    private static Thread newThread(Runnable r, String name) {
        Thread thread = new Thread(r, name);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> Log.err(e));
        return thread;
    }

    public static <T> PriorityCallable<T> priorityCallable(int priority, Callable<T> callable) {
        return new PriorityCallable<T>(priority) {
            @Override
            public T call() throws Exception {
                return callable.call();
            }
        };
    }

    public static abstract class PriorityCallable<T> implements Callable<T> {
        int priority;

        public PriorityCallable(int priority) {
            this.priority = priority;
        }
    }

    // Source - https://stackoverflow.com/a/16577568
    // Posted by Stanislav Vitvitskyy
    // Retrieved 2026-01-08, License - CC BY-SA 3.0
    // Modified to keep execution order
    private static class PriorityFuture<T> implements RunnableFuture<T> {
        private static AtomicInteger counter = new AtomicInteger();
        private int id = counter.getAndIncrement();

        private RunnableFuture<T> src;
        private int priority;

        public PriorityFuture(RunnableFuture<T> other, int priority) {
            this.src = other;
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        public int getId() {
            return id;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return src.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
            return src.isCancelled();
        }

        public boolean isDone() {
            return src.isDone();
        }

        public T get() throws InterruptedException, ExecutionException {
            return src.get();
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return src.get();
        }

        public void run() {
            src.run();
        }
    }

    // Source - https://stackoverflow.com/a/16577568
    // Posted by Stanislav Vitvitskyy
    // Retrieved 2026-01-08, License - CC BY-SA 3.0
    private static class PriorityFutureComparator implements Comparator<Runnable> {
        public int compare(Runnable o1, Runnable o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;
            else {
                int p1 = ((PriorityFuture<?>) o1).getPriority();
                int p2 = ((PriorityFuture<?>) o2).getPriority();
                int i1 = ((PriorityFuture<?>) o1).getId();
                int i2 = ((PriorityFuture<?>) o2).getId();
                return p1 == p2 ? Integer.compare(i1, i2) : Integer.compare(p1, p2);
            }
        }
    }

}
