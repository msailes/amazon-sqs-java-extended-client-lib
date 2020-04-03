/*
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

package software.amazon.awssdk.services.sqs;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Arrays;

import static com.tomasmalmsten.matchers.StringMatchesUUIDPattern.matchesThePatternOfAUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class ExtendedSqsClientTest {

    private SqsClient extendedSqsWithDefaultConfig;
    private SqsClient mockSqsBackend;
    private S3Client mockS3;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRATY_SMALLER_THRESSHOLD = 500;

    @Before
    public void setupClient() {
        mockS3 = mock(S3Client.class);
        mockSqsBackend = mock(SqsClient.class);
        when(mockS3.putObject(isA(PutObjectRequest.class), isA(RequestBody.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME);

        extendedSqsWithDefaultConfig = new ExtendedSqsClient(mockSqsBackend, extendedClientConfiguration);
    }

    @Test
    public void testWhenSendLargeMessageThenPayloadIsStoredInS3() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(SQS_QUEUE_URL)
                .messageBody(messageBody)
                .build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class), isA(RequestBody.class));
    }

    @Test(expected = SdkClientException.class)
    public void testNullMessageRequestThrowsClientException() {
        SendMessageRequest messageRequest = null;
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);
    }

    @Test(expected = SdkClientException.class)
    public void testEmptyMessageBodyThrowsClientException() {
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .messageBody("")
                .build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);
    }

    @Test(expected = SdkClientException.class)
    public void testNullMessageBodyThrowsClientException() {
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .messageBody(null)
                .build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);
    }

    @Test(expected = SdkClientException.class)
    public void testThatSqsMessageNotSentIfS3PutObjectFails() {
        S3Client s3Client = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.withLargePayloadSupportEnabled(s3Client, S3_BUCKET_NAME);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.create("Ekk", new Exception()));

        ExtendedSqsClient extendedSqsClient = new ExtendedSqsClient(mockSqsBackend, extendedClientConfiguration);

        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(SQS_QUEUE_URL)
                .messageBody("anything")
                .build();

        extendedSqsClient.sendMessage(messageRequest);
    }

    @Test
    public void testThatS3PointerIsSentWhenMessageStoredInS3() throws Exception {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(SQS_QUEUE_URL)
                .messageBody(messageBody)
                .build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(captor.capture());

        String capturedMessageBody = captor.getValue().messageBody();
        JsonDataConverter jsonDataConverter = new JsonDataConverter();
        MessageS3Pointer messageS3Pointer = jsonDataConverter.deserializeFromJson(capturedMessageBody, MessageS3Pointer.class);

        assertThat(messageS3Pointer.getS3BucketName(), equalTo(S3_BUCKET_NAME));
        assertThat(messageS3Pointer.getS3Key(), matchesThePatternOfAUUID());
    }

    private String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }
}
