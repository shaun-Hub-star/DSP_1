package org.dsp.S3_service;

import java.io.File;

public interface IS3_Methods {

    /**
     * @param key fileId
     * @param path file path that you want to upload
     * */
    void uploadFile(String key, String path);
    /**
     * @param key fileId
     * @param file the file object that you want to upload
     * */
    void uploadFile(String key, File file);

    /**
     * @param key fileId
     * */
    File downloadFile(String key, String downloadTo);

    /**
     * @param key fileId
     * */
    void deleteFile(String key);

    boolean createBucket();

    void deleteBucketAndContent();

    boolean doesBucketExist();

    String downloadFileContentFromS3(String fileNameInS3, String fileType, String outputFileDir);

}
