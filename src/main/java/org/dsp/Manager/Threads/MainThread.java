package org.dsp.Manager.Threads;

import org.dsp.Manager.ResultManager;
import org.dsp.S3_service.S3Instance;
import org.dsp.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class MainThread {

    private static final ResultManager resultManager = new ResultManager();
    private static final int maxNumberOfEc2Instances = 17;
    private static final AtomicInteger numberOfWorkers = new AtomicInteger(0);
    private static final AtomicInteger numberOfMessagesInProcess = new AtomicInteger(0);
    private static final Region region = Region.US_EAST_1;


    public static void main(String[] args) {// FIXME: 30/11/2022 maven
        String suffix = args[0];
        String managerExistsBucketName = args[1];
        String managerExistsFileKey = args[2];
        int numberOfLinksRequiredToNewComputer = Integer.parseInt(args[3]);

        //creating s3 instance for managerExistsBucket
        S3Instance s3ManagerBucket = new S3Instance(region, managerExistsBucketName);

        //getting manager ID from s3
        String managerId = s3ManagerBucket.downloadFileContentFromS3(managerExistsFileKey, "txt", ".");

        //creating thread pools for 'downloadAndDistribute' and for 'uploadAndSend' tasks
        ExecutorService downloadAndDistributeThreadPool = Executors.newFixedThreadPool(2);
        ExecutorService uploadAndSendThreadPool = Executors.newFixedThreadPool(2);

        //setting workers sqs queues names
        String managerToWorkerName = managerId + "managerToWorker";
        String workerToManagerName = managerId + "workerToManager";

        //creating workers sqs queues
        SQSQueue managerToWorker = new SQSQueue(managerToWorkerName, region);
        SQSQueue workerToManager = new SQSQueue(workerToManagerName, region);

        //creating thread responsible for getting local requests
        Thread getLocalRequestsThread = new Thread(
                new GetLocalRequestsThread(managerId, region, downloadAndDistributeThreadPool, managerToWorker, numberOfMessagesInProcess, resultManager));
        getLocalRequestsThread.start();

        //creating thread responsible for getting OCR results from the workers
        Thread workersResultThread = new Thread(
                new WorkersResultThread(region, uploadAndSendThreadPool, workerToManager, numberOfMessagesInProcess, resultManager));
        workersResultThread.start();


    }
}
