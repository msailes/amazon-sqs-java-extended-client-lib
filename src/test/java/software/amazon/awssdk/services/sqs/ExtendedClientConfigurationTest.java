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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * Tests the ExtendedClientConfiguration class.
 */
public class ExtendedClientConfigurationTest {

    private static String s3BucketName = "test-bucket-name";
    private S3Client s3;

    @Before
    public void setup() {
        s3 = mock(S3Client.class);
        when(s3.putObject(isA(PutObjectRequest.class), isA(RequestBody.class))).thenReturn(null);
    }

    @Test
    public void testCopyConstructor() {
        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();

        extendedClientConfig.withLargePayloadSupportEnabled(s3, s3BucketName)
                .withAlwaysThroughS3(alwaysThroughS3).withMessageSizeThreshold(messageSizeThreshold);

        ExtendedClientConfiguration newExtendedClientConfig = new ExtendedClientConfiguration(extendedClientConfig);

        assertEquals(s3, newExtendedClientConfig.getAmazonS3Client());
        assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        assertTrue(newExtendedClientConfig.isLargePayloadSupportEnabled());
        assertTrue(newExtendedClientConfig.isAlwaysThroughS3());
        assertEquals(messageSizeThreshold, newExtendedClientConfig.getMessageSizeThreshold());

        assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabled() {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setLargePayloadSupportEnabled(s3, s3BucketName);

        assertTrue(extendedClientConfiguration.isLargePayloadSupportEnabled());
        assertNotNull(extendedClientConfiguration.getAmazonS3Client());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testDisableLargePayloadSupport() {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setLargePayloadSupportDisabled();

        assertNull(extendedClientConfiguration.getAmazonS3Client());
        assertNull(extendedClientConfiguration.getS3BucketName());

        verify(s3, never()).putObject(isA(PutObjectRequest.class), isA(RequestBody.class));
    }

    @Test
    public void testAlwaysThroughS3() {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        extendedClientConfiguration.setAlwaysThroughS3(true);
        assertTrue(extendedClientConfiguration.isAlwaysThroughS3());

        extendedClientConfiguration.setAlwaysThroughS3(false);
        assertFalse(extendedClientConfiguration.isAlwaysThroughS3());
    }

    @Test
    public void testMessageSizeThreshold() {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        assertEquals(SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD,
                extendedClientConfiguration.getMessageSizeThreshold());

        int messageLength = 1000;
        extendedClientConfiguration.setMessageSizeThreshold(messageLength);
        assertEquals(messageLength, extendedClientConfiguration.getMessageSizeThreshold());
    }
}