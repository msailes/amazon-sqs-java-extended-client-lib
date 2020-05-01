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
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.BatchEntryIdsNotDistinctException;
import software.amazon.awssdk.services.sqs.model.BatchRequestTooLongException;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.EmptyBatchRequestException;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.InvalidAttributeNameException;
import software.amazon.awssdk.services.sqs.model.InvalidBatchEntryIdException;
import software.amazon.awssdk.services.sqs.model.InvalidIdFormatException;
import software.amazon.awssdk.services.sqs.model.InvalidMessageContentsException;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsRequest;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageNotInflightException;
import software.amazon.awssdk.services.sqs.model.OverLimitException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueDeletedRecentlyException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.RemovePermissionResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.TagQueueRequest;
import software.amazon.awssdk.services.sqs.model.TagQueueResponse;
import software.amazon.awssdk.services.sqs.model.TooManyEntriesInBatchRequestException;
import software.amazon.awssdk.services.sqs.model.UntagQueueRequest;
import software.amazon.awssdk.services.sqs.model.UntagQueueResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Extended SQS Client extends the functionality of SQS client.
 * All service calls made using this client are blocking, and will not returnØ
 * until the service call completes.
 *
 * <p>
 * The Extended SQS extended client enables sending and receiving large messages
 * via Amazon S3. You can use this library to:
 * </p>
 *
 * <ul>
 * <li>Specify whether messages are always stored in Amazon S3 or only when a
 * message size exceeds 256 KB.</li>
 * <li>Send a message that references a single message object stored in an
 * Amazon S3 bucket.</li>
 * <li>Get the corresponding message object from an Amazon S3 bucket.</li>
 * <li>Delete the corresponding message object from an Amazon S3 bucket.</li>
 * </ul>
 */
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
     * @param sqsClient            The Amazon SQS client to use to connect to Amazon SQS.
     * @param extendedClientConfig The extended client configuration options controlling the
     *                             functionality of this client.
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

    /**
     * <p>
     * Adds a permission to a queue for a specific <a
     * href="https://docs.aws.amazon.com/general/latest/gr/glos-chap.html#P">principal</a>. This allows sharing access
     * to the queue.
     * </p>
     * <p>
     * When you create a queue, you have full control access rights for the queue. Only you, the owner of the queue, can
     * grant or deny permissions to the queue. For more information about these permissions, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html#write-messages-to-shared-queue"
     * >Allow Developers to Write Messages to a Shared Queue</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * <code>AddPermission</code> generates a policy for you. You can use <code> <a>SetQueueAttributes</a> </code> to
     * upload your policy. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-creating-custom-policies.html"
     * >Using Custom Policies with the Amazon SQS Access Policy Language</a> in the <i>Amazon Simple Queue Service
     * Developer Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * An Amazon SQS policy can have a maximum of 7 actions.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param addPermissionRequest
     * @return Result of the AddPermission operation returned by the service.
     * @throws OverLimitException
     *         The specified action violates a limit. For example, <code>ReceiveMessage</code> returns this error if the
     *         maximum number of inflight messages is reached and <code>AddPermission</code> returns this error if the
     *         maximum number of permissions for the queue is reached.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.AddPermission
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/AddPermission" target="_top">AWS API
     *      Documentation</a>
     */
    public AddPermissionResponse addPermission(AddPermissionRequest addPermissionRequest) throws OverLimitException,
            AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.addPermission(addPermissionRequest);
    }

    /**
     * <p>
     * Adds a permission to a queue for a specific <a
     * href="https://docs.aws.amazon.com/general/latest/gr/glos-chap.html#P">principal</a>. This allows sharing access
     * to the queue.
     * </p>
     * <p>
     * When you create a queue, you have full control access rights for the queue. Only you, the owner of the queue, can
     * grant or deny permissions to the queue. For more information about these permissions, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html#write-messages-to-shared-queue"
     * >Allow Developers to Write Messages to a Shared Queue</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * <code>AddPermission</code> generates a policy for you. You can use <code> <a>SetQueueAttributes</a> </code> to
     * upload your policy. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-creating-custom-policies.html"
     * >Using Custom Policies with the Amazon SQS Access Policy Language</a> in the <i>Amazon Simple Queue Service
     * Developer Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * An Amazon SQS policy can have a maximum of 7 actions.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link AddPermissionRequest.Builder} avoiding the need to
     * create one manually via {@link AddPermissionRequest#builder()}
     * </p>
     *
     * @param addPermissionRequest
     *        A {@link Consumer} that will call methods on {@link AddPermissionRequest.Builder} to create a request.
     * @return Result of the AddPermission operation returned by the service.
     * @throws OverLimitException
     *         The specified action violates a limit. For example, <code>ReceiveMessage</code> returns this error if the
     *         maximum number of inflight messages is reached and <code>AddPermission</code> returns this error if the
     *         maximum number of permissions for the queue is reached.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.AddPermission
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/AddPermission" target="_top">AWS API
     *      Documentation</a>
     */
    public AddPermissionResponse addPermission(Consumer<AddPermissionRequest.Builder> addPermissionRequest)
            throws OverLimitException, AwsServiceException, SdkClientException, SqsException {
        return addPermission(AddPermissionRequest.builder().applyMutation(addPermissionRequest).build());
    }

    /**
     * <p>
     * Changes the visibility timeout of a specified message in a queue to a new value. The default visibility timeout
     * for a message is 30 seconds. The minimum is 0 seconds. The maximum is 12 hours. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html">
     * Visibility Timeout</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * For example, you have a message with a visibility timeout of 5 minutes. After 3 minutes, you call
     * <code>ChangeMessageVisibility</code> with a timeout of 10 minutes. You can continue to call
     * <code>ChangeMessageVisibility</code> to extend the visibility timeout to the maximum allowed time. If you try to
     * extend the visibility timeout beyond the maximum, your request is rejected.
     * </p>
     * <p>
     * An Amazon SQS message has three basic states:
     * </p>
     * <ol>
     * <li>
     * <p>
     * Sent to a queue by a producer.
     * </p>
     * </li>
     * <li>
     * <p>
     * Received from the queue by a consumer.
     * </p>
     * </li>
     * <li>
     * <p>
     * Deleted from the queue.
     * </p>
     * </li>
     * </ol>
     * <p>
     * A message is considered to be <i>stored</i> after it is sent to a queue by a producer, but not yet received from
     * the queue by a consumer (that is, between states 1 and 2). There is no limit to the number of stored messages. A
     * message is considered to be <i>in flight</i> after it is received from a queue by a consumer, but not yet deleted
     * from the queue (that is, between states 2 and 3). There is a limit to the number of inflight messages.
     * </p>
     * <p>
     * Limits that apply to inflight messages are unrelated to the <i>unlimited</i> number of stored messages.
     * </p>
     * <p>
     * For most standard queues (depending on queue traffic and message backlog), there can be a maximum of
     * approximately 120,000 inflight messages (received from a queue by a consumer, but not yet deleted from the
     * queue). If you reach this limit, Amazon SQS returns the <code>OverLimit</code> error message. To avoid reaching
     * the limit, you should delete messages from the queue after they're processed. You can also increase the number of
     * queues you use to process your messages. To request a limit increase, <a href=
     * "https://console.aws.amazon.com/support/home#/case/create?issueType=service-limit-increase&amp;limitType=service-code-sqs"
     * >file a support request</a>.
     * </p>
     * <p>
     * For FIFO queues, there can be a maximum of 20,000 inflight messages (received from a queue by a consumer, but not
     * yet deleted from the queue). If you reach this limit, Amazon SQS returns no error messages.
     * </p>
     * <important>
     * <p>
     * If you attempt to set the <code>VisibilityTimeout</code> to a value greater than the maximum time left, Amazon
     * SQS returns an error. Amazon SQS doesn't automatically recalculate and increase the timeout to the maximum
     * remaining time.
     * </p>
     * <p>
     * Unlike with a queue, when you change the visibility timeout for a specific message the timeout value is applied
     * immediately but isn't saved in memory for that message. If you don't delete a message after it is received, the
     * visibility timeout for the message reverts to the original timeout value (not to the value you set using the
     * <code>ChangeMessageVisibility</code> action) the next time the message is received.
     * </p>
     * </important>
     *
     * @param changeMessageVisibilityRequest
     * @return Result of the ChangeMessageVisibility operation returned by the service.
     * @throws MessageNotInflightException
     *         The specified message isn't in flight.
     * @throws ReceiptHandleIsInvalidException
     *         The specified receipt handle isn't valid.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ChangeMessageVisibility
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ChangeMessageVisibility" target="_top">AWS
     *      API Documentation</a>
     */
    public ChangeMessageVisibilityResponse changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws MessageNotInflightException, ReceiptHandleIsInvalidException, AwsServiceException, SdkClientException,
            SqsException {
        return this.sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    /**
     * <p>
     * Changes the visibility timeout of a specified message in a queue to a new value. The default visibility timeout
     * for a message is 30 seconds. The minimum is 0 seconds. The maximum is 12 hours. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html">
     * Visibility Timeout</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * For example, you have a message with a visibility timeout of 5 minutes. After 3 minutes, you call
     * <code>ChangeMessageVisibility</code> with a timeout of 10 minutes. You can continue to call
     * <code>ChangeMessageVisibility</code> to extend the visibility timeout to the maximum allowed time. If you try to
     * extend the visibility timeout beyond the maximum, your request is rejected.
     * </p>
     * <p>
     * An Amazon SQS message has three basic states:
     * </p>
     * <ol>
     * <li>
     * <p>
     * Sent to a queue by a producer.
     * </p>
     * </li>
     * <li>
     * <p>
     * Received from the queue by a consumer.
     * </p>
     * </li>
     * <li>
     * <p>
     * Deleted from the queue.
     * </p>
     * </li>
     * </ol>
     * <p>
     * A message is considered to be <i>stored</i> after it is sent to a queue by a producer, but not yet received from
     * the queue by a consumer (that is, between states 1 and 2). There is no limit to the number of stored messages. A
     * message is considered to be <i>in flight</i> after it is received from a queue by a consumer, but not yet deleted
     * from the queue (that is, between states 2 and 3). There is a limit to the number of inflight messages.
     * </p>
     * <p>
     * Limits that apply to inflight messages are unrelated to the <i>unlimited</i> number of stored messages.
     * </p>
     * <p>
     * For most standard queues (depending on queue traffic and message backlog), there can be a maximum of
     * approximately 120,000 inflight messages (received from a queue by a consumer, but not yet deleted from the
     * queue). If you reach this limit, Amazon SQS returns the <code>OverLimit</code> error message. To avoid reaching
     * the limit, you should delete messages from the queue after they're processed. You can also increase the number of
     * queues you use to process your messages. To request a limit increase, <a href=
     * "https://console.aws.amazon.com/support/home#/case/create?issueType=service-limit-increase&amp;limitType=service-code-sqs"
     * >file a support request</a>.
     * </p>
     * <p>
     * For FIFO queues, there can be a maximum of 20,000 inflight messages (received from a queue by a consumer, but not
     * yet deleted from the queue). If you reach this limit, Amazon SQS returns no error messages.
     * </p>
     * <important>
     * <p>
     * If you attempt to set the <code>VisibilityTimeout</code> to a value greater than the maximum time left, Amazon
     * SQS returns an error. Amazon SQS doesn't automatically recalculate and increase the timeout to the maximum
     * remaining time.
     * </p>
     * <p>
     * Unlike with a queue, when you change the visibility timeout for a specific message the timeout value is applied
     * immediately but isn't saved in memory for that message. If you don't delete a message after it is received, the
     * visibility timeout for the message reverts to the original timeout value (not to the value you set using the
     * <code>ChangeMessageVisibility</code> action) the next time the message is received.
     * </p>
     * </important><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ChangeMessageVisibilityRequest.Builder} avoiding
     * the need to create one manually via {@link ChangeMessageVisibilityRequest#builder()}
     * </p>
     *
     * @param changeMessageVisibilityRequest
     *        A {@link Consumer} that will call methods on {@link ChangeMessageVisibilityRequest.Builder} to create a
     *        request.
     * @return Result of the ChangeMessageVisibility operation returned by the service.
     * @throws MessageNotInflightException
     *         The specified message isn't in flight.
     * @throws ReceiptHandleIsInvalidException
     *         The specified receipt handle isn't valid.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ChangeMessageVisibility
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ChangeMessageVisibility" target="_top">AWS
     *      API Documentation</a>
     */
    public ChangeMessageVisibilityResponse changeMessageVisibility(
            Consumer<ChangeMessageVisibilityRequest.Builder> changeMessageVisibilityRequest) throws MessageNotInflightException,
            ReceiptHandleIsInvalidException, AwsServiceException, SdkClientException, SqsException {
        return changeMessageVisibility(ChangeMessageVisibilityRequest.builder().applyMutation(changeMessageVisibilityRequest)
                .build());
    }

    /**
     * <p>
     * Changes the visibility timeout of multiple messages. This is a batch version of
     * <code> <a>ChangeMessageVisibility</a>.</code> The result of the action on each message is reported individually
     * in the response. You can send up to 10 <code> <a>ChangeMessageVisibility</a> </code> requests with each
     * <code>ChangeMessageVisibilityBatch</code> action.
     * </p>
     * <important>
     * <p>
     * Because the batch request can result in a combination of successful and unsuccessful actions, you should check
     * for batch errors even when the call returns an HTTP status code of <code>200</code>.
     * </p>
     * </important>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     *
     * @param changeMessageVisibilityBatchRequest
     * @return Result of the ChangeMessageVisibilityBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ChangeMessageVisibilityBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ChangeMessageVisibilityBatch"
     *      target="_top">AWS API Documentation</a>
     */
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest)
            throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException,
            InvalidBatchEntryIdException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    /**
     * <p>
     * Changes the visibility timeout of multiple messages. This is a batch version of
     * <code> <a>ChangeMessageVisibility</a>.</code> The result of the action on each message is reported individually
     * in the response. You can send up to 10 <code> <a>ChangeMessageVisibility</a> </code> requests with each
     * <code>ChangeMessageVisibilityBatch</code> action.
     * </p>
     * <important>
     * <p>
     * Because the batch request can result in a combination of successful and unsuccessful actions, you should check
     * for batch errors even when the call returns an HTTP status code of <code>200</code>.
     * </p>
     * </important>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ChangeMessageVisibilityBatchRequest.Builder}
     * avoiding the need to create one manually via {@link ChangeMessageVisibilityBatchRequest#builder()}
     * </p>
     *
     * @param changeMessageVisibilityBatchRequest
     *        A {@link Consumer} that will call methods on {@link ChangeMessageVisibilityBatchRequest.Builder} to create
     *        a request.
     * @return Result of the ChangeMessageVisibilityBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ChangeMessageVisibilityBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ChangeMessageVisibilityBatch"
     *      target="_top">AWS API Documentation</a>
     */
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(
            Consumer<ChangeMessageVisibilityBatchRequest.Builder> changeMessageVisibilityBatchRequest)
            throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException,
            InvalidBatchEntryIdException, AwsServiceException, SdkClientException, SqsException {
        return changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest.builder()
                .applyMutation(changeMessageVisibilityBatchRequest).build());
    }

    /**
     * <p>
     * Creates a new standard or FIFO queue. You can pass one or more attributes in the request. Keep the following
     * caveats in mind:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If you don't specify the <code>FifoQueue</code> attribute, Amazon SQS creates a standard queue.
     * </p>
     * <note>
     * <p>
     * You can't change the queue type after you create it and you can't convert an existing standard queue into a FIFO
     * queue. You must either create a new FIFO queue for your application or delete your existing standard queue and
     * recreate it as a FIFO queue. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html#FIFO-queues-moving"
     * >Moving From a Standard Queue to a FIFO Queue</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * </note></li>
     * <li>
     * <p>
     * If you don't provide a value for an attribute, the queue is created with the default value for the attribute.
     * </p>
     * </li>
     * <li>
     * <p>
     * If you delete a queue, you must wait at least 60 seconds before creating a queue with the same name.
     * </p>
     * </li>
     * </ul>
     * <p>
     * To successfully create a new queue, you must provide a queue name that adheres to the <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/limits-queues.html">limits
     * related to queues</a> and is unique within the scope of your queues.
     * </p>
     * <p>
     * To get the queue URL, use the <code> <a>GetQueueUrl</a> </code> action. <code> <a>GetQueueUrl</a> </code>
     * requires only the <code>QueueName</code> parameter. be aware of existing queue names:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If you provide the name of an existing queue along with the exact names and values of all the queue's attributes,
     * <code>CreateQueue</code> returns the queue URL for the existing queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * If the queue name, attribute names, or attribute values don't match an existing queue, <code>CreateQueue</code>
     * returns an error.
     * </p>
     * </li>
     * </ul>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param createQueueRequest
     * @return Result of the CreateQueue operation returned by the service.
     * @throws QueueDeletedRecentlyException
     *         You must wait 60 seconds after deleting a queue before you can create another queue with the same name.
     * @throws QueueNameExistsException
     *         A queue with this name already exists. Amazon SQS returns this error only if the request includes
     *         attributes whose values differ from those of the existing queue.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.CreateQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/CreateQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public CreateQueueResponse createQueue(CreateQueueRequest createQueueRequest) throws QueueDeletedRecentlyException,
            QueueNameExistsException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.createQueue(createQueueRequest);
    }

    /**
     * <p>
     * Creates a new standard or FIFO queue. You can pass one or more attributes in the request. Keep the following
     * caveats in mind:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If you don't specify the <code>FifoQueue</code> attribute, Amazon SQS creates a standard queue.
     * </p>
     * <note>
     * <p>
     * You can't change the queue type after you create it and you can't convert an existing standard queue into a FIFO
     * queue. You must either create a new FIFO queue for your application or delete your existing standard queue and
     * recreate it as a FIFO queue. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html#FIFO-queues-moving"
     * >Moving From a Standard Queue to a FIFO Queue</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * </note></li>
     * <li>
     * <p>
     * If you don't provide a value for an attribute, the queue is created with the default value for the attribute.
     * </p>
     * </li>
     * <li>
     * <p>
     * If you delete a queue, you must wait at least 60 seconds before creating a queue with the same name.
     * </p>
     * </li>
     * </ul>
     * <p>
     * To successfully create a new queue, you must provide a queue name that adheres to the <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/limits-queues.html">limits
     * related to queues</a> and is unique within the scope of your queues.
     * </p>
     * <p>
     * To get the queue URL, use the <code> <a>GetQueueUrl</a> </code> action. <code> <a>GetQueueUrl</a> </code>
     * requires only the <code>QueueName</code> parameter. be aware of existing queue names:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If you provide the name of an existing queue along with the exact names and values of all the queue's attributes,
     * <code>CreateQueue</code> returns the queue URL for the existing queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * If the queue name, attribute names, or attribute values don't match an existing queue, <code>CreateQueue</code>
     * returns an error.
     * </p>
     * </li>
     * </ul>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link CreateQueueRequest.Builder} avoiding the need to
     * create one manually via {@link CreateQueueRequest#builder()}
     * </p>
     *
     * @param createQueueRequest
     *        A {@link Consumer} that will call methods on {@link CreateQueueRequest.Builder} to create a request.
     * @return Result of the CreateQueue operation returned by the service.
     * @throws QueueDeletedRecentlyException
     *         You must wait 60 seconds after deleting a queue before you can create another queue with the same name.
     * @throws QueueNameExistsException
     *         A queue with this name already exists. Amazon SQS returns this error only if the request includes
     *         attributes whose values differ from those of the existing queue.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.CreateQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/CreateQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public CreateQueueResponse createQueue(Consumer<CreateQueueRequest.Builder> createQueueRequest)
            throws QueueDeletedRecentlyException, QueueNameExistsException, AwsServiceException, SdkClientException, SqsException {
        return createQueue(CreateQueueRequest.builder().applyMutation(createQueueRequest).build());
    }

    /**
     * <p>
     * Deletes the specified message from the specified queue. To select the message to delete, use the
     * <code>ReceiptHandle</code> of the message (<i>not</i> the <code>MessageId</code> which you receive when you send
     * the message). Amazon SQS can delete a message from a queue even if a visibility timeout setting causes the
     * message to be locked by another consumer. Amazon SQS automatically deletes messages left in a queue longer than
     * the retention period configured for the queue.
     * </p>
     * <note>
     * <p>
     * The <code>ReceiptHandle</code> is associated with a <i>specific instance</i> of receiving a message. If you
     * receive a message more than once, the <code>ReceiptHandle</code> is different each time you receive a message.
     * When you use the <code>DeleteMessage</code> action, you must provide the most recently received
     * <code>ReceiptHandle</code> for the message (otherwise, the request succeeds, but the message might not be
     * deleted).
     * </p>
     * <p>
     * For standard queues, it is possible to receive a message even after you delete it. This might happen on rare
     * occasions if one of the servers which stores a copy of the message is unavailable when you send the request to
     * delete the message. The copy remains on the server and might be returned to you during a subsequent receive
     * request. You should ensure that your application is idempotent, so that receiving a message more than once does
     * not cause issues.
     * </p>
     * </note>
     *
     * @param deleteMessageRequest
     * @return Result of the DeleteMessage operation returned by the service.
     * @throws InvalidIdFormatException
     *         The specified receipt handle isn't valid for the current version.
     * @throws ReceiptHandleIsInvalidException
     *         The specified receipt handle isn't valid.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteMessageResponse deleteMessage(DeleteMessageRequest deleteMessageRequest) throws InvalidIdFormatException,
            ReceiptHandleIsInvalidException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.deleteMessage(deleteMessageRequest);
    }

    /**
     * <p>
     * Deletes the specified message from the specified queue. To select the message to delete, use the
     * <code>ReceiptHandle</code> of the message (<i>not</i> the <code>MessageId</code> which you receive when you send
     * the message). Amazon SQS can delete a message from a queue even if a visibility timeout setting causes the
     * message to be locked by another consumer. Amazon SQS automatically deletes messages left in a queue longer than
     * the retention period configured for the queue.
     * </p>
     * <note>
     * <p>
     * The <code>ReceiptHandle</code> is associated with a <i>specific instance</i> of receiving a message. If you
     * receive a message more than once, the <code>ReceiptHandle</code> is different each time you receive a message.
     * When you use the <code>DeleteMessage</code> action, you must provide the most recently received
     * <code>ReceiptHandle</code> for the message (otherwise, the request succeeds, but the message might not be
     * deleted).
     * </p>
     * <p>
     * For standard queues, it is possible to receive a message even after you delete it. This might happen on rare
     * occasions if one of the servers which stores a copy of the message is unavailable when you send the request to
     * delete the message. The copy remains on the server and might be returned to you during a subsequent receive
     * request. You should ensure that your application is idempotent, so that receiving a message more than once does
     * not cause issues.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteMessageRequest.Builder} avoiding the need to
     * create one manually via {@link DeleteMessageRequest#builder()}
     * </p>
     *
     * @param deleteMessageRequest
     *        A {@link Consumer} that will call methods on {@link DeleteMessageRequest.Builder} to create a request.
     * @return Result of the DeleteMessage operation returned by the service.
     * @throws InvalidIdFormatException
     *         The specified receipt handle isn't valid for the current version.
     * @throws ReceiptHandleIsInvalidException
     *         The specified receipt handle isn't valid.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteMessageResponse deleteMessage(Consumer<DeleteMessageRequest.Builder> deleteMessageRequest)
            throws InvalidIdFormatException, ReceiptHandleIsInvalidException, AwsServiceException, SdkClientException,
            SqsException {
        return deleteMessage(DeleteMessageRequest.builder().applyMutation(deleteMessageRequest).build());
    }

    /**
     * <p>
     * Deletes up to ten messages from the specified queue. This is a batch version of
     * <code> <a>DeleteMessage</a>.</code> The result of the action on each message is reported individually in the
     * response.
     * </p>
     * <important>
     * <p>
     * Because the batch request can result in a combination of successful and unsuccessful actions, you should check
     * for batch errors even when the call returns an HTTP status code of <code>200</code>.
     * </p>
     * </important>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     *
     * @param deleteMessageBatchRequest
     * @return Result of the DeleteMessageBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteMessageBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteMessageBatch" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteMessageBatchResponse deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException,
            InvalidBatchEntryIdException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
    }

    /**
     * <p>
     * Deletes up to ten messages from the specified queue. This is a batch version of
     * <code> <a>DeleteMessage</a>.</code> The result of the action on each message is reported individually in the
     * response.
     * </p>
     * <important>
     * <p>
     * Because the batch request can result in a combination of successful and unsuccessful actions, you should check
     * for batch errors even when the call returns an HTTP status code of <code>200</code>.
     * </p>
     * </important>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteMessageBatchRequest.Builder} avoiding the
     * need to create one manually via {@link DeleteMessageBatchRequest#builder()}
     * </p>
     *
     * @param deleteMessageBatchRequest
     *        A {@link Consumer} that will call methods on {@link DeleteMessageBatchRequest.Builder} to create a
     *        request.
     * @return Result of the DeleteMessageBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteMessageBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteMessageBatch" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteMessageBatchResponse deleteMessageBatch(Consumer<DeleteMessageBatchRequest.Builder> deleteMessageBatchRequest)
            throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException,
            InvalidBatchEntryIdException, AwsServiceException, SdkClientException, SqsException {
        return deleteMessageBatch(DeleteMessageBatchRequest.builder().applyMutation(deleteMessageBatchRequest).build());
    }

    /**
     * <p>
     * Deletes the queue specified by the <code>QueueUrl</code>, regardless of the queue's contents. If the specified
     * queue doesn't exist, Amazon SQS returns a successful response.
     * </p>
     * <important>
     * <p>
     * Be careful with the <code>DeleteQueue</code> action: When you delete a queue, any messages in the queue are no
     * longer available.
     * </p>
     * </important>
     * <p>
     * When you delete a queue, the deletion process takes up to 60 seconds. Requests you send involving that queue
     * during the 60 seconds might succeed. For example, a <code> <a>SendMessage</a> </code> request might succeed, but
     * after 60 seconds the queue and the message you sent no longer exist.
     * </p>
     * <p>
     * When you delete a queue, you must wait at least 60 seconds before creating a queue with the same name.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param deleteQueueRequest
     * @return Result of the DeleteQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteQueueResponse deleteQueue(DeleteQueueRequest deleteQueueRequest) throws AwsServiceException,
            SdkClientException, SqsException {
        return this.sqsClient.deleteQueue(deleteQueueRequest);
    }

    /**
     * <p>
     * Deletes the queue specified by the <code>QueueUrl</code>, regardless of the queue's contents. If the specified
     * queue doesn't exist, Amazon SQS returns a successful response.
     * </p>
     * <important>
     * <p>
     * Be careful with the <code>DeleteQueue</code> action: When you delete a queue, any messages in the queue are no
     * longer available.
     * </p>
     * </important>
     * <p>
     * When you delete a queue, the deletion process takes up to 60 seconds. Requests you send involving that queue
     * during the 60 seconds might succeed. For example, a <code> <a>SendMessage</a> </code> request might succeed, but
     * after 60 seconds the queue and the message you sent no longer exist.
     * </p>
     * <p>
     * When you delete a queue, you must wait at least 60 seconds before creating a queue with the same name.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteQueueRequest.Builder} avoiding the need to
     * create one manually via {@link DeleteQueueRequest#builder()}
     * </p>
     *
     * @param deleteQueueRequest
     *        A {@link Consumer} that will call methods on {@link DeleteQueueRequest.Builder} to create a request.
     * @return Result of the DeleteQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteQueueResponse deleteQueue(Consumer<DeleteQueueRequest.Builder> deleteQueueRequest) throws AwsServiceException,
            SdkClientException, SqsException {
        return deleteQueue(DeleteQueueRequest.builder().applyMutation(deleteQueueRequest).build());
    }

    /**
     * <p>
     * Gets attributes for the specified queue.
     * </p>
     * <note>
     * <p>
     * To determine whether a queue is <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html">FIFO</a>, you
     * can check whether <code>QueueName</code> ends with the <code>.fifo</code> suffix.
     * </p>
     * </note>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     *
     * @param getQueueAttributesRequest
     * @return Result of the GetQueueAttributes operation returned by the service.
     * @throws InvalidAttributeNameException
     *         The specified attribute doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.GetQueueAttributes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/GetQueueAttributes" target="_top">AWS API
     *      Documentation</a>
     */
    public GetQueueAttributesResponse getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
            throws InvalidAttributeNameException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.getQueueAttributes(getQueueAttributesRequest);
    }

    /**
     * <p>
     * Gets attributes for the specified queue.
     * </p>
     * <note>
     * <p>
     * To determine whether a queue is <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html">FIFO</a>, you
     * can check whether <code>QueueName</code> ends with the <code>.fifo</code> suffix.
     * </p>
     * </note>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetQueueAttributesRequest.Builder} avoiding the
     * need to create one manually via {@link GetQueueAttributesRequest#builder()}
     * </p>
     *
     * @param getQueueAttributesRequest
     *        A {@link Consumer} that will call methods on {@link GetQueueAttributesRequest.Builder} to create a
     *        request.
     * @return Result of the GetQueueAttributes operation returned by the service.
     * @throws InvalidAttributeNameException
     *         The specified attribute doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.GetQueueAttributes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/GetQueueAttributes" target="_top">AWS API
     *      Documentation</a>
     */
    public GetQueueAttributesResponse getQueueAttributes(Consumer<GetQueueAttributesRequest.Builder> getQueueAttributesRequest)
            throws InvalidAttributeNameException, AwsServiceException, SdkClientException, SqsException {
        return getQueueAttributes(GetQueueAttributesRequest.builder().applyMutation(getQueueAttributesRequest).build());
    }

    /**
     * <p>
     * Returns the URL of an existing Amazon SQS queue.
     * </p>
     * <p>
     * To access a queue that belongs to another AWS account, use the <code>QueueOwnerAWSAccountId</code> parameter to
     * specify the account ID of the queue's owner. The queue's owner must grant you permission to access the queue. For
     * more information about shared queue access, see <code> <a>AddPermission</a> </code> or see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html#write-messages-to-shared-queue"
     * >Allow Developers to Write Messages to a Shared Queue</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     *
     * @param getQueueUrlRequest
     * @return Result of the GetQueueUrl operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.GetQueueUrl
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/GetQueueUrl" target="_top">AWS API
     *      Documentation</a>
     */
    public GetQueueUrlResponse getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) throws QueueDoesNotExistException,
            AwsServiceException, SdkClientException, SqsException {
        return this.getQueueUrl(getQueueUrlRequest);
    }

    /**
     * <p>
     * Returns the URL of an existing Amazon SQS queue.
     * </p>
     * <p>
     * To access a queue that belongs to another AWS account, use the <code>QueueOwnerAWSAccountId</code> parameter to
     * specify the account ID of the queue's owner. The queue's owner must grant you permission to access the queue. For
     * more information about shared queue access, see <code> <a>AddPermission</a> </code> or see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html#write-messages-to-shared-queue"
     * >Allow Developers to Write Messages to a Shared Queue</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetQueueUrlRequest.Builder} avoiding the need to
     * create one manually via {@link GetQueueUrlRequest#builder()}
     * </p>
     *
     * @param getQueueUrlRequest
     *        A {@link Consumer} that will call methods on {@link GetQueueUrlRequest.Builder} to create a request.
     * @return Result of the GetQueueUrl operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.GetQueueUrl
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/GetQueueUrl" target="_top">AWS API
     *      Documentation</a>
     */
    public GetQueueUrlResponse getQueueUrl(Consumer<GetQueueUrlRequest.Builder> getQueueUrlRequest)
            throws QueueDoesNotExistException, AwsServiceException, SdkClientException, SqsException {
        return getQueueUrl(GetQueueUrlRequest.builder().applyMutation(getQueueUrlRequest).build());
    }

    /**
     * <p>
     * Returns a list of your queues that have the <code>RedrivePolicy</code> queue attribute configured with a
     * dead-letter queue.
     * </p>
     * <p>
     * For more information about using dead-letter queues, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html"
     * >Using Amazon SQS Dead-Letter Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     *
     * @param listDeadLetterSourceQueuesRequest
     * @return Result of the ListDeadLetterSourceQueues operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListDeadLetterSourceQueues
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListDeadLetterSourceQueues" target="_top">AWS
     *      API Documentation</a>
     */
    public ListDeadLetterSourceQueuesResponse listDeadLetterSourceQueues(
            ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) throws QueueDoesNotExistException,
            AwsServiceException, SdkClientException, SqsException {
        return this.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    /**
     * <p>
     * Returns a list of your queues that have the <code>RedrivePolicy</code> queue attribute configured with a
     * dead-letter queue.
     * </p>
     * <p>
     * For more information about using dead-letter queues, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html"
     * >Using Amazon SQS Dead-Letter Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListDeadLetterSourceQueuesRequest.Builder} avoiding
     * the need to create one manually via {@link ListDeadLetterSourceQueuesRequest#builder()}
     * </p>
     *
     * @param listDeadLetterSourceQueuesRequest
     *        A {@link Consumer} that will call methods on {@link ListDeadLetterSourceQueuesRequest.Builder} to create a
     *        request.
     * @return Result of the ListDeadLetterSourceQueues operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListDeadLetterSourceQueues
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListDeadLetterSourceQueues" target="_top">AWS
     *      API Documentation</a>
     */
    public ListDeadLetterSourceQueuesResponse listDeadLetterSourceQueues(
            Consumer<ListDeadLetterSourceQueuesRequest.Builder> listDeadLetterSourceQueuesRequest)
            throws QueueDoesNotExistException, AwsServiceException, SdkClientException, SqsException {
        return listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest.builder()
                .applyMutation(listDeadLetterSourceQueuesRequest).build());
    }

    /**
     * <p>
     * List all cost allocation tags added to the specified Amazon SQS queue. For an overview, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-tags.html">Tagging
     * Your Amazon SQS Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param listQueueTagsRequest
     * @return Result of the ListQueueTags operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueueTags
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueueTags" target="_top">AWS API
     *      Documentation</a>
     */
    public ListQueueTagsResponse listQueueTags(ListQueueTagsRequest listQueueTagsRequest) throws AwsServiceException,
            SdkClientException, SqsException {
        return this.sqsClient.listQueueTags(listQueueTagsRequest);
    }

    /**
     * <p>
     * List all cost allocation tags added to the specified Amazon SQS queue. For an overview, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-tags.html">Tagging
     * Your Amazon SQS Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListQueueTagsRequest.Builder} avoiding the need to
     * create one manually via {@link ListQueueTagsRequest#builder()}
     * </p>
     *
     * @param listQueueTagsRequest
     *        A {@link Consumer} that will call methods on {@link ListQueueTagsRequest.Builder} to create a request.
     * @return Result of the ListQueueTags operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueueTags
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueueTags" target="_top">AWS API
     *      Documentation</a>
     */
    public ListQueueTagsResponse listQueueTags(Consumer<ListQueueTagsRequest.Builder> listQueueTagsRequest)
            throws AwsServiceException, SdkClientException, SqsException {
        return listQueueTags(ListQueueTagsRequest.builder().applyMutation(listQueueTagsRequest).build());
    }

    /**
     * <p>
     * Returns a list of your queues. The maximum number of queues that can be returned is 1,000. If you specify a value
     * for the optional <code>QueueNamePrefix</code> parameter, only queues with a name that begins with the specified
     * value are returned.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @return Result of the ListQueues operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueues
     * @see #listQueues(ListQueuesRequest)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueues" target="_top">AWS API
     *      Documentation</a>
     */
    public ListQueuesResponse listQueues() throws AwsServiceException, SdkClientException, SqsException {
        return listQueues(ListQueuesRequest.builder().build());
    }

    /**
     * <p>
     * Returns a list of your queues. The maximum number of queues that can be returned is 1,000. If you specify a value
     * for the optional <code>QueueNamePrefix</code> parameter, only queues with a name that begins with the specified
     * value are returned.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param listQueuesRequest
     * @return Result of the ListQueues operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueues
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueues" target="_top">AWS API
     *      Documentation</a>
     */
    public ListQueuesResponse listQueues(ListQueuesRequest listQueuesRequest) throws AwsServiceException, SdkClientException,
            SqsException {
        return this.sqsClient.listQueues(listQueuesRequest);
    }

    /**
     * <p>
     * Returns a list of your queues. The maximum number of queues that can be returned is 1,000. If you specify a value
     * for the optional <code>QueueNamePrefix</code> parameter, only queues with a name that begins with the specified
     * value are returned.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListQueuesRequest.Builder} avoiding the need to
     * create one manually via {@link ListQueuesRequest#builder()}
     * </p>
     *
     * @param listQueuesRequest
     *        A {@link Consumer} that will call methods on {@link ListQueuesRequest.Builder} to create a request.
     * @return Result of the ListQueues operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueues
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueues" target="_top">AWS API
     *      Documentation</a>
     */
    public ListQueuesResponse listQueues(Consumer<ListQueuesRequest.Builder> listQueuesRequest) throws AwsServiceException,
            SdkClientException, SqsException {
        return listQueues(ListQueuesRequest.builder().applyMutation(listQueuesRequest).build());
    }

    /**
     * <p>
     * Deletes the messages in a queue specified by the <code>QueueURL</code> parameter.
     * </p>
     * <important>
     * <p>
     * When you use the <code>PurgeQueue</code> action, you can't retrieve any messages deleted from a queue.
     * </p>
     * <p>
     * The message deletion process takes up to 60 seconds. We recommend waiting for 60 seconds regardless of your
     * queue's size.
     * </p>
     * </important>
     * <p>
     * Messages sent to the queue <i>before</i> you call <code>PurgeQueue</code> might be received but are deleted
     * within the next minute.
     * </p>
     * <p>
     * Messages sent to the queue <i>after</i> you call <code>PurgeQueue</code> might be deleted while the queue is
     * being purged.
     * </p>
     *
     * @param purgeQueueRequest
     * @return Result of the PurgeQueue operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws PurgeQueueInProgressException
     *         Indicates that the specified queue previously received a <code>PurgeQueue</code> request within the last
     *         60 seconds (the time it can take to delete the messages in the queue).
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.PurgeQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/PurgeQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public PurgeQueueResponse purgeQueue(PurgeQueueRequest purgeQueueRequest) throws QueueDoesNotExistException,
            PurgeQueueInProgressException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.purgeQueue(purgeQueueRequest);
    }

    /**
     * <p>
     * Deletes the messages in a queue specified by the <code>QueueURL</code> parameter.
     * </p>
     * <important>
     * <p>
     * When you use the <code>PurgeQueue</code> action, you can't retrieve any messages deleted from a queue.
     * </p>
     * <p>
     * The message deletion process takes up to 60 seconds. We recommend waiting for 60 seconds regardless of your
     * queue's size.
     * </p>
     * </important>
     * <p>
     * Messages sent to the queue <i>before</i> you call <code>PurgeQueue</code> might be received but are deleted
     * within the next minute.
     * </p>
     * <p>
     * Messages sent to the queue <i>after</i> you call <code>PurgeQueue</code> might be deleted while the queue is
     * being purged.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PurgeQueueRequest.Builder} avoiding the need to
     * create one manually via {@link PurgeQueueRequest#builder()}
     * </p>
     *
     * @param purgeQueueRequest
     *        A {@link Consumer} that will call methods on {@link PurgeQueueRequest.Builder} to create a request.
     * @return Result of the PurgeQueue operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws PurgeQueueInProgressException
     *         Indicates that the specified queue previously received a <code>PurgeQueue</code> request within the last
     *         60 seconds (the time it can take to delete the messages in the queue).
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.PurgeQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/PurgeQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public PurgeQueueResponse purgeQueue(Consumer<PurgeQueueRequest.Builder> purgeQueueRequest)
            throws QueueDoesNotExistException, PurgeQueueInProgressException, AwsServiceException, SdkClientException,
            SqsException {
        return purgeQueue(PurgeQueueRequest.builder().applyMutation(purgeQueueRequest).build());
    }

    /**
     * <p>
     * Retrieves one or more messages (up to 10), from the specified queue. Using the <code>WaitTimeSeconds</code>
     * parameter enables long-poll support. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html">Amazon
     * SQS Long Polling</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * Short poll is the default behavior where a weighted random set of machines is sampled on a
     * <code>ReceiveMessage</code> call. Thus, only the messages on the sampled machines are returned. If the number of
     * messages in the queue is small (fewer than 1,000), you most likely get fewer messages than you requested per
     * <code>ReceiveMessage</code> call. If the number of messages in the queue is extremely small, you might not
     * receive any messages in a particular <code>ReceiveMessage</code> response. If this happens, repeat the request.
     * </p>
     * <p>
     * For each message returned, the response includes the following:
     * </p>
     * <ul>
     * <li>
     * <p>
     * The message body.
     * </p>
     * </li>
     * <li>
     * <p>
     * An MD5 digest of the message body. For information about MD5, see <a
     * href="https://www.ietf.org/rfc/rfc1321.txt">RFC1321</a>.
     * </p>
     * </li>
     * <li>
     * <p>
     * The <code>MessageId</code> you received when you sent the message to the queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * The receipt handle.
     * </p>
     * </li>
     * <li>
     * <p>
     * The message attributes.
     * </p>
     * </li>
     * <li>
     * <p>
     * An MD5 digest of the message attributes.
     * </p>
     * </li>
     * </ul>
     * <p>
     * The receipt handle is the identifier you must provide when deleting the message. For more information, see <a
     * href
     * ="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-message-identifiers.html"
     * >Queue and Message Identifiers</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * You can provide the <code>VisibilityTimeout</code> parameter in your request. The parameter is applied to the
     * messages that Amazon SQS returns in the response. If you don't include the parameter, the overall visibility
     * timeout for the queue is used for the returned messages. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html"
     * >Visibility Timeout</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * A message that isn't deleted or a message whose visibility isn't extended before the visibility timeout expires
     * counts as a failed receive. Depending on the configuration of the queue, the message might be sent to the
     * dead-letter queue.
     * </p>
     * <note>
     * <p>
     * In the future, new attributes might be added. If you write code that calls this action, we recommend that you
     * structure your code so that it can handle new attributes gracefully.
     * </p>
     * </note>
     *
     * @param receiveMessageRequest
     * @return Result of the ReceiveMessage operation returned by the service.
     * @throws OverLimitException
     *         The specified action violates a limit. For example, <code>ReceiveMessage</code> returns this error if the
     *         maximum number of inflight messages is reached and <code>AddPermission</code> returns this error if the
     *         maximum number of permissions for the queue is reached.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ReceiveMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ReceiveMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) throws AwsServiceException, SdkClientException {
        if (receiveMessageRequest == null) {
            String errorMessage = "receiveMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return this.sqsClient.receiveMessage(receiveMessageRequest);
        }

        ReceiveMessageRequest.Builder builder = receiveMessageRequest.toBuilder();
        if (!receiveMessageRequest.messageAttributeNames().contains(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME)) {
            ArrayList<String> messageAttributeNames = new ArrayList<>(receiveMessageRequest.messageAttributeNames());
            messageAttributeNames.add(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
            builder.messageAttributeNames(messageAttributeNames);
        }

        ReceiveMessageResponse receiveMessageResponse = this.sqsClient.receiveMessage(builder.build());
        ReceiveMessageResponse.Builder responseBuilder = receiveMessageResponse.toBuilder();

        List<Message> messages = receiveMessageResponse.messages();
        List<Message> alteredMessages = new ArrayList<>();

        for (Message message : messages) {

            // for each received message check if they are stored in S3.
            MessageAttributeValue largePayloadAttributeValue = message.messageAttributes().get(
                    SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
            if (largePayloadAttributeValue != null) {
                String messageBody = message.body();
                MessageS3Pointer s3Pointer = readMessageS3PointerFromJSON(messageBody);
                String textFromS3 = getTextFromS3(s3Pointer.getS3BucketName(), s3Pointer.getS3Key());
                LOG.info("S3 object read, Bucket name: " + s3Pointer.getS3BucketName() + ", Object key: " + s3Pointer.getS3Key() + ".");

                Message.Builder messageBuilder = message.toBuilder();
                messageBuilder.body(textFromS3);

                // remove the additional attribute before returning the message
                // to user.
                HashMap<String, MessageAttributeValue> stringMessageAttributeValueHashMap = new HashMap<>(message.messageAttributes());
                stringMessageAttributeValueHashMap.remove(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
                messageBuilder.messageAttributes(stringMessageAttributeValueHashMap);

                // Embed s3 object pointer in the receipt handle.
                String modifiedReceiptHandle = embedS3PointerInReceiptHandle(message.receiptHandle(),
                        s3Pointer.getS3BucketName(), s3Pointer.getS3Key());

                messageBuilder.receiptHandle(modifiedReceiptHandle);
                alteredMessages.add(messageBuilder.build());
            } else {
                alteredMessages.add(message);
            }
        }
        return responseBuilder.messages(alteredMessages)
                .build();
    }

    /**
     * <p>
     * Retrieves one or more messages (up to 10), from the specified queue. Using the <code>WaitTimeSeconds</code>
     * parameter enables long-poll support. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html">Amazon
     * SQS Long Polling</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * Short poll is the default behavior where a weighted random set of machines is sampled on a
     * <code>ReceiveMessage</code> call. Thus, only the messages on the sampled machines are returned. If the number of
     * messages in the queue is small (fewer than 1,000), you most likely get fewer messages than you requested per
     * <code>ReceiveMessage</code> call. If the number of messages in the queue is extremely small, you might not
     * receive any messages in a particular <code>ReceiveMessage</code> response. If this happens, repeat the request.
     * </p>
     * <p>
     * For each message returned, the response includes the following:
     * </p>
     * <ul>
     * <li>
     * <p>
     * The message body.
     * </p>
     * </li>
     * <li>
     * <p>
     * An MD5 digest of the message body. For information about MD5, see <a
     * href="https://www.ietf.org/rfc/rfc1321.txt">RFC1321</a>.
     * </p>
     * </li>
     * <li>
     * <p>
     * The <code>MessageId</code> you received when you sent the message to the queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * The receipt handle.
     * </p>
     * </li>
     * <li>
     * <p>
     * The message attributes.
     * </p>
     * </li>
     * <li>
     * <p>
     * An MD5 digest of the message attributes.
     * </p>
     * </li>
     * </ul>
     * <p>
     * The receipt handle is the identifier you must provide when deleting the message. For more information, see <a
     * href
     * ="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-message-identifiers.html"
     * >Queue and Message Identifiers</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * You can provide the <code>VisibilityTimeout</code> parameter in your request. The parameter is applied to the
     * messages that Amazon SQS returns in the response. If you don't include the parameter, the overall visibility
     * timeout for the queue is used for the returned messages. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html"
     * >Visibility Timeout</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * A message that isn't deleted or a message whose visibility isn't extended before the visibility timeout expires
     * counts as a failed receive. Depending on the configuration of the queue, the message might be sent to the
     * dead-letter queue.
     * </p>
     * <note>
     * <p>
     * In the future, new attributes might be added. If you write code that calls this action, we recommend that you
     * structure your code so that it can handle new attributes gracefully.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ReceiveMessageRequest.Builder} avoiding the need to
     * create one manually via {@link ReceiveMessageRequest#builder()}
     * </p>
     *
     * @param receiveMessageRequest
     *        A {@link Consumer} that will call methods on {@link ReceiveMessageRequest.Builder} to create a request.
     * @return Result of the ReceiveMessage operation returned by the service.
     * @throws OverLimitException
     *         The specified action violates a limit. For example, <code>ReceiveMessage</code> returns this error if the
     *         maximum number of inflight messages is reached and <code>AddPermission</code> returns this error if the
     *         maximum number of permissions for the queue is reached.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ReceiveMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ReceiveMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public ReceiveMessageResponse receiveMessage(Consumer<ReceiveMessageRequest.Builder> receiveMessageRequest)
            throws OverLimitException, AwsServiceException, SdkClientException, SqsException {
        return receiveMessage(ReceiveMessageRequest.builder().applyMutation(receiveMessageRequest).build());
    }

    /**
     * <p>
     * Revokes any permissions in the queue policy that matches the specified <code>Label</code> parameter.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * Only the owner of a queue can remove permissions from it.
     * </p>
     * </li>
     * <li>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
     *
     * @param removePermissionRequest
     * @return Result of the RemovePermission operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.RemovePermission
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/RemovePermission" target="_top">AWS API
     *      Documentation</a>
     */
    public RemovePermissionResponse removePermission(RemovePermissionRequest removePermissionRequest)
            throws AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.removePermission(removePermissionRequest);
    }

    /**
     * <p>
     * Revokes any permissions in the queue policy that matches the specified <code>Label</code> parameter.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * Only the owner of a queue can remove permissions from it.
     * </p>
     * </li>
     * <li>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link RemovePermissionRequest.Builder} avoiding the need
     * to create one manually via {@link RemovePermissionRequest#builder()}
     * </p>
     *
     * @param removePermissionRequest
     *        A {@link Consumer} that will call methods on {@link RemovePermissionRequest.Builder} to create a request.
     * @return Result of the RemovePermission operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.RemovePermission
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/RemovePermission" target="_top">AWS API
     *      Documentation</a>
     */
    public RemovePermissionResponse removePermission(Consumer<RemovePermissionRequest.Builder> removePermissionRequest)
            throws AwsServiceException, SdkClientException, SqsException {
        return removePermission(RemovePermissionRequest.builder().applyMutation(removePermissionRequest).build());
    }

    /**
     * <p>
     * Delivers a message to the specified queue.
     * </p>
     * <important>
     * <p>
     * A message can include only XML, JSON, and unformatted text. The following Unicode characters are allowed:
     * </p>
     * <p>
     * <code>#x9</code> | <code>#xA</code> | <code>#xD</code> | <code>#x20</code> to <code>#xD7FF</code> |
     * <code>#xE000</code> to <code>#xFFFD</code> | <code>#x10000</code> to <code>#x10FFFF</code>
     * </p>
     * <p>
     * Any characters not included in this list will be rejected. For more information, see the <a
     * href="http://www.w3.org/TR/REC-xml/#charsets">W3C specification for characters</a>.
     * </p>
     * </important>
     *
     * @param sendMessageRequest
     * @return Result of the SendMessage operation returned by the service.
     * @throws InvalidMessageContentsException The message contains characters outside the allowed set.
     * @throws UnsupportedOperationException   Error code 400. Unsupported operation.
     * @throws SdkException                    Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *                                         catch all scenarios.
     * @throws SdkClientException              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException                    Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SendMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SendMessage" target="_top">AWS API
     * Documentation</a>
     */
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

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return this.sqsClient.sendMessage(sendMessageRequest);
        }

        if (clientConfiguration.isAlwaysThroughS3() || isLarge(sendMessageRequest)) {
            sendMessageRequest = storeMessageInS3(sendMessageRequest);
        }

        return this.sqsClient.sendMessage(sendMessageRequest);
    }

    /**
     * <p>
     * Delivers a message to the specified queue.
     * </p>
     * <important>
     * <p>
     * A message can include only XML, JSON, and unformatted text. The following Unicode characters are allowed:
     * </p>
     * <p>
     * <code>#x9</code> | <code>#xA</code> | <code>#xD</code> | <code>#x20</code> to <code>#xD7FF</code> |
     * <code>#xE000</code> to <code>#xFFFD</code> | <code>#x10000</code> to <code>#x10FFFF</code>
     * </p>
     * <p>
     * Any characters not included in this list will be rejected. For more information, see the <a
     * href="http://www.w3.org/TR/REC-xml/#charsets">W3C specification for characters</a>.
     * </p>
     * </important><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SendMessageRequest.Builder} avoiding the need to
     * create one manually via {@link SendMessageRequest#builder()}
     * </p>
     *
     * @param sendMessageRequest A {@link Consumer} that will call methods on {@link SendMessageRequest.Builder} to create a request.
     * @return Result of the SendMessage operation returned by the service.
     * @throws InvalidMessageContentsException The message contains characters outside the allowed set.
     * @throws UnsupportedOperationException   Error code 400. Unsupported operation.
     * @throws SdkException                    Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *                                         catch all scenarios.
     * @throws SdkClientException              If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException                    Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SendMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SendMessage" target="_top">AWS API
     * Documentation</a>
     */
    public SendMessageResponse sendMessage(Consumer<SendMessageRequest.Builder> sendMessageRequest) throws AwsServiceException, SdkClientException {
        return sendMessage(SendMessageRequest.builder().applyMutation(sendMessageRequest).build());
    }


    /**
     * <p>
     * Delivers up to ten messages to the specified queue. This is a batch version of <code> <a>SendMessage</a>.</code>
     * For a FIFO queue, multiple messages within a single batch are enqueued in the order they are sent.
     * </p>
     * <p>
     * The result of sending each message is reported individually in the response. Because the batch request can result
     * in a combination of successful and unsuccessful actions, you should check for batch errors even when the call
     * returns an HTTP status code of <code>200</code>.
     * </p>
     * <p>
     * The maximum allowed individual message size and the maximum total payload size (the sum of the individual lengths
     * of all of the batched messages) are both 256 KB (262,144 bytes).
     * </p>
     * <important>
     * <p>
     * A message can include only XML, JSON, and unformatted text. The following Unicode characters are allowed:
     * </p>
     * <p>
     * <code>#x9</code> | <code>#xA</code> | <code>#xD</code> | <code>#x20</code> to <code>#xD7FF</code> |
     * <code>#xE000</code> to <code>#xFFFD</code> | <code>#x10000</code> to <code>#x10FFFF</code>
     * </p>
     * <p>
     * Any characters not included in this list will be rejected. For more information, see the <a
     * href="http://www.w3.org/TR/REC-xml/#charsets">W3C specification for characters</a>.
     * </p>
     * </important>
     * <p>
     * If you don't specify the <code>DelaySeconds</code> parameter for an entry, Amazon SQS uses the default value for
     * the queue.
     * </p>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     *
     * @param sendMessageBatchRequest
     * @return Result of the SendMessageBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws BatchRequestTooLongException
     *         The length of all the messages put together is more than the limit.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws UnsupportedOperationException
     *         Error code 400. Unsupported operation.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SendMessageBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SendMessageBatch" target="_top">AWS API
     *      Documentation</a>
     */
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest)
            throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException,
            BatchRequestTooLongException, InvalidBatchEntryIdException,
            software.amazon.awssdk.services.sqs.model.UnsupportedOperationException, AwsServiceException, SdkClientException,
            SqsException {
        return this.sqsClient.sendMessageBatch(sendMessageBatchRequest);
    }

    /**
     * <p>
     * Delivers up to ten messages to the specified queue. This is a batch version of <code> <a>SendMessage</a>.</code>
     * For a FIFO queue, multiple messages within a single batch are enqueued in the order they are sent.
     * </p>
     * <p>
     * The result of sending each message is reported individually in the response. Because the batch request can result
     * in a combination of successful and unsuccessful actions, you should check for batch errors even when the call
     * returns an HTTP status code of <code>200</code>.
     * </p>
     * <p>
     * The maximum allowed individual message size and the maximum total payload size (the sum of the individual lengths
     * of all of the batched messages) are both 256 KB (262,144 bytes).
     * </p>
     * <important>
     * <p>
     * A message can include only XML, JSON, and unformatted text. The following Unicode characters are allowed:
     * </p>
     * <p>
     * <code>#x9</code> | <code>#xA</code> | <code>#xD</code> | <code>#x20</code> to <code>#xD7FF</code> |
     * <code>#xE000</code> to <code>#xFFFD</code> | <code>#x10000</code> to <code>#x10FFFF</code>
     * </p>
     * <p>
     * Any characters not included in this list will be rejected. For more information, see the <a
     * href="http://www.w3.org/TR/REC-xml/#charsets">W3C specification for characters</a>.
     * </p>
     * </important>
     * <p>
     * If you don't specify the <code>DelaySeconds</code> parameter for an entry, Amazon SQS uses the default value for
     * the queue.
     * </p>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;Attribute.1=first</code>
     * </p>
     * <p>
     * <code>&amp;Attribute.2=second</code>
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SendMessageBatchRequest.Builder} avoiding the need
     * to create one manually via {@link SendMessageBatchRequest#builder()}
     * </p>
     *
     * @param sendMessageBatchRequest
     *        A {@link Consumer} that will call methods on {@link SendMessageBatchRequest.Builder} to create a request.
     * @return Result of the SendMessageBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws BatchRequestTooLongException
     *         The length of all the messages put together is more than the limit.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws UnsupportedOperationException
     *         Error code 400. Unsupported operation.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SendMessageBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SendMessageBatch" target="_top">AWS API
     *      Documentation</a>
     */
    public SendMessageBatchResponse sendMessageBatch(Consumer<SendMessageBatchRequest.Builder> sendMessageBatchRequest)
            throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException,
            BatchRequestTooLongException, InvalidBatchEntryIdException,
            software.amazon.awssdk.services.sqs.model.UnsupportedOperationException, AwsServiceException, SdkClientException,
            SqsException {
        return sendMessageBatch(SendMessageBatchRequest.builder().applyMutation(sendMessageBatchRequest).build());
    }

    /**
     * <p>
     * Sets the value of one or more queue attributes. When you change a queue's attributes, the change can take up to
     * 60 seconds for most of the attributes to propagate throughout the Amazon SQS system. Changes made to the
     * <code>MessageRetentionPeriod</code> attribute can take up to 15 minutes.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * In the future, new attributes might be added. If you write code that calls this action, we recommend that you
     * structure your code so that it can handle new attributes gracefully.
     * </p>
     * </li>
     * <li>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
     *
     * @param setQueueAttributesRequest
     * @return Result of the SetQueueAttributes operation returned by the service.
     * @throws InvalidAttributeNameException
     *         The specified attribute doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SetQueueAttributes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SetQueueAttributes" target="_top">AWS API
     *      Documentation</a>
     */
    public SetQueueAttributesResponse setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest)
            throws InvalidAttributeNameException, AwsServiceException, SdkClientException, SqsException {
        return this.sqsClient.setQueueAttributes(setQueueAttributesRequest);
    }

    /**
     * <p>
     * Sets the value of one or more queue attributes. When you change a queue's attributes, the change can take up to
     * 60 seconds for most of the attributes to propagate throughout the Amazon SQS system. Changes made to the
     * <code>MessageRetentionPeriod</code> attribute can take up to 15 minutes.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * In the future, new attributes might be added. If you write code that calls this action, we recommend that you
     * structure your code so that it can handle new attributes gracefully.
     * </p>
     * </li>
     * <li>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SetQueueAttributesRequest.Builder} avoiding the
     * need to create one manually via {@link SetQueueAttributesRequest#builder()}
     * </p>
     *
     * @param setQueueAttributesRequest
     *        A {@link Consumer} that will call methods on {@link SetQueueAttributesRequest.Builder} to create a
     *        request.
     * @return Result of the SetQueueAttributes operation returned by the service.
     * @throws InvalidAttributeNameException
     *         The specified attribute doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SetQueueAttributes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SetQueueAttributes" target="_top">AWS API
     *      Documentation</a>
     */
    public SetQueueAttributesResponse setQueueAttributes(Consumer<SetQueueAttributesRequest.Builder> setQueueAttributesRequest)
            throws InvalidAttributeNameException, AwsServiceException, SdkClientException, SqsException {
        return setQueueAttributes(SetQueueAttributesRequest.builder().applyMutation(setQueueAttributesRequest).build());
    }

    /**
     * <p>
     * Add cost allocation tags to the specified Amazon SQS queue. For an overview, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-tags.html">Tagging
     * Your Amazon SQS Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * When you use queue tags, keep the following guidelines in mind:
     * </p>
     * <ul>
     * <li>
     * <p>
     * Adding more than 50 tags to a queue isn't recommended.
     * </p>
     * </li>
     * <li>
     * <p>
     * Tags don't have any semantic meaning. Amazon SQS interprets tags as character strings.
     * </p>
     * </li>
     * <li>
     * <p>
     * Tags are case-sensitive.
     * </p>
     * </li>
     * <li>
     * <p>
     * A new tag with a key identical to that of an existing tag overwrites the existing tag.
     * </p>
     * </li>
     * </ul>
     * <p>
     * For a full list of tag restrictions, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-limits.html#limits-queues"
     * >Limits Related to Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param tagQueueRequest
     * @return Result of the TagQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.TagQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/TagQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public TagQueueResponse tagQueue(TagQueueRequest tagQueueRequest) throws AwsServiceException, SdkClientException,
            SqsException {
        return this.sqsClient.tagQueue(tagQueueRequest);
    }

    /**
     * <p>
     * Add cost allocation tags to the specified Amazon SQS queue. For an overview, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-tags.html">Tagging
     * Your Amazon SQS Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * When you use queue tags, keep the following guidelines in mind:
     * </p>
     * <ul>
     * <li>
     * <p>
     * Adding more than 50 tags to a queue isn't recommended.
     * </p>
     * </li>
     * <li>
     * <p>
     * Tags don't have any semantic meaning. Amazon SQS interprets tags as character strings.
     * </p>
     * </li>
     * <li>
     * <p>
     * Tags are case-sensitive.
     * </p>
     * </li>
     * <li>
     * <p>
     * A new tag with a key identical to that of an existing tag overwrites the existing tag.
     * </p>
     * </li>
     * </ul>
     * <p>
     * For a full list of tag restrictions, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-limits.html#limits-queues"
     * >Limits Related to Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link TagQueueRequest.Builder} avoiding the need to
     * create one manually via {@link TagQueueRequest#builder()}
     * </p>
     *
     * @param tagQueueRequest
     *        A {@link Consumer} that will call methods on {@link TagQueueRequest.Builder} to create a request.
     * @return Result of the TagQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.TagQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/TagQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public TagQueueResponse tagQueue(Consumer<TagQueueRequest.Builder> tagQueueRequest) throws AwsServiceException,
            SdkClientException, SqsException {
        return tagQueue(TagQueueRequest.builder().applyMutation(tagQueueRequest).build());
    }

    /**
     * <p>
     * Remove cost allocation tags from the specified Amazon SQS queue. For an overview, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-tags.html">Tagging
     * Your Amazon SQS Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param untagQueueRequest
     * @return Result of the UntagQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.UntagQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/UntagQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public UntagQueueResponse untagQueue(UntagQueueRequest untagQueueRequest) throws AwsServiceException, SdkClientException,
            SqsException {
        return this.sqsClient.untagQueue(untagQueueRequest);
    }

    /**
     * <p>
     * Remove cost allocation tags from the specified Amazon SQS queue. For an overview, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-tags.html">Tagging
     * Your Amazon SQS Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link UntagQueueRequest.Builder} avoiding the need to
     * create one manually via {@link UntagQueueRequest#builder()}
     * </p>
     *
     * @param untagQueueRequest
     *        A {@link Consumer} that will call methods on {@link UntagQueueRequest.Builder} to create a request.
     * @return Result of the UntagQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.UntagQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/UntagQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public UntagQueueResponse untagQueue(Consumer<UntagQueueRequest.Builder> untagQueueRequest) throws AwsServiceException,
            SdkClientException, SqsException {
        return this.sqsClient.untagQueue(untagQueueRequest);
    }

    private SendMessageRequest storeMessageInS3(SendMessageRequest sendMessageRequest) {
        SendMessageRequest.Builder builder = sendMessageRequest.toBuilder();
//        checkMessageAttributes(sendMessageRequest.messageAttributes());

        String s3Key = UUID.randomUUID().toString();
        String messageContentStr = sendMessageRequest.messageBody();
        Long messageContentSize = getStringSizeInBytes(messageContentStr);

        MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
                .dataType("Number")
                .stringValue(messageContentSize.toString())
                .build();
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>(sendMessageRequest.messageAttributes());
        messageAttributes.put(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, messageAttributeValue);

        storeTextInS3(s3Key, messageContentStr, messageContentSize);
        LOG.info("S3 object created, Bucket name: " + clientConfiguration.getS3BucketName() + ", Object key: " + s3Key
                + ".");

        MessageS3Pointer s3Pointer = new MessageS3Pointer(clientConfiguration.getS3BucketName(), s3Key);
        String s3PointerStr = getJSONFromS3Pointer(s3Pointer);
        return builder.messageBody(s3PointerStr)
                .messageAttributes(messageAttributes)
                .build();
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

    private String embedS3PointerInReceiptHandle(String receiptHandle, String s3MsgBucketName, String s3MsgKey) {
        String modifiedReceiptHandle = SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + s3MsgBucketName
                + SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + SQSExtendedClientConstants.S3_KEY_MARKER
                + s3MsgKey + SQSExtendedClientConstants.S3_KEY_MARKER + receiptHandle;
        return modifiedReceiptHandle;
    }

    private MessageS3Pointer readMessageS3PointerFromJSON(String messageBody) {
        MessageS3Pointer s3Pointer = null;
        try {
            JsonDataConverter jsonDataConverter = new JsonDataConverter();
            s3Pointer = jsonDataConverter.deserializeFromJson(messageBody, MessageS3Pointer.class);
        } catch (Exception e) {
            String errorMessage = "Failed to read the S3 object pointer from an SQS message. Message was not received.";
            LOG.error(errorMessage, e);
            throw SdkClientException.create(errorMessage, e);
        }
        return s3Pointer;
    }

    private String getTextFromS3(String s3BucketName, String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3BucketName)
                .key(s3Key)
                .build();
        String embeddedText = null;
        ResponseBytes<GetObjectResponse> object = null;
        try {
            object = clientConfiguration.getAmazonS3Client().getObject(getObjectRequest, ResponseTransformer.toBytes());
        } catch (SdkException e) {
            String errorMessage = "Failed to get the S3 object which contains the message payload. Message was not received.";
            LOG.error(errorMessage, e);
            throw SdkException.create(errorMessage, e);
        }

        try {
            embeddedText = object.asUtf8String();
        } catch (UncheckedIOException e) {
            String errorMessage = "Failure when handling the message which was read from S3 object. Message was not received.";
            LOG.error(errorMessage, e);
            throw SdkClientException.create(errorMessage, e);
        }

        return embeddedText;
    }

    private boolean isLarge(SendMessageRequest sendMessageRequest) {
        int msgAttributesSize = getMsgAttributesSize(sendMessageRequest.messageAttributes());
        long msgBodySize = getStringSizeInBytes(sendMessageRequest.messageBody());
        long totalMsgSize = msgAttributesSize + msgBodySize;
        return (totalMsgSize > clientConfiguration.getMessageSizeThreshold());
    }

    private int getMsgAttributesSize(Map<String, MessageAttributeValue> msgAttributes) {
        int totalMsgAttributesSize = 0;
        for (Map.Entry<String, MessageAttributeValue> entry : msgAttributes.entrySet()) {
            totalMsgAttributesSize += getStringSizeInBytes(entry.getKey());

            MessageAttributeValue entryVal = entry.getValue();
            if (entryVal.dataType() != null) {
                totalMsgAttributesSize += getStringSizeInBytes(entryVal.dataType());
            }

            String stringVal = entryVal.stringValue();
            if (stringVal != null) {
                totalMsgAttributesSize += getStringSizeInBytes(entryVal.stringValue());
            }

            SdkBytes binaryVal = entryVal.binaryValue();
            if (binaryVal != null) {
                totalMsgAttributesSize += getStringSizeInBytes(binaryVal.toString());
            }
        }
        return totalMsgAttributesSize;
    }

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
                .bucket(this.clientConfiguration.getS3BucketName())
                .key(s3Key)
                .build();
        try {
            amazonS3Client.putObject(putObjectRequest, RequestBody.fromString(messageContentStr));
        } catch (SdkException e) {
            String errorMessage = "Failed to store the message content in an S3 object. SQS message was not sent.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage, e);
        }
    }

    private static long getStringSizeInBytes(String str) {
        CountingOutputStream counterOutputStream = new CountingOutputStream();
        try {
            Writer writer = new OutputStreamWriter(counterOutputStream, StandardCharsets.UTF_8);
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
