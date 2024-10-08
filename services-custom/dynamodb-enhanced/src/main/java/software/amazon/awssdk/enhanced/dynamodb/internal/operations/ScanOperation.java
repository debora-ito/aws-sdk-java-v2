/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.ProjectionExpression;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@SdkInternalApi
public class ScanOperation<T> implements PaginatedTableOperation<T, ScanRequest, ScanResponse>,
        PaginatedIndexOperation<T, ScanRequest, ScanResponse> {


    private final ScanEnhancedRequest request;

    private ScanOperation(ScanEnhancedRequest request) {
        this.request = request;
    }

    public static <T> ScanOperation<T> create(ScanEnhancedRequest request) {
        return new ScanOperation<>(request);
    }

    @Override
    public OperationName operationName() {
        return OperationName.SCAN;
    }

    @Override
    public ScanRequest generateRequest(TableSchema<T> tableSchema,
                                       OperationContext operationContext,
                                       DynamoDbEnhancedClientExtension extension) {
        Map<String, AttributeValue> expressionValues = null;
        Map<String, String> expressionNames = null;

        if (this.request.filterExpression() != null) {
            expressionValues = this.request.filterExpression().expressionValues();
            expressionNames = this.request.filterExpression().expressionNames();
        }

        String projectionExpressionAsString = null;
        if (this.request.attributesToProject() != null) {
            ProjectionExpression attributesToProject = ProjectionExpression.create(this.request.nestedAttributesToProject());
            projectionExpressionAsString = attributesToProject.projectionExpressionAsString().orElse(null);
            expressionNames = Expression.joinNames(expressionNames, attributesToProject.expressionAttributeNames());
        }

        ScanRequest.Builder scanRequest = ScanRequest.builder()
            .tableName(operationContext.tableName())
            .limit(this.request.limit())
            .segment(this.request.segment())
            .totalSegments(this.request.totalSegments())
            .exclusiveStartKey(this.request.exclusiveStartKey())
            .consistentRead(this.request.consistentRead())
            .returnConsumedCapacity(this.request.returnConsumedCapacity())
            .select(this.request.select())
            .expressionAttributeValues(expressionValues)
            .expressionAttributeNames(expressionNames)
            .projectionExpression(projectionExpressionAsString);

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            scanRequest = scanRequest.indexName(operationContext.indexName());
        }

        if (this.request.filterExpression() != null) {
            scanRequest = scanRequest.filterExpression(this.request.filterExpression().expression());
        }

        return scanRequest.build();
    }

    @Override
    public Page<T> transformResponse(ScanResponse response,
                                     TableSchema<T> tableSchema,
                                     OperationContext context,
                                     DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {

        return EnhancedClientUtils.readAndTransformPaginatedItems(response,
                                                                  tableSchema,
                                                                  context,
                                                                  dynamoDbEnhancedClientExtension,
                                                                  ScanResponse::items,
                                                                  ScanResponse::lastEvaluatedKey,
                                                                  ScanResponse::count,
                                                                  ScanResponse::scannedCount,
                                                                  ScanResponse::consumedCapacity);
    }

    @Override
    public Function<ScanRequest, SdkIterable<ScanResponse>> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::scanPaginator;
    }

    @Override
    public Function<ScanRequest, SdkPublisher<ScanResponse>> asyncServiceCall(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return dynamoDbAsyncClient::scanPaginator;
    }

}
