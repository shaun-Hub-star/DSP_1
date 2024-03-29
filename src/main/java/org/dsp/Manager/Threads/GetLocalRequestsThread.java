package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.Manager.Tasks.DownloadAndDistributeTask;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class GetLocalRequestsThread extends ActOnMessageThread {

    private final SQSQueue localToManager;
    private final String terminationCode;
    private final String workerToManagerName;
    public GetLocalRequestsThread(String managerId, Region region, ExecutorService downloadAndDistributeThreadPool, SQSQueue managerToWorker, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager,
                                  String terminationCode, AtomicBoolean systemTerminated, AtomicBoolean terminationMessageOccurred) {
        super(region, downloadAndDistributeThreadPool, managerToWorker, numberOfMessagesInProcess, resultManager, systemTerminated, terminationMessageOccurred);
        this.localToManager = new SQSQueue(managerId, region);
        this.terminationCode = terminationCode;
        this.workerToManagerName = managerId + "workerToManager";

    }

    @Override
    public void run() {
        //the last message is always the termination message
        while (!receivedTerminationMessage()) {
            actOnMessage(localToManager.receiveMsg());
        }

        localToManager.deleteQueue();
        // in case all the workers already finished their job,
        // we want to notify the thread responsible to terminate the system,
        // that we received a termination message
        new SQSQueue(workerToManagerName, region).sendMessage(new SQSMessage("terminate", "terminate"));
    }

    @Override
    protected void actOnMessage(SQSMessage requestMessage){
        System.out.println("[Debug] received local request: LocalBucket: " + requestMessage.getRequestId() + " body: " + requestMessage.getBody());
        localToManager.deleteMessage(requestMessage);
        if(requestMessage.getBody().equals(terminationCode)){
            notifyTerminationMessageOccurred();
            return;
        }

        String bucketNameOfLocalApp = requestMessage.getRequestId();
        String imgLinksFileKey = requestMessage.getBody();

        Runnable downloadAndDistribute = new DownloadAndDistributeTask(region, bucketNameOfLocalApp, imgLinksFileKey, sqsQueue, numberOfMessagesInProcess, resultManager);
        threadPool.execute(downloadAndDistribute);
    }


}
