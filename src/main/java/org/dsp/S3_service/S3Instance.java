package org.dsp.S3_service;

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
    private final Region region;
    private final S3Client s3;

    public S3Instance(Region region) {

        this.bucket = "bucket" + System.currentTimeMillis();
        this.region = region;
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
        this.region = region;
        this.s3 = S3Client.builder().
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

                /*
                * .createBucketConfiguration(
                                CreateBucketConfiguration.builder()
                                        .locationConstraint(region.id())
                                        .build())
                * */

        System.out.println("Bucket created: " + this.bucket);
        return true;
    }

    @Override
    public void deleteBucketAndContent() {
        if(!doesBucketExist()) return;

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
        try (FileReader fr = new FileReader(result)) {
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String line;
            while ((line = br.readLine()) != null) {
                body.append("\n").append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!result.delete())
            System.out.println("Fail: failed to delete file locally: " + fileNameInS3 + "." + fileType);
        return body.toString();
    }

    public String getBucket() {
        return bucket;
    }


    public static void main(String[] args) {
     /*   S3Instance s3 = new S3Instance(Region.US_EAST_1);
        s3.createBucket();
        String links_path = "/home/spl-labs/Desktop/DSP_213/src/main/resources/image_links.txt";
        s3.uploadFile("image_links.txt", links_path);
        File downloadedFile = s3.downloadFile("image_links.txt");
        List<String> images_link_ = parseImagesLinks(downloadedFile.getPath());
        List<String> images_link = new ArrayList<>(
                new HashSet<>(images_link_));
        images_link.forEach(System.out::println);*/


    }
}
