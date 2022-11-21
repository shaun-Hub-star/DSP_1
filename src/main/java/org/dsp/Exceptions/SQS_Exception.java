package org.dsp.Exceptions;

public class SQS_Exception extends RuntimeException{
    public SQS_Exception(Throwable err) {
        super(err.getMessage(), err);
    }
}
