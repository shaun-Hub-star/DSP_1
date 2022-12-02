package org.dsp.Manager.Threads;

import org.dsp.AWS_SERVICES.EC2_service.EC2_Service;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.Manager.ResultManager;
import org.dsp.AWS_SERVICES.S3_service.S3Instance;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainThread {

    private static final ResultManager resultManager = new ResultManager();
    private static final int maxNumberOfEc2Instances = 17;
    private static int numberOfWorkers = 0;
    private static final AtomicInteger numberOfMessagesInProcess = new AtomicInteger(0);
    private static final Region region = Region.US_EAST_1;
    private static final AtomicBoolean systemTerminated = new AtomicBoolean(false);
    private static final AtomicBoolean terminationMessageOccurred = new AtomicBoolean(false);

    private static int numberLinksRequiredToNewInstance;
    private static final EC2_Service ec2 = new EC2_Service(region);
    private static String jarsBucket = "jars";
    private static S3Instance s3Jars;
    private static final String workerJarKey = "WorkerJar";
    private static final String workerTag = "WorkerTag";
    private static String managerId;
    private static final LinkedList<String> workersIds = new LinkedList<>();


    public static void main(String[] args) {// FIXME: 30/11/2022 maven

        /*
         * *** SETUP ***
         * */
        String suffix = args[0];//for uniqueness
        String managerExistsBucketName = args[1];//to download manager id
        String managerExistsFileKey = args[2];//same
        try {
            numberLinksRequiredToNewInstance = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR]: " + e.getMessage());
        }
        String terminationCode = args[4];

        //creating s3 instance for jars bucket
        jarsBucket += suffix;
        s3Jars = new S3Instance(region, jarsBucket);

        //creating s3 instance for managerExistsBucket
        S3Instance s3ManagerBucket = new S3Instance(region, managerExistsBucketName);
        //getting manager ID from s3
        managerId = s3ManagerBucket.downloadFileContentFromS3(managerExistsFileKey, "txt", ".");

        //creating thread pools for 'downloadAndDistribute' and for 'uploadAndSend' tasks
        ExecutorService downloadAndDistributeThreadPool = Executors.newFixedThreadPool(2);
        ExecutorService uploadAndSendThreadPool = Executors.newFixedThreadPool(2);

        //setting workers sqs queues names
        String managerToWorkerName = managerId + "managerToWorker";
        String workerToManagerName = managerId + "workerToManager";

        //creating workers sqs queues
        SQSQueue managerToWorker = new SQSQueue(managerToWorkerName, region);
        SQSQueue workerToManager = new SQSQueue(workerToManagerName, region);

        /*
         * *** END-SETUP ***
         * */

        /*
         * *** START-LOGIC ***
         * */

        //creating thread responsible for getting local requests
        Thread getLocalRequestsThread = new Thread(
                new GetLocalRequestsThread(managerId, region, downloadAndDistributeThreadPool, managerToWorker,
                        numberOfMessagesInProcess, resultManager, terminationCode, systemTerminated, terminationMessageOccurred));
        getLocalRequestsThread.start();

        //creating thread responsible for getting OCR results from the workers
        Thread workersResultThread = new Thread(
                new WorkersResultThread(region, uploadAndSendThreadPool, workerToManager,
                        numberOfMessagesInProcess, resultManager, systemTerminated, terminationMessageOccurred));
        workersResultThread.start();

        //handling workers
        while (!systemTerminated.get()) {
            synchronized (numberOfMessagesInProcess) {
                while (getNumberOfWorkersToAdd() == 0) {
                    try {
                        numberOfMessagesInProcess.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            int numOfWorkersToAdd = getNumberOfWorkersToAdd();
            System.out.println("[Debug] manager about to add " + numOfWorkersToAdd + " workers. " +
                    "there would be a total of " + (numberOfWorkers + numOfWorkersToAdd) + " workers");

            for (int i = 0; i < numOfWorkersToAdd; i++) {
                createWorker();
            }
        }

        //termination
        for (String workerId : workersIds) {
            ec2.terminateEC2(workerId);
            downloadAndDistributeThreadPool.shutdown();
            uploadAndSendThreadPool.shutdown();
        }

    }

    private static int getNumberOfWorkersToAdd() {
        //getting all the workers that crashed:
        List<String> notRunningWorkers = ec2.getAllNotRunningInstancesWithAGivenTag("name", workerTag);
        workersIds.removeAll(notRunningWorkers);

        //updating number of workers
        numberOfWorkers -= notRunningWorkers.size();

        int numberOfWorkersToAdd;
        if (numberOfWorkers == 0)
            numberOfWorkersToAdd = numberOfMessagesInProcess.get() / (numberLinksRequiredToNewInstance) + 1;
        else
            numberOfWorkersToAdd = numberOfMessagesInProcess.get() / (numberOfWorkers * numberLinksRequiredToNewInstance);

        return Math.min(maxNumberOfEc2Instances - numberOfWorkers, numberOfWorkersToAdd);

    }

    private static void createWorker() {
        String workerScript =
                "#!/bin/bash\n" +
                        "sudo yum update -y\n" +
                        "sudo amazon-linux-extras install java-openjdk11 -y\n" +
                        "sudo yum install java-devel -y\n" +
                        "mkdir WorkerFiles\n" +
                        "cd WorkerFiles\n" +
                        "aws s3 cp s3://" + s3Jars.getBucket() + "/" + workerJarKey + " " + workerJarKey + ".jar\n" +
                        "java -jar " + workerJarKey + ".jar " +
                        /*args:*/managerId;

        String workerId = ec2.createEc2Instance(workerTag, workerScript, InstanceType.T2_MICRO);
        workersIds.add(workerId);
        numberOfWorkers++;
    }
}
