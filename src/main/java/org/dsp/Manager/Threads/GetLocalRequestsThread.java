package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.Manager.Tasks.DownloadAndDistributeTask;
import org.dsp.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;


public class GetLocalRequestsThread extends ActOnMessageThread {

    private final SQSQueue localToManager;

    public GetLocalRequestsThread(String managerId, Region region, ExecutorService downloadAndDistributeThreadPool, SQSQueue managerToWorker, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager) {
        super(region, downloadAndDistributeThreadPool, managerToWorker, numberOfMessagesInProcess, resultManager);
        this.localToManager = new SQSQueue(managerId, region);

    }

    @Override
    public void run() {
        while (!terminated()) {
            SQSMessage requestMessage = localToManager.receiveMsg();
            localToManager.deleteMessage(requestMessage);
            String bucketNameOfLocalApp = requestMessage.getRequestId();
            String imgLinksFileKey = requestMessage.getBody();

            Runnable downloadAndDistribute = new DownloadAndDistributeTask(region, bucketNameOfLocalApp, imgLinksFileKey, sqsQueue, numberOfMessagesInProcess, resultManager);
            threadPool.execute(downloadAndDistribute);

        }
    }


}
