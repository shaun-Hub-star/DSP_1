package org.dsp.SQS_service;

import org.dsp.Exceptions.SQS_Exception;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class SQSQueue implements ISQS_service{
    private final String queueName;
    private final Region region = Region.US_EAST_1;
    private final String queueUrl;
    private final SqsClient sqs;

    public SQSQueue(String queueName){
        this.queueName = queueName;
        this.sqs = SqsClient.builder().region(Region.US_WEST_2).build();

        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            CreateQueueResponse create_result = sqs.createQueue(request);
        } catch (QueueNameExistsException e) {
            throw new SQS_Exception(e);

        }
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        this.queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();
    }

    public void sendMessage(SQSMessage msg){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(msg.toString())
                //.delaySeconds(5) //WTF is this
                .build();
        sqs.sendMessage(send_msg_request);
    }


    public void sendMultiMsg(List<SQSMessage> messages) {
        List<SendMessageBatchRequestEntry> sendMessageBatchRequestEntries = new ArrayList<>();
        messages.forEach(msg -> sendMessageBatchRequestEntries.add(SendMessageBatchRequestEntry.builder()
                .messageBody(msg.getBody())
                .id(msg.getRequestId())
                .build()));

        SendMessageBatchRequest send_batch_request = SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(sendMessageBatchRequestEntries)
                .build();
        sqs.sendMessageBatch(send_batch_request);
    }

    public SQSMessage receiveMsg(){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();

        List<Message> messages = sqs.receiveMessage(receiveRequest).messages();

        // delete messages from the queue
        for (Message m : messages) {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(m.receiptHandle())
                    .build();
            sqs.deleteMessage(deleteRequest);
        }
        return null;//TODO
    }

}
