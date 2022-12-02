package org.dsp.messages;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SQSMessage {
    private final String msg;
    private final String requestId;
    private final Message amazonMessage;

//    public SQSMessage(String msg, String requestId){
//        this.msg = msg;
//        this.requestId = requestId;
//    }
    /**
     * @param msg is a string that we would like to extract the msg and the request id
     * */

    public SQSMessage(Message msg){
        this.amazonMessage = msg;
        String[] split = msg.body().split("\n");
        this.requestId = split[0];
        this.msg = split[1];
    }
    /*
    * in practice the clientId is the bucket for some client
    *
    * */
    public SQSMessage(String messageBody, String clientId){
        this.amazonMessage = Message.builder().messageId(clientId).body(messageBody).build();
        this.msg = messageBody;
        this.requestId = clientId;
    }
    @Override
    public String toString() {
        return requestId + "\n" + msg;
    }

    public String getBody() {
        return msg;
    }

    public String getRequestId() {
        return requestId;
    }

    public Message getMessage() {
        return amazonMessage;
    }
}
