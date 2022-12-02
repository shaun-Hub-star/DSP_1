package org.dsp.AWS_SERVICES.S3_service;

public class S3_Exception extends RuntimeException{
    /**
     * @param errorMessage is the error message that caused the error
     * @param err the delegated exception
     * */
    public S3_Exception(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
