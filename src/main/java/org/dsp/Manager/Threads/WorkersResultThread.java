package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.Manager.Tasks.UploadAndSendTask;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.Manager.WorkerResult;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersResultThread extends ActOnMessageThread {


    public WorkersResultThread(Region region, ExecutorService uploadAndSendThreadPool, SQSQueue workerToManager, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager, AtomicBoolean systemTerminated, AtomicBoolean terminationMessageOccurred) {
        super(region, uploadAndSendThreadPool, workerToManager, numberOfMessagesInProcess, resultManager, systemTerminated, terminationMessageOccurred);
        System.out.println("WorkersResultThread.WorkersResultThread");

    }

    @Override
    public void run() {
        while (!systemTerminated()) {
            System.out.println("[Debug] Act on message workers result thread");
            actOnMessage(sqsQueue.receiveMsg()); //sqsQueue = workerToManager

        }
    }

    @Override
    protected void actOnMessage(SQSMessage resultMessage){
        sqsQueue.deleteMessage(resultMessage);
        numberOfMessagesInProcess.decrementAndGet();
        String bucketNameOfLocalApp = resultMessage.getRequestId();
        resultManager.addResult(bucketNameOfLocalApp, new WorkerResult(resultMessage));

        if (resultManager.didFinishRequest(bucketNameOfLocalApp)) {
            System.out.println("[Debug] finish working on local request " + bucketNameOfLocalApp);
            resultManager.deleteLocalBucketEntry(bucketNameOfLocalApp);
            Runnable uploadAndSend = new UploadAndSendTask(region, bucketNameOfLocalApp, resultManager.getWorkerResults(bucketNameOfLocalApp));
            threadPool.execute(uploadAndSend); //threadPool = uploadAndSendThreadPool
        }

        if(resultManager.isEmpty() && receivedTerminationMessage()) {//TODO: debug termination process
            System.out.println("[Debug] termination message occurred and and the resultManager is empty ");
            terminateSystem();
        }
    }

}
