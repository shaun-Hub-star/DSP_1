package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.Manager.Tasks.DownloadAndDistributeTask;
import org.dsp.Manager.Tasks.UploadAndSendTask;
import org.dsp.SQS_service.SQSQueue;
import org.dsp.Worker.WorkerResult;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersResultThread extends ActOnMessageThread {


    public WorkersResultThread(Region region, ExecutorService uploadAndSendThreadPool, SQSQueue workerToManager, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager) {
        super(region, uploadAndSendThreadPool, workerToManager, numberOfMessagesInProcess, resultManager);
    }

    @Override
    public void run() {
        while (!terminated()) {
            SQSMessage resultMessage = sqsQueue.receiveMsg(); //sqsQueue = workerToManager
            sqsQueue.deleteMessage(resultMessage);
            numberOfMessagesInProcess.decrementAndGet();
            String bucketNameOfLocalApp = resultMessage.getRequestId();
            resultManager.addResult(bucketNameOfLocalApp, new WorkerResult(resultMessage));
            if (resultManager.didFinishRequest(bucketNameOfLocalApp)) {
                resultManager.deleteLocalBucketEntry(bucketNameOfLocalApp);
                Runnable uploadAndSend = new UploadAndSendTask(region, bucketNameOfLocalApp, resultManager.getWorkerResults(bucketNameOfLocalApp));
                threadPool.execute(uploadAndSend); //threadPool = uploadAndSendThreadPool
            }
        }
    }

}
