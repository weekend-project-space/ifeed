package org.bitmagic.ifeed.infrastructure.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
@Slf4j
public final class TaskUtils {

    private TaskUtils() {}

    /**
     * 带超时控制的批量任务执行
     * <p>
     * 功能：
     * 1. 提交任务并收集 Future
     * 2. 指定超时后：取消任务 + 清空队列 + 强制唤醒 latch
     * 3. 自动清理超时监控线程
     *
     * @param executor     线程池
     * @param tasks        任务列表（每个任务是一个 Runnable）
     * @param timeout      超时时间
     * @param unit         时间单位
     * @param latch        任务计数器（用于等待）
     * @param success      成功计数器
     * @param failed       失败计数器
     * @return 所有 Future 列表
     */
    public static List<Future<?>> executeWithTimeout(
            ExecutorService executor,
            List<Runnable> tasks,
            long timeout,
            TimeUnit unit,
            CountDownLatch latch,
            AtomicInteger success,
            AtomicInteger failed) {

        List<Future<?>> futures = new ArrayList<>(tasks.size());

        // 1. 提交所有任务
        for (Runnable task : tasks) {
            Future<?> future = executor.submit(() -> {
                try {
                    task.run();
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.error("Task execution failed", e);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // 2. 启动超时监控
        ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r, "timeout-watcher-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                }
        );

        watcher.schedule(() -> {
            log.warn("Task batch TIMEOUT after {} {}. Cancelling and clearing queue...", timeout, unit);

            // 取消所有任务
            futures.forEach(f -> f.cancel(true));

            // 清空队列
            BlockingQueue<Runnable> queue = ((ThreadPoolExecutor) executor).getQueue();
            int cleared = queue.size();
            queue.clear();
            log.warn("Cleared {} pending tasks from queue", cleared);

            // 强制唤醒 latch
            while (latch.getCount() > 0) {
                latch.countDown();
            }

            // 关闭 watcher 自身
            watcher.shutdownNow();
        }, timeout, unit);

        return futures;
    }
}