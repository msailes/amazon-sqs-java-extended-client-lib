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

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Arrays;
import java.util.UUID;

public class ExampleApplication {

    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    public static void main(String[] args) {
        SqsClient sqsClient = SqsClient.create();
        S3Client s3Client = S3Client.create();
        String queueName = UUID.randomUUID().toString();
        System.out.println(queueName);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.withLargePayloadSupportEnabled(s3Client,"ms-extended-sqs-client");

        ExtendedSqsClient extendedSqsClient = new ExtendedSqsClientBuilder().withExtendedClientConfiguration(extendedClientConfiguration)
                .withSqsClient(sqsClient)
                .build();
        CreateQueueResponse createQueueResponse = sqsClient.createQueue(CreateQueueRequest.builder()
                .queueName(queueName).build());
        String queueUrl = createQueueResponse.queueUrl();
        System.out.println(queueUrl);

        int sizeOfMessageToSend = MORE_THAN_SQS_SIZE_LIMIT;
        int numberOfMessagesToSend = 10;

        for (int i = 0; i < numberOfMessagesToSend; i++) {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(generateStringWithLength(sizeOfMessageToSend))
                    .build();

            System.out.println("Sending message with String of length: " + sizeOfMessageToSend);
            extendedSqsClient.sendMessage(sendMessageRequest);
            sizeOfMessageToSend++;
        }

        ReceiveMessageResponse receiveMessageResponse = extendedSqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(numberOfMessagesToSend)
                .build());

        for (Message message : receiveMessageResponse.messages()) {
            System.out.println(message.body());
        }

        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    private static String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }
}
