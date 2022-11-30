package org.dsp.SQS_service;

import org.dsp.Exceptions.SQS_Exception;
import org.dsp.messages.SQSMessage;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;


public class SQSQueue implements ISQS_service {
    private final String queueName;
    private final Region region;
    private final String queueUrl;
    private final SqsClient sqs;


    public SQSQueue(String queueName, Region region) {
        String regex = "((?![a-zA-Z0-9\\s]).)*";
        this.queueName = queueName.replaceAll(regex, "");
        this.region = region;
        this.sqs = SqsClient.builder().region(this.region).
                credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(this.queueName)
                    .build();
            CreateQueueResponse create_result = sqs.createQueue(request);
            System.out.println("Queue has been created: " + create_result);
        } catch (QueueNameExistsException ignore) {
/*
            if(!queueName.equals("localToManagerSQS") && !queueName.startsWith("bucket")) { //sqs Q
                System.out.println("check that it is only happening with localToManagerSQS "+e.getMessage());
                throw e;
            }
*/
        }
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(this.queueName)
                .build();
        this.queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();
    }

    public void sendMessage(SQSMessage msg) {
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(msg.toString())
                .delaySeconds(5)
                .build();
        sqs.sendMessage(send_msg_request);
    }


    public void sendMultiMsg(List<SQSMessage> messages) {
        List<SendMessageBatchRequestEntry> sendMessageBatchRequestEntries = new ArrayList<>();
        messages.forEach(msg -> sendMessageBatchRequestEntries.add(SendMessageBatchRequestEntry.builder()
                .messageBody(msg.toString())
                .id(msg.getRequestId())
                .build()));

        SendMessageBatchRequest send_batch_request = SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(sendMessageBatchRequestEntries)
                .build();
        sqs.sendMessageBatch(send_batch_request);
    }

    public synchronized SQSMessage receiveMsg() {
        int timeInMinutes = 8;
        int waitTimeSecondsForEachIteration = 20;
        for(int i = 0; i < timeInMinutes * 60 / waitTimeSecondsForEachIteration; i++){
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .visibilityTimeout(80)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(waitTimeSecondsForEachIteration) //FIXME???
                    .build();


            List<Message> messages = sqs.receiveMessage(receiveRequest).messages();
            //BlockingQueue<Message> blockingQueue = new LinkedBlockingQueue<>(messages);
            if (messages.size() == 0) {
                continue;
            }

            return new SQSMessage(messages.get(0));
        }

        throw new SQS_Exception(new RuntimeException("Could not receive massage in time. Q: " + this.queueName));
    }

    public void deleteMessage(SQSMessage sqsMessage) {
        Message message = sqsMessage.getMessage();
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build();
        sqs.deleteMessage(deleteRequest);
    }

    public void deleteQueue() {
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();
        sqs.deleteQueue(deleteQueueRequest);
    }

}
