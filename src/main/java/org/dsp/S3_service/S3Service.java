package org.dsp.S3_service;

import org.apache.commons.io.FileUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.dsp.Main.parseImagesLinks;

public class S3Service implements IS3_Methods{

    private final String bucket;
    private final Region region;
    private final S3Client s3;

    public S3Service() {

        this.bucket = "bucket" + System.currentTimeMillis();
        this.region = Region.US_EAST_1;
        this.s3 = S3Client.builder().region(region).build();
    }

    /**
     * @param key    fileId
     * @param path   file path that you want to upload
     */
    @Override
    public void uploadFile(String key, String path) {
        uploadFile(key, new File(path));
    }

    /**
     * @param key    fileId
     * @param file   the file object that you want to upload
     */
    @Override
    public void uploadFile(String key, File file) {
        s3.putObject(PutObjectRequest.builder().bucket(this.bucket).key(key).build(), RequestBody.fromFile(file));
    }

    /**
     * @param key    fileId
     */
    @Override
    public File downloadFile(String key) {
        File urlFile = new File("/home/spl-labs/Desktop/DSP_213/src/main/java/org/dsp/S3_service/test.txt");
        var responseInputStream = s3.getObject(GetObjectRequest.builder().bucket(this.bucket).key(key).build());
        try {
            FileUtils.copyInputStreamToFile(responseInputStream, urlFile);
        } catch (IOException e) {
            throw new S3_Exception(e.getMessage(),e);
        }
        return urlFile;
    }

    /**
     * @param key    fileId
     */
    @Override
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(this.bucket).key(key).build();
        s3.deleteObject(deleteObjectRequest);
    }

    @Override
    public void createBucket() {
        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucket)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .locationConstraint(region.id())
                                .build())
                .build());

        System.out.println(bucket);
    }


    @Override
    public void deleteBucket() {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(this.bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }


    public static void main(String[] args){
        S3Service s3 = new S3Service();
        s3.createBucket();
        String links_path = "/home/spl-labs/Desktop/DSP_213/src/main/resources/image_links.txt";
        s3.uploadFile("image_links.txt", links_path);
        File downloadedFile = s3.downloadFile("image_links.txt");
        List<String> images_link_ = parseImagesLinks(downloadedFile.getPath());
        List<String> images_link = new ArrayList<>(
                new HashSet<>(images_link_));
        images_link.forEach(System.out::println);


    }
}
