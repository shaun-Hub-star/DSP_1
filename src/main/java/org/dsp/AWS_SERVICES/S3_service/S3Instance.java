package org.dsp.AWS_SERVICES.S3_service;

import org.apache.commons.io.FileUtils;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class S3Instance implements IS3_Methods {

    private final String bucket;
    private final S3Client s3;

    public S3Instance(Region region) {

        this.bucket = "bucket" + System.currentTimeMillis();
        this.s3 = S3Client.builder().
                region(region).credentialsProvider(ProfileCredentialsProvider.create()).build();
        createBucket();
    }

    /**
     * @param region     region
     * @param bucketName assumes that the bucket already exists
     */
    public S3Instance(Region region, String bucketName) {
        this.bucket = bucketName;
        this.s3 = S3Client.builder().
                credentialsProvider(ProfileCredentialsProvider.create()).
                region(region).build();
        /*this.s3 = S3Client.builder().
                region(region).credentialsProvider(ProfileCredentialsProvider.create()).build();
    */
    }

    /**
     * @param key  fileId
     * @param path file path that you want to upload
     */
    @Override
    public void uploadFile(String key, String path) {
        uploadFile(key, new File(path));
    }

    /**
     * @param key  file on name s3
     * @param file the file object that you want to upload
     */
    @Override
    public void uploadFile(String key, File file) {
        s3.putObject(PutObjectRequest.builder().bucket(this.bucket).key(key).build(), RequestBody.fromFile(file));
    }

    /**
     * @param key fileId
     */
    @Override
    public File downloadFile(String key, String downloadTo) {
        File urlFile = new File(downloadTo);
        ResponseInputStream<GetObjectResponse> responseInputStream = s3.getObject(GetObjectRequest.builder().bucket(this.bucket).key(key).build());
        try {
            FileUtils.copyInputStreamToFile(responseInputStream, urlFile);
        } catch (IOException e) {
            throw new S3_Exception(e.getMessage(), e);
        }
        return urlFile;
    }

    /**
     * @param key fileId
     */
    @Override
    public void deleteFile(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(this.bucket).key(key).build());
    }

    @Override
    public boolean doesBucketExist() {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(this.bucket)
                .build();

        try {
            s3.headBucket(headBucketRequest);
            return true;

        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    @Override
    public boolean createBucket() {
        if (doesBucketExist()) return false;

        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(this.bucket)
                .build());
        System.out.println("Bucket created: " + this.bucket);
        return true;
    }

    @Override
    public void deleteBucketAndContent() {
        if (!doesBucketExist()) return;

        ListObjectsResponse objectListing = s3.listObjects(ListObjectsRequest.builder().bucket(this.bucket).build());

        for (S3Object file : objectListing.contents())
            deleteFile(file.key());

        // After all objects are deleted, delete the bucket.
        s3.deleteBucket(DeleteBucketRequest.builder().bucket(this.bucket).build());

    }


    @Override
    public String downloadFileContentFromS3(String fileNameInS3, String fileType, String outputFileDir) {
        File result = downloadFile(fileNameInS3, outputFileDir + "\\" + fileNameInS3 + "." + fileType);
        StringBuilder body = new StringBuilder();
        String bodyString = "";
        try (FileReader fr = new FileReader(result)) {
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String line;
            while ((line = br.readLine()) != null) {
                body.append(line).append("\n");
            }
            bodyString = body.toString();
            bodyString = bodyString.substring(0,bodyString.length()-1);

        } catch (IOException e) {
            e.printStackTrace();
         }
        if (!result.delete())
            System.out.println("Fail: failed to delete file locally: " + fileNameInS3 + "." + fileType);
        return bodyString;
    }

    public String getBucket() {
        return bucket;
    }

}
