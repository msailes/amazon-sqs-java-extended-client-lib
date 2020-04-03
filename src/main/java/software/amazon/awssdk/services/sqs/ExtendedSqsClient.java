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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

public class ExtendedSqsClient implements SqsClient {
    private static final Logger LOG = LoggerFactory.getLogger(ExtendedSqsClient.class);

    private final ExtendedClientConfiguration clientConfiguration;
    private final SqsClient sqsClient;
    private final JsonDataConverter jsonDataConverter = new JsonDataConverter();

    /**
     * Constructs a new Amazon SQS extended client to invoke service methods on
     * Amazon SQS with extended functionality using the specified Amazon SQS
     * client object.
     *
     * <p>
     * All service calls made using this new client object are blocking, and
     * will not return until the service call completes.
     *
     * @param sqsClient
     *            The Amazon SQS client to use to connect to Amazon SQS.
     * @param extendedClientConfig
     *            The extended client configuration options controlling the
     *            functionality of this client.
     */
    protected ExtendedSqsClient(SqsClient sqsClient, ExtendedClientConfiguration extendedClientConfig) {
        this.sqsClient = sqsClient;
        this.clientConfiguration = new ExtendedClientConfiguration(extendedClientConfig);
    }

    public static ExtendedSqsClient defaultClient(String s3BucketName) {
        S3Client s3Client = S3Client.builder().build();
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration().withLargePayloadSupportEnabled(s3Client, s3BucketName);
        return new ExtendedSqsClientBuilder()
                .withSqsClient(SqsClient.builder().build())
                .withExtendedClientConfiguration(extendedClientConfiguration).build();
    }

    public static ExtendedSqsClientBuilder builder() {
        return new ExtendedSqsClientBuilder();
    }

    @Override
    public String serviceName() {
        return this.sqsClient.serviceName();
    }

    @Override
    public void close() {
        this.sqsClient.close();
    }

    @Override
    public SendMessageResponse sendMessage(SendMessageRequest sendMessageRequest) throws AwsServiceException, SdkClientException {
        if (sendMessageRequest == null) {
            String errorMessage = "sendMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        if (sendMessageRequest.messageBody() == null || "".equals(sendMessageRequest.messageBody())) {
            String errorMessage = "messageBody cannot be null or empty.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        SendMessageRequest updatedSendMessageRequest = storeMessageInS3(sendMessageRequest);

        return this.sqsClient.sendMessage(updatedSendMessageRequest);
    }

    private SendMessageRequest storeMessageInS3(SendMessageRequest sendMessageRequest) {
//        checkMessageAttributes(sendMessageRequest.messageAttributes());

        String s3Key = UUID.randomUUID().toString();
        String messageContentStr = sendMessageRequest.messageBody();
        Long messageContentSize = getStringSizeInBytes(messageContentStr);

        // Add a new message attribute as a flag
//        MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
//                .dataType("Number")
//                .stringValue(messageContentSize.toString())
//                .build();
//        sendMessageRequest.addMessageAttributesEntry(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME,
//                messageAttributeValue);

        storeTextInS3(s3Key, messageContentStr, messageContentSize);
        LOG.info("S3 object created, Bucket name: " + clientConfiguration.getS3BucketName() + ", Object key: " + s3Key
                + ".");

        MessageS3Pointer s3Pointer = new MessageS3Pointer(clientConfiguration.getS3BucketName(), s3Key);
        String s3PointerStr = getJSONFromS3Pointer(s3Pointer);
        return sendMessageRequest.toBuilder().messageBody(s3PointerStr).build();
    }

//    private void checkMessageAttributes(Map<String, MessageAttributeValue> messageAttributes) {
//        int msgAttributesSize = getMsgAttributesSize(messageAttributes);
//        if (msgAttributesSize > clientConfiguration.getMessageSizeThreshold()) {
//            String errorMessage = "Total size of Message attributes is " + msgAttributesSize
//                    + " bytes which is larger than the threshold of " + clientConfiguration.getMessageSizeThreshold()
//                    + " Bytes. Consider including the payload in the message body instead of message attributes.";
//            LOG.error(errorMessage);
//            throw new AmazonClientException(errorMessage);
//        }
//
//        int messageAttributesNum = messageAttributes.size();
//        if (messageAttributesNum > SQSExtendedClientConstants.MAX_ALLOWED_ATTRIBUTES) {
//            String errorMessage = "Number of message attributes [" + messageAttributesNum
//                    + "] exceeds the maximum allowed for large-payload messages ["
//                    + SQSExtendedClientConstants.MAX_ALLOWED_ATTRIBUTES + "].";
//            LOG.error(errorMessage);
//            throw new AmazonClientException(errorMessage);
//        }
//
//        MessageAttributeValue largePayloadAttributeValue = messageAttributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
//        if (largePayloadAttributeValue != null) {
//            String errorMessage = "Message attribute name " + SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME
//                    + " is reserved for use by SQS extended client.";
//            LOG.error(errorMessage);
//            throw new AmazonClientException(errorMessage);
//        }
//    }
//
//    private int getMsgAttributesSize(Map<String, MessageAttributeValue> msgAttributes) {
//        int totalMsgAttributesSize = 0;
//        for (Map.Entry<String, MessageAttributeValue> entry : msgAttributes.entrySet()) {
//            totalMsgAttributesSize += getStringSizeInBytes(entry.getKey());
//
//            MessageAttributeValue entryVal = entry.getValue();
//            if (entryVal.dataType() != null) {
//                totalMsgAttributesSize += getStringSizeInBytes(entryVal.dataType());
//            }
//
//            String stringVal = entryVal.stringValue();
//            if (stringVal != null) {
//                totalMsgAttributesSize += getStringSizeInBytes(entryVal.stringValue());
//            }
//
//            SdkBytes binaryVal = entryVal.binaryValue();
//            if (binaryVal != null) {
//                totalMsgAttributesSize += binaryVal.array().length;
//            }
//        }
//        return totalMsgAttributesSize;
//    }

    private String getJSONFromS3Pointer(MessageS3Pointer s3Pointer) {
        String s3PointerStr = null;
        try {
            s3PointerStr = jsonDataConverter.serializeToJson(s3Pointer);
        } catch (Exception e) {
            String errorMessage = "Failed to convert S3 object pointer to text. Message was not sent.";
            LOG.error(errorMessage, e);
            throw SdkClientException.create(errorMessage, e);
        }
        return s3PointerStr;
    }

    private void storeTextInS3(String s3Key, String messageContentStr, Long messageContentSize) {

        // @TODO Not sure if this is needed
        //        InputStream messageContentStream = new ByteArrayInputStream(messageContentStr.getBytes(StandardCharsets.UTF_8));
        //        ObjectMetadata messageContentStreamMetadata = new ObjectMetadata();
        //        messageContentStreamMetadata.setContentLength(messageContentSize);
        //        PutObjectRequest putObjectRequest = new PutObjectRequest(clientConfiguration.getS3BucketName(), s3Key,
        //                messageContentStream, messageContentStreamMetadata);
        S3Client amazonS3Client = this.clientConfiguration.getAmazonS3Client();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Key)
                .key(UUID.randomUUID().toString())
                .build();
        try {
            amazonS3Client.putObject(putObjectRequest, RequestBody.fromString(messageContentStr));
        } catch (SdkException e){
            String errorMessage = "Failed to store the message content in an S3 object. SQS message was not sent.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage, e);
        }
    }

    private static long getStringSizeInBytes(String str) {
        CountingOutputStream counterOutputStream = new CountingOutputStream();
        try {
            Writer writer = new OutputStreamWriter(counterOutputStream, "UTF-8");
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            String errorMessage = "Failed to calculate the size of message payload.";
            LOG.error(errorMessage, e);
            throw SdkClientException.create(errorMessage, e);
        }
        return counterOutputStream.getTotalSize();
    }
}
