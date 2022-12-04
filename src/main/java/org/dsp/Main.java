package org.dsp;

import org.dsp.AWS_SERVICES.S3_service.S3Instance;
import software.amazon.awssdk.regions.Region;
import java.io.*;


public class Main {
    public static void main(String[] args) throws IOException {

        String suffix = "0525381648dqw4w9wgxcq";
        String jarsBucket = "jars" + suffix;
        Region region = Region.US_EAST_1;
        S3Instance s3Jars = new S3Instance(region, jarsBucket);
        //s3Jars.deleteBucketAndContent();
        //uploadManagerJar(s3Jars);
        uploadWorkerJar(s3Jars);
        //uploadCredentials(s3Jars);
        uploadTessdata(s3Jars);


    }

    private static void uploadManagerJar(S3Instance s3Jars){
        s3Jars.createBucket();
        String managerJarKey = "ManagerJar";
        String managerJarPath = "out/artifacts/Manager_jar/DSP_213.jar";
        s3Jars.uploadFile(managerJarKey, managerJarPath);
    }

    private static void uploadWorkerJar(S3Instance s3Jars){
        s3Jars.createBucket();
        String workerJarKey = "WorkerJar";
        String workerJarPath = "out/artifacts/Worker_jar/DSP_213.jar";
        s3Jars.uploadFile(workerJarKey, workerJarPath);
    }

    private static void uploadCredentials(S3Instance s3Jars){
        s3Jars.createBucket();
        String credentialsKey = "credentials";
        String credentialsPath = "/home/spl-labs/Desktop/credentials";
        s3Jars.uploadFile(credentialsKey, credentialsPath);
    }

    private static void uploadTessdata(S3Instance s3Jars){
        s3Jars.createBucket();
        String tessdataKey = "eng.traineddata";
        String tessdataPath = "src/main/Resources/tessdata/eng.traineddata";
        s3Jars.uploadFile(tessdataKey, tessdataPath);
    }



    





}
