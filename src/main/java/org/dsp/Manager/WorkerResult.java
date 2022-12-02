package org.dsp.Manager;

import org.dsp.messages.SQSMessage;

public class WorkerResult {
    private final String imgLink;
    private final String ocrResult;

    public WorkerResult(String imgLink, String ocrResult){
        this.imgLink = imgLink;
        this.ocrResult = ocrResult;
    }

    public WorkerResult(SQSMessage sqsMessage){
        String[] split = sqsMessage.getBody().split("\n");
        this.imgLink = split[0];
        this.ocrResult = split[1];
    }

    public String getImgLink() {
        return imgLink;
    }

    public String getOcrResult() {
        return ocrResult;
    }

    @Override
    public String toString() {
        return "WorkerResult{" +
                "imgLink='" + imgLink + '\'' +
                ", ocrResult='" + ocrResult + '\'' +
                '}';
    }
}
