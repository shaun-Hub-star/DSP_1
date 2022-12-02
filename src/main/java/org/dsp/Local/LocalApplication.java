package org.dsp.Local;

import org.dsp.AWS_SERVICES.EC2_service.EC2_Service;
import org.dsp.AWS_SERVICES.S3_service.S3Instance;
import org.dsp.AWS_SERVICES.SQS_service.SQSQueue;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.io.*;

public class LocalApplication {


    private final String suffix = "0525381648dqw4w9wgxcq";
    private final String managerJarKey = "ManagerJar";
    private final String managerTag = "ManagerTag";
    private final String managerExistsBucketName = "managerexistsbucket"+suffix;
    private final String managerExistsFileKey = "managerExistFile";
    private final String jarsBucket = "jars"+suffix;
    private final Region region = Region.US_EAST_1;
    private final S3Instance s3Instance = new S3Instance(region);
    private final S3Instance s3ManagerBucket = new S3Instance(region, managerExistsBucketName);
    private final S3Instance s3Jars = new S3Instance(region, jarsBucket);
    private String managerId;
    private final String inputFilePath;
    private final String inputFileName;
    private final String outputFilePath;
    private final EC2_Service ec2_service = new EC2_Service(region);
    private SQSQueue localToManagerSQS;
    private final SQSQueue resultFromManager = new SQSQueue(s3Instance.getBucket(), this.region);
    private final int ratioOfComputers;

    private final String terminationCode = "terminate";

    public LocalApplication(String inputFilePath, String outputFilePath, int ratioOfComputers) {
        this.inputFilePath = inputFilePath;
        this.inputFileName = getNameOfTheFile(inputFilePath);
        this.outputFilePath = outputFilePath;
        this.ratioOfComputers = ratioOfComputers;

    }

    private void startManager() {
        System.out.println("[DEBUG] Create manager if not exist");

        if (createManagerBucketIfNotExists()) {
            //the manager does not exist
            String managerScript =
                    "#!/bin/bash\n" +
                            "sudo yum update -y\n" +
                            "sudo amazon-linux-extras install java-openjdk11 -y\n" +
                            "sudo yum install java-devel -y\n" +
                            "mkdir ManagerFiles\n" +
                            "cd ManagerFiles\n" +
                            "aws s3 cp s3://" + s3Jars.getBucket() + "/" + this.managerJarKey + " " + this.managerJarKey + ".jar\n" +
                            "java -jar " + this.managerJarKey + ".jar " +
                            /*args:*/suffix + " " + managerExistsBucketName + " " + managerExistsFileKey + " " + ratioOfComputers + " " + terminationCode;

            this.managerId = ec2_service.createEc2Instance(managerTag, managerScript, InstanceType.M4_LARGE);
            createAndUploadManagerFile();
            System.out.println("[DEBUG] Manager created and started!.");

        } else {
            System.out.println("[DEBUG] Manager already exists. taking his id from s3");
            setManagerIdFromS3();
            System.out.println("[DEBUG] took manager id from s3: " + this.managerId);



        }
        //set sqs queue from local to manager in the name of the manager ID
        this.localToManagerSQS = new SQSQueue(managerId, this.region);
    }

    private void uploadImageURLsToS3() {
        s3Instance.uploadFile(this.inputFileName, this.inputFilePath);
    }

    private String getNameOfTheFile(String inputFilePath) {
        String[] split = inputFilePath.split("/");
        return split[split.length - 1];
    }


    /*
     * creates a dummy bucket if fails in case of existing return false
     * return true iff the bucket does not exist.
     *
     * */
    private boolean createManagerBucketIfNotExists() {
        try {
            return s3ManagerBucket.createBucket();

        } catch (Exception e) {
            System.out.println("Error: could not notify manager got created : " + e.getMessage());
            throw e;
        }
    }

    private void createAndUploadManagerFile() {
        String txtFileName = this.managerExistsFileKey + ".txt";
        try (FileWriter fw = new FileWriter(txtFileName)) {
            fw.write(this.managerId);

        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(txtFileName);
        s3ManagerBucket.uploadFile(this.managerExistsFileKey, file);
        if (!file.delete()) {
            System.out.println("[DEBUG]: Fail: failed to delete managerExistFile locally");
        }
    }

    private void setManagerIdFromS3() {
        this.managerId = s3ManagerBucket.downloadFileContentFromS3(this.managerExistsFileKey, "txt", this.outputFilePath);
    }


    public void terminateManager() {
        //notify to other locals that the manager does not exist
        deleteManagerExistsBucket();

        //send terminate message to the manager:

        sendTerminationMessageToManager();
        //TODO: send a message to the manager to terminate all the workers.
        //TODO: only terminate manager after cleaning his whole SQS queue.
        //TODO: manager: close managerBucket in order of new request create a different manager.
        //TODO: close the SQS localToManager(manager_id)
        //TODO: create a suicide message from the manager after sending all the results. in case of in case of self suicide does not work
        this.ec2_service.terminateEC2(this.managerId); //TODO???: move this line to the manager.
    }

    private void sendTerminationMessageToManager() {
        localToManagerSQS.sendMessage(new SQSMessage(terminationCode, s3Instance.getBucket()/*client id*/));
    }

    private void deleteManagerExistsBucket() {
        s3ManagerBucket.deleteBucketAndContent();
    }

    private void sendTheLocationOfInputFileOnS3ForManager() {
        localToManagerSQS.sendMessage(new SQSMessage(inputFileName, s3Instance.getBucket()));
    }

    private void waitForResultFromManager() {
        SQSMessage message = resultFromManager.receiveMsg(); //this function ensures that we get a message from the manager.
        System.out.println("[DEBUG] receive results from manager: " + message.getBody());
    }

    private void deleteResultSQS() {
        resultFromManager.deleteQueue();
    }

    private String getHtmlBodyFromS3() {
        return s3Instance.downloadFileContentFromS3("result", "txt", outputFilePath);
    }


    private void createHTML(String htmlBody) {
        String html = "<html>\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">\n" +
                "<title>OCR</title>\n" +
                "</head>\n" + htmlBody + "</html>";

        try (FileWriter fw = new FileWriter(outputFilePath + "\\results.html")) {
            fw.write(html);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteLocalAppBucket() {
        this.s3Instance.deleteBucketAndContent();
    }

    /**
     * Runs this operation.
     */



    public void run() {
        try{
            uploadImageURLsToS3();
            startManager();

            sendTheLocationOfInputFileOnS3ForManager();
            /*
            waitForResultFromManager();
            deleteResultSQS();
            //String htmlBody = getHtmlBodyFromS3();
            deleteLocalAppBucket();
            //createHTML(htmlBody);

             */

        } catch (Exception e){
            /*
            deleteResultSQS();
            deleteLocalAppBucket();

             */
            e.printStackTrace();
        }
        finally {
            /*
            s3ManagerBucket.deleteBucketAndContent();
            ec2_service.terminateEC2(this.managerId);

             */
        }

    }
}
