package org.dsp.messages;

import software.amazon.awssdk.services.sqs.model.Message;

public class SQSMessage {
    private final String msg;
    private final String requestId;

    public SQSMessage(String msg, String requestId){
        this.msg = msg;
        this.requestId = requestId;
    }
    /**
     * @param msg is a string that we would like to extract the msg and the request id
     * */

    public SQSMessage(Message msg){
        this.msg = msg.body();
        this.requestId = msg.messageId();
    }

    @Override
    public String toString() {
        return requestId + '\n' + msg;
    }

    public String getBody() {
        return msg;
    }

    public String getRequestId() {
        return requestId;
    }
}
