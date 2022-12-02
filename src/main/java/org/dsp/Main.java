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
        s3Jars.deleteBucketAndContent();
        s3Jars.createBucket();
        String managerJarKey = "ManagerJar";
        String managerJarPath = "/home/spl-labs/Desktop/DSP_213/out/artifacts/ManagerEXE_jar/DSP_213.jar";
        s3Jars.uploadFile(managerJarKey, managerJarPath);
        String workerJarKey = "WorkerJar";
        String workerJarPath = "/home/spl-labs/Desktop/DSP_213/out/artifacts/WorkerEXE_jar/DSP_213.jar";
        s3Jars.uploadFile(workerJarKey, workerJarPath);


    }

    





}
