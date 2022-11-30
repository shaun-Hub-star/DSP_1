package org.dsp.Manager.Tasks;

import org.dsp.Manager.ResultManager;
import org.dsp.S3_service.S3Instance;
import org.dsp.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadAndDistributeTask implements Runnable{

    private final S3Instance s3LocalBucket;
    private final String imgLinksFileKey;
    private final SQSQueue managerToWorker;
    private final AtomicInteger numberOfMessagesInProcess;
    private final ResultManager resultManager;


    public DownloadAndDistributeTask(Region region, String bucketNameOfLocalApp, String imgLinksFileKey, SQSQueue managerToWorker, AtomicInteger numberOfMessagesInProcess, ResultManager resultManager) {
        this.s3LocalBucket = new S3Instance(region, bucketNameOfLocalApp);
        this.imgLinksFileKey = imgLinksFileKey;
        this.managerToWorker = managerToWorker;
        this.numberOfMessagesInProcess = numberOfMessagesInProcess;
        this.resultManager = resultManager;
    }

    @Override
    public void run() {
        List<SQSMessage> links = new LinkedList<>();
        File imgLinksFile = s3LocalBucket.downloadFile(imgLinksFileKey, "");

        int numberOfLinks = 0;
        try (FileReader fr = new FileReader(imgLinksFile)) {
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String imgLink;
            while ((imgLink = br.readLine()) != null) {
                links.add(new SQSMessage(this.s3LocalBucket.getBucket(), imgLink));
                numberOfLinks++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean ignore = imgLinksFile.delete();
        this.numberOfMessagesInProcess.addAndGet(numberOfLinks);
        this.numberOfMessagesInProcess.notify(); //FIXME
        this.resultManager.addLocalBucketKey(s3LocalBucket.getBucket(), numberOfLinks);
        managerToWorker.sendMultiMsg(links);

    }
}
