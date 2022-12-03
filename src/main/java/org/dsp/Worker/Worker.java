package org.dsp.Worker;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.Manager.Threads.Terminatable;
import org.dsp.Pair;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Worker {

    private static final Region region = Region.US_EAST_1;
    private static Boolean terminated = Boolean.FALSE;


    public static void main(String[] args) {
        System.out.println("[DEBUG]: Worker.main");
        String managerId = args[0];
        // Hello darling
        //setting workers sqs queues names
        String managerToWorkerName = managerId + "managerToWorker";
        String workerToManagerName = managerId + "workerToManager";

        //setting workers sqs queues
        SQSQueue managerToWorkerSQS = new SQSQueue(managerToWorkerName, region);
        SQSQueue workerToManager = new SQSQueue(workerToManagerName, region);


        while (!terminated) {
            SQSMessage sqsMessage = managerToWorkerSQS.receiveMsg();
            try{
                run(sqsMessage, workerToManager);
                managerToWorkerSQS.deleteMessage(sqsMessage);
            } catch (Exception e){
                System.out.println("[Error] unexpected error occurred: " + e.getMessage());
            }
        }
    }

    private static void terminate() {
        terminated = Boolean.TRUE;
    }

    private static void run(SQSMessage bucketName_imgLink, SQSQueue workerToManager) {

        String localBucket = bucketName_imgLink.getRequestId();
        String imageLink = bucketName_imgLink.getBody();

        //download image
        try {
            File imageFile = downloadImage(imageLink);
            if (imageFile == null) {
                workerToManager.sendMessage(new SQSMessage(imageLink + "\nshould not be happening, could not write to a file", localBucket));
                return;
            }
            //performOCR
            String OCROutput = doOCR(imageFile);
            if (OCROutput == null) {
                workerToManager.sendMessage(new SQSMessage(imageLink + "\nFailed to do OCR to the provided image", localBucket));
                boolean ignore = imageFile.delete();
                return;
            }
            //send to workerToManager
            workerToManager.sendMessage(new SQSMessage(imageLink + "\n" + OCROutput, localBucket));

            boolean ignore = imageFile.delete();
        } catch (IOException e) {
            workerToManager.sendMessage(new SQSMessage(imageLink + "\nFailed to download the provided link", localBucket));
        }

    }

    private static Pair<String, String> getLinkNameAndFormat(String linkName) {
        String[] split = linkName.split("/");
        String name = split[split.length - 1];
        return new Pair<>(name, name.split("\\.")[1]);//if the time allows to do format checking because the ocr supports certain formats
    }

    private static File downloadImage(String link) throws IOException {
        System.out.println("Downloading the image the image link is: " + link);
        URL url = new URL(link);
        System.out.println("did not crash from URL in line 89 " + url);
        BufferedImage img;
        try {
            img = ImageIO.read(url);
            Pair<String, String> nameFormat = getLinkNameAndFormat(link);
            File file = new File(nameFormat.getFirst());
            ImageIO.write(img, nameFormat.getSecond(), file);
            return file;
        } catch (Exception e) {
            return null;
        }

    }

    private static String doOCR(File img) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("src/main/resources/tessdata");
            return tesseract.doOCR(img);
        } catch (TesseractException e) {
            return null;
        }

    }
}
