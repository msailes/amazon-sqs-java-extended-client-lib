package software.amazon.awssdk.services.sqs;/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ExampleApplication {

    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    public static final int NUMBER_OF_MESSAGES = 10;

    public static void main(String[] args) {
        ExampleApplication exampleApplication = new ExampleApplication();
        exampleApplication.run();
    }

    private void run() {
        ExtendedSqsClient extendedSqsClient = ExtendedSqsClient.defaultClient("ms-extended-sqs-client");

        String queueName = UUID.randomUUID().toString();
        System.out.println(queueName);
        CreateQueueResponse createQueueResponse = extendedSqsClient.createQueue(CreateQueueRequest.builder()
                .queueName(queueName).build());
        String queueUrl = createQueueResponse.queueUrl();
        System.out.println(queueUrl);

        int sizeOfMessageToSend = MORE_THAN_SQS_SIZE_LIMIT;

        for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(generateStringWithLength(sizeOfMessageToSend))
                    .build();

            System.out.println("Sending message with String of length: " + sizeOfMessageToSend);
            extendedSqsClient.sendMessage(sendMessageRequest);
            sizeOfMessageToSend++;
        }

        int numberOfMessagesRecieved = 0;

        do {
            ReceiveMessageResponse receiveMessageResponse = extendedSqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(10)
                    .build());

            List<DeleteMessageBatchRequestEntry> receiptList = new ArrayList<>();

            for (Message message : receiveMessageResponse.messages()) {
                System.out.println(message.body().substring(0, 5) + "...");
                DeleteMessageBatchRequestEntry deleteRequest = DeleteMessageBatchRequestEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .receiptHandle(message.receiptHandle())
                        .build();
                receiptList.add(deleteRequest);
                numberOfMessagesRecieved++;
                System.out.println("Number of messages recieved: " + numberOfMessagesRecieved);
            }

            DeleteMessageBatchRequest deleteMessageBatchRequest = DeleteMessageBatchRequest.builder()
                    .entries(receiptList)
                    .queueUrl(queueUrl)
                    .build();
            extendedSqsClient.deleteMessageBatch(deleteMessageBatchRequest);

        } while (numberOfMessagesRecieved < NUMBER_OF_MESSAGES );

        extendedSqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    private static String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }
}
