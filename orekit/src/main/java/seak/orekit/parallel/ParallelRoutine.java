/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.parallel;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import seak.orekit.coverage.analysis.FastCoverageAnalysis;

/**
 * Integrated method to use a central thread pool
 *
 * @author nozomihitomi
 */
public class ParallelRoutine {

    /**
     * Create a singleton instance
     */
    private static ParallelRoutine instance;

    private static ExecutorService pool;

    private static CompletionService<SubRoutine> ecs;

    /**
     * The number of threads in the thread pool
     */
    private static int numThreads;

    /**
     * Creates a singleton instance
     *
     * @param numThreads
     */
    private ParallelRoutine(int numThreads) {
        ParallelRoutine.pool = Executors.newFixedThreadPool(numThreads);
        ParallelRoutine.ecs = new ExecutorCompletionService(pool);
        ParallelRoutine.numThreads = numThreads;
        instance = this;
    }

    /**
     * Gets an instance of the ParallelRoutine. A new instance is created if the
     * current thread pool has been shutdown or if the number of threads does
     * not match the current number. When creating a new instance, the current
     * pool will be shutdown and will not wait for any running processes to
     * finish
     *
     * @param numThreads The number of threads to create in the pool
     * @return an instance of ParallelRoutine
     */
    public static ParallelRoutine getInstance(int numThreads) {
        if (instance == null){
            return new ParallelRoutine(numThreads);
        }
        if (ParallelRoutine.pool.isShutdown() || ParallelRoutine.pool.isTerminated()) {
            return new ParallelRoutine(numThreads);
        }
        if (numThreads != ParallelRoutine.numThreads) {
            ParallelRoutine.shutDown();
            return new ParallelRoutine(numThreads);
        }
        return instance;
    }

    /**
     * Submits a subroutine to the thread pool
     *
     * @param subroutine routine to run or call
     * @return a Future representing pending completion of the task
     */
    public static Future<SubRoutine> submit(SubRoutine subroutine) {
        return ParallelRoutine.ecs.submit(subroutine);
    }

    /**
     * Retrieves and removes the Future representing the next completed task,
     * waiting if none are yet present.
     *
     * @return the Future representing the next completed task
     * @throws java.lang.InterruptedException
     */
    public static Future<SubRoutine> take() throws InterruptedException {
        return ParallelRoutine.ecs.take();
    }

    /**
     * Shuts down the thread pool and will not wait for any running processes to
     * finish
     */
    public static void shutDown() {
        ParallelRoutine.pool.shutdown();
    }
}
