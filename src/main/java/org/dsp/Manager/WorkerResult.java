package org.dsp.Manager;

import org.dsp.messages.SQSMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkerResult {
    private final String imgLink;
    private final String ocrResult;

    public WorkerResult(String imgLink, String ocrResult){
        this.imgLink = imgLink;
        this.ocrResult = ocrResult;
    }

    public WorkerResult(SQSMessage sqsMessage){
        String[] split = sqsMessage.getBody().split("\n");
        if(split.length < 2){
            this.imgLink = "error";
            this.ocrResult = "error";
        }
        else{
            this.imgLink = split[0];
            StringBuilder body = new StringBuilder();
            for(int i = 1; i < split.length; i++){
                body.append(split[i]).append("\n");
            }
            this.ocrResult = body.substring(0, body.length() - 1);
        }
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
