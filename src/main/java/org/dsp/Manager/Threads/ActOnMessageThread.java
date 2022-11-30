package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.Manager.Tasks.DownloadAndDistributeTask;
import org.dsp.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ActOnMessageThread implements Runnable, Terminatable {
    private volatile boolean terminated = false;
    protected final ExecutorService threadPool;
    protected final Region region;
    protected final SQSQueue sqsQueue;
    protected final AtomicInteger numberOfMessagesInProcess;
    protected final ResultManager resultManager;

    protected ActOnMessageThread(Region region, ExecutorService threadPool, SQSQueue sqsQueue, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager) {
        this.threadPool = threadPool;
        this.region = region;
        this.sqsQueue = sqsQueue;
        this.numberOfMessagesInProcess = numberOfMessagesInProcess;
        this.resultManager = resultManager;
    }

    public abstract void run();

    @Override
    public void terminate() {
        this.terminated = true;
    }

    @Override
    public boolean terminated() {
        return terminated;
    }
}
