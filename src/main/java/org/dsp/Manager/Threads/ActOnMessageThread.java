package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ActOnMessageThread implements Runnable, Terminatable {
    private final AtomicBoolean systemTerminated;
    private final AtomicBoolean terminatedMessageAccrued; //occurred

    protected final ExecutorService threadPool;

    protected final Region region;
    protected final SQSQueue sqsQueue;
    protected final AtomicInteger numberOfMessagesInProcess;
    protected final ResultManager resultManager;

    protected ActOnMessageThread(Region region, ExecutorService threadPool, SQSQueue sqsQueue, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager, AtomicBoolean systemTerminated, AtomicBoolean terminatedMessageAccrued) {
        this.threadPool = threadPool;
        this.region = region;
        this.sqsQueue = sqsQueue;
        this.numberOfMessagesInProcess = numberOfMessagesInProcess;
        this.resultManager = resultManager;
        this.terminatedMessageAccrued = terminatedMessageAccrued;
        this.systemTerminated = systemTerminated;
    }

    public abstract void run();

    protected abstract void actOnMessage(SQSMessage message);

    public void terminateSystem(){
        this.systemTerminated.set(true);
    }

    public void notifyTerminationMessageOccurred(){
        this.terminatedMessageAccrued.set(true);
    }

    public boolean systemTerminated(){
        return systemTerminated.get();
    }

    public boolean receivedTerminationMessage(){
        return this.terminatedMessageAccrued.get();
    }

}
