/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BucketInventoryTest extends TestBase {
    private String destinBucket;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String destinBucketName = bucketName + "-destin";
        ossClient.createBucket(destinBucketName);
        Thread.sleep(2000);
        destinBucket = destinBucketName;
    }

    private InventoryConfiguration createTestInventoryConfiguration(String configurationId) {
        if (configurationId == null) {
            throw new RuntimeException("inventory configuration id should not be null.");
        }

        // fields
        List<String> fields = new ArrayList<String>();
        fields.add(InventoryOptionalFields.Size);
        fields.add(InventoryOptionalFields.LastModifiedDate);
        fields.add(InventoryOptionalFields.ETag);
        fields.add(InventoryOptionalFields.StorageClass);
        fields.add(InventoryOptionalFields.IsMultipartUploaded);
        fields.add(InventoryOptionalFields.EncryptionStatus);

        // schedule
        InventorySchedule inventorySchedule = new InventorySchedule().withFrequency(InventoryFrequency.Daily);

        // filter
        InventoryFilter inventoryFilter = new InventoryFilter().withPrefix("testPrefix");

        // destination
        InventoryEncryption inventoryEncryption = new InventoryEncryption();
        inventoryEncryption.setServerSideKmsEncryption(new InventoryServerSideEncryptionKMS().withKeyId("123"));
        InventoryOSSBucketDestination ossBucketDestin = new InventoryOSSBucketDestination()
                .withFormat(InventoryFormat.CSV)
                .withPrefix("bucket-prefix")
                .withAccountId(TestConfig.OSS_TEST_INVENTORY_BUCKET_DESTINATION_ACCOUNT)
                .withRoleArn(TestConfig.OSS_TEST_INVENTORY_BUCKET_DESTINATION_ARN)
                .withBucket(destinBucket)
                .withEncryption(inventoryEncryption);

        InventoryDestination destination = new InventoryDestination().withOSSBucketDestination(ossBucketDestin);

        InventoryConfiguration inventoryConfiguration = new InventoryConfiguration()
                .withInventoryId(configurationId)
                .withEnabled(false)
                .withIncludedObjectVersions(InventoryIncludedObjectVersions.Current)
                .withOptionalFields(fields)
                .withFilter(inventoryFilter)
                .withSchedule(inventorySchedule)
                .withDestination(destination);

        return inventoryConfiguration;
    }

    @Test
    public void testBucketInventoryNormal() {
        String inventoryId = "testid";
        // fields
        List<String> fields = new ArrayList<String>();
        fields.add(InventoryOptionalFields.Size);
        fields.add(InventoryOptionalFields.LastModifiedDate);
        fields.add(InventoryOptionalFields.ETag);
        fields.add(InventoryOptionalFields.StorageClass);
        fields.add(InventoryOptionalFields.IsMultipartUploaded);
        fields.add(InventoryOptionalFields.EncryptionStatus);

        // schedule
        InventorySchedule inventorySchedule = new InventorySchedule().withFrequency(InventoryFrequency.Weekly);

        // filter
        InventoryFilter inventoryFilter = new InventoryFilter().withPrefix("testPrefix");

        // destination
        InventoryEncryption inventoryEncryption = new InventoryEncryption();
        inventoryEncryption.setServerSideOssEncryption(new InventoryServerSideEncryptionOSS());
        InventoryOSSBucketDestination ossBucketDestin = new InventoryOSSBucketDestination()
                .withFormat(InventoryFormat.CSV)
                .withPrefix("bucket-prefix")
                .withAccountId(TestConfig.OSS_TEST_INVENTORY_BUCKET_DESTINATION_ACCOUNT)
                .withRoleArn(TestConfig.OSS_TEST_INVENTORY_BUCKET_DESTINATION_ARN)
                .withBucket(destinBucket)
                .withEncryption(inventoryEncryption);

        InventoryDestination destination = new InventoryDestination().withOSSBucketDestination(ossBucketDestin);

        InventoryConfiguration inventoryConfiguration = new InventoryConfiguration()
                .withInventoryId(inventoryId)
                .withEnabled(false)
                .withIncludedObjectVersions(InventoryIncludedObjectVersions.All)
                .withOptionalFields(fields)
                .withFilter(inventoryFilter)
                .withSchedule(inventorySchedule)
                .withDestination(destination);

        // put
        try {
            ossClient.setBucketInventoryConfiguration(bucketName, inventoryConfiguration);
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // get and delete
        try {
            GetBucketInventoryConfigurationResult result = ossClient.getBucketInventoryConfiguration(
                   new GetBucketInventoryConfigurationRequest(bucketName, inventoryId));

            InventoryConfiguration actualConfig = result.getInventoryConfiguration();
            Assert.assertEquals(inventoryId, actualConfig.getInventoryId());
            Assert.assertEquals(InventoryIncludedObjectVersions.All.toString(), actualConfig.getIncludedObjectVersions());
            Assert.assertEquals("testPrefix", actualConfig.getInventoryFilter().getPrefix());
            Assert.assertEquals(InventoryFrequency.Weekly.toString(), actualConfig.getSchedule().getFrequency());
            Assert.assertEquals(6, actualConfig.getOptionalFields().size());

            InventoryOSSBucketDestination actualDestin = actualConfig.getDestination().getOssBucketDestination();

            Assert.assertEquals(TestConfig.OSS_TEST_INVENTORY_BUCKET_DESTINATION_ACCOUNT, actualDestin.getAccountId());
            Assert.assertEquals(destinBucket, actualDestin.getBucket());
            Assert.assertEquals(TestConfig.OSS_TEST_INVENTORY_BUCKET_DESTINATION_ARN, actualDestin.getRoleArn());
            Assert.assertEquals(InventoryFormat.CSV.toString(), actualDestin.getFormat());
            Assert.assertEquals("bucket-prefix", actualDestin.getPrefix());
            Assert.assertNotNull(actualDestin.getEncryption().getServerSideOssEncryption());
            Assert.assertNull(actualDestin.getEncryption().getServerSideKmsEncryption());
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucketInventoryConfiguration(new DeleteBucketInventoryConfigurationRequest(bucketName, inventoryId));
        }
    }

    @Test
    public void testErrorInventoryEncryption() {
        try {
            InventoryEncryption inventoryEncryption = new InventoryEncryption();
            inventoryEncryption.setServerSideOssEncryption(new InventoryServerSideEncryptionOSS());
            inventoryEncryption.setServerSideKmsEncryption(new InventoryServerSideEncryptionKMS().withKeyId("test-kms-id"));
            Assert.fail("The KMS encryption and OSS encryption only can be selected one");
        } catch (ClientException e) {
        }
    }

    @Test
    public void testListFewInventoryConfiguration() {
        String idPrefix = "testid-";
        int sum = 3;
        try {
            for (int i = 0; i < sum; i++) {
                String id = idPrefix + String.valueOf(i);
                InventoryConfiguration inventoryConfiguration = createTestInventoryConfiguration(id);
                ossClient.setBucketInventoryConfiguration(bucketName, inventoryConfiguration);
            }
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            ListBucketInventoryConfigurationsRequest request = new ListBucketInventoryConfigurationsRequest(bucketName);
            ListBucketInventoryConfigurationsResult result = ossClient.listBucketInventoryConfigurations(request);
            Assert.assertEquals(sum, result.getInventoryConfigurationList().size());
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            for (int i = 0; i < sum; i++) {
                String id = idPrefix + String.valueOf(i);
                ossClient.deleteBucketInventoryConfiguration(bucketName, id);
            }
        }
    }

    @Test
    public void testListLotInventoryConfiguration() {
        String idPrefix = "testid-";
        int sum = 102;
        try {
            for (int i = 0; i < sum; i++) {
                String id = idPrefix + String.valueOf(i);
                InventoryConfiguration inventoryConfiguration = createTestInventoryConfiguration(id);
                ossClient.setBucketInventoryConfiguration(bucketName, inventoryConfiguration);
            }
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            int count = 0;
            ListBucketInventoryConfigurationsRequest request = new ListBucketInventoryConfigurationsRequest(bucketName);

            ListBucketInventoryConfigurationsResult result = ossClient.listBucketInventoryConfigurations(request);
            count += result.getInventoryConfigurationList().size();
            Assert.assertEquals(true, result.isTruncated());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNotNull(result.getNextContinuationToken());

            String continuationToken = result.getNextContinuationToken();
            request = new ListBucketInventoryConfigurationsRequest(bucketName, continuationToken);
            result = ossClient.listBucketInventoryConfigurations(request);
            count += result.getInventoryConfigurationList().size();
            Assert.assertEquals(false, result.isTruncated());
            Assert.assertEquals(continuationToken, result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertEquals(sum , count);
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            for (int i = 0; i < sum; i++) {
                String id = idPrefix + String.valueOf(i);
                ossClient.deleteBucketInventoryConfiguration(bucketName, id);
            }
        }
    }

    @Test
    public void testListOneHundredInventoryConfiguration() {
        String idPrefix = "testid-";
        int sum = 100;
        try {
            for (int i = 0; i < sum; i++) {
                String id = idPrefix + String.valueOf(i);
                InventoryConfiguration inventoryConfiguration = createTestInventoryConfiguration(id);
                ossClient.setBucketInventoryConfiguration(bucketName, inventoryConfiguration);
            }
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            ListBucketInventoryConfigurationsRequest request = new ListBucketInventoryConfigurationsRequest(bucketName);
            ListBucketInventoryConfigurationsResult result = ossClient.listBucketInventoryConfigurations(request);
            Assert.assertEquals(false, result.isTruncated());
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            for (int i = 0; i < sum; i++) {
                String id = idPrefix + String.valueOf(i);
                ossClient.deleteBucketInventoryConfiguration(bucketName, id);
            }
        }
    }

    @Test
    public void testListNoneInventoryConfiguration() {
        try {
            ListBucketInventoryConfigurationsRequest request = new ListBucketInventoryConfigurationsRequest(bucketName);
            ListBucketInventoryConfigurationsResult result = ossClient.listBucketInventoryConfigurations(request);
            Assert.fail("There is no inventory configuration, should be failed.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_INVENTORY, e.getErrorCode());
        }
    }

}
