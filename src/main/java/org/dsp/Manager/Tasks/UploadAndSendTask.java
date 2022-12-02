package org.dsp.Manager.Tasks;

import org.dsp.AWS_SERVICES.S3_service.S3Instance;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.Manager.WorkerResult;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class UploadAndSendTask implements Runnable {
    private final S3Instance s3LocalBucket;
    private final List<WorkerResult> workerResults;
    private final SQSQueue managerToLocal;

    public UploadAndSendTask(Region region, String bucketNameOfLocalApp, List<WorkerResult> workerResults) {
        this.workerResults = workerResults;
        this.s3LocalBucket = new S3Instance(region, bucketNameOfLocalApp);
        this.managerToLocal = new SQSQueue(bucketNameOfLocalApp, region);
    }


    @Override
    public void run() {
        String htmlBody = createHtmlBody();

        File htmlBodyResultFile = new File("results_" + s3LocalBucket.getBucket() + ".txt");
        try (FileWriter fw = new FileWriter(htmlBodyResultFile)) {
            fw.write(htmlBody);

        } catch (IOException e) {
            throw new RuntimeException(e); //FIXME
        }

        String s3ResultsKey = "results";
        s3LocalBucket.uploadFile(s3ResultsKey, htmlBodyResultFile);
        System.out.println("[Debug] uploaded result file");
        boolean ignore = htmlBodyResultFile.delete();

        managerToLocal.sendMessage(new SQSMessage(s3ResultsKey, s3LocalBucket.getBucket()));

    }

    private String createHtmlBody() {
        StringBuilder body = new StringBuilder();
        for (WorkerResult result : this.workerResults) {
            body.append("<p>\n\t<img src=\"")
                    .append(result.getImgLink())
                    .append("\"><br>\n\t")
                    .append(result.getOcrResult())
                    .append("\n</p>\n");
        }
        return body.toString();
    }
}
