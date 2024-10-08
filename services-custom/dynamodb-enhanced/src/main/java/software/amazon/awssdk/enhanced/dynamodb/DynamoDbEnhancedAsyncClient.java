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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Asynchronous interface for running commands against a DynamoDb database.
 * <p>
 * By default, all command methods throw an {@link UnsupportedOperationException} to prevent interface extensions from breaking
 * implementing classes.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface DynamoDbEnhancedAsyncClient extends DynamoDbEnhancedResource {

    /**
     * Returns a mapped table that can be used to execute commands that work with mapped items against that table.
     *
     * @param tableName   The name of the physical table persisted by DynamoDb.
     * @param tableSchema A {@link TableSchema} that maps the table to a modeled object.
     * @param <T>         The modeled object type being mapped to this table.
     * @return A {@link DynamoDbAsyncTable} object that can be used to execute table operations against.
     */
    <T> DynamoDbAsyncTable<T> table(String tableName, TableSchema<T> tableSchema);

    /**
     * Retrieves items from one or more tables by their primary keys, see {@link Key}. BatchGetItem is a composite operation where
     * the request contains one batch of {@link GetItemEnhancedRequest} per targeted table. The operation makes several calls to
     * the database; each time you iterate over the result to retrieve a page, a call is made for the items on that page.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined in the
     * {@link BatchGetItemEnhancedRequest}.
     * <p>
     * <b>Partial results</b>. A single call to DynamoDb has restraints on how much data can be retrieved.
     * If those limits are exceeded, the call yields a partial result. This may also be the case if provisional throughput is
     * exceeded or there is an internal DynamoDb processing failure. The operation automatically retries any unprocessed keys
     * returned from DynamoDb in subsequent calls for pages.
     * <p>
     * This operation calls the low-level {@link DynamoDbAsyncClient#batchGetItemPaginator} operation. Consult the BatchGetItem
     * documentation for further details and constraints as well as current limits of data retrieval.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * BatchGetResultPagePublisher publisher = enhancedClient.batchGetItem(
     *     BatchGetItemEnhancedRequest.builder()
     *                                .readBatches(ReadBatch.builder(FirstItem.class)
     *                                                      .mappedTableResource(firstItemTable)
     *                                                      .addGetItem(GetItemEnhancedRequest.builder().key(key1).build())
     *                                                      .addGetItem(GetItemEnhancedRequest.builder().key(key2).build())
     *                                                      .build(),
     *                                             ReadBatch.builder(SecondItem.class)
     *                                                      .mappedTableResource(secondItemTable)
     *                                                      .addGetItem(GetItemEnhancedRequest.builder().key(key3).build())
     *                                                      .build())
     *                                .build());
     * }
     * </pre>
     *
     * <p>
     * The returned {@link BatchGetResultPagePublisher} can be subscribed to request a stream of {@link BatchGetResultPage}s or a
     * stream of flattened results belonging to the supplied table across all pages.
     *
     * <p>
     * 1) Subscribing to {@link BatchGetResultPage}s
     * <pre>
     * {@code
     * publisher.subscribe(page -> {
     *     page.resultsForTable(firstItemTable).forEach(item -> System.out.println(item));
     *     page.resultsForTable(secondItemTable).forEach(item -> System.out.println(item));
     * });
     * }
     * </pre>
     *
     * <p>
     * 2) Subscribing to results across all pages
     * <pre>
     * {@code
     * publisher.resultsForTable(firstItemTable).subscribe(item -> System.out.println(item));
     * publisher.resultsForTable(secondItemTable).subscribe(item -> System.out.println(item));
     * }
     * </pre>
     *
     * @param request A {@link BatchGetItemEnhancedRequest} containing keys grouped by tables.
     * @return a publisher {@link SdkPublisher} with paginated results of type {@link BatchGetResultPage}.
     * @see #batchGetItem(Consumer)
     * @see DynamoDbAsyncClient#batchGetItemPaginator
     */
    default BatchGetResultPagePublisher batchGetItem(BatchGetItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves items from one or more tables by their primary keys, see {@link Key}. BatchGetItem is a composite operation where
     * the request contains one batch of {@link GetItemEnhancedRequest} per targeted table. The operation makes several calls to
     * the database; each time you iterate over the result to retrieve a page, a call is made for the items on that page.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link BatchGetItemEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * BatchGetResultPagePublisher batchResults = enhancedClient.batchGetItem(r -> r.addReadBatches(
     *     ReadBatch.builder(FirstItem.class)
     *              .mappedTableResource(firstItemTable)
     *              .addGetItem(i -> i.key(key1))
     *              .addGetItem(i -> i.key(key2))
     *              .build(),
     *     ReadBatch.builder(SecondItem.class)
     *              .mappedTableResource(secondItemTable)
     *              .addGetItem(i -> i.key(key3))
     *              .build()));
     * }
     * </pre>
     *
     * @param requestConsumer a {@link Consumer} of {@link BatchGetItemEnhancedRequest.Builder} containing keys grouped by
     *                        tables.
     * @return a publisher {@link SdkPublisher} with paginated results of type {@link BatchGetResultPage}.
     * @see #batchGetItem(BatchGetItemEnhancedRequest)
     * @see DynamoDbAsyncClient#batchGetItem
     */
    default BatchGetResultPagePublisher batchGetItem(Consumer<BatchGetItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Puts and/or deletes multiple items in one or more tables. BatchWriteItem is a composite operation where the request
     * contains one batch of (a mix of) {@link PutItemEnhancedRequest} and {@link DeleteItemEnhancedRequest} per targeted table.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined in the
     * {@link BatchWriteItemEnhancedRequest}.
     * <p>
     * <b>Note: </b> BatchWriteItem cannot update items. Instead, use the individual updateItem operation
     * {@link DynamoDbAsyncTable#updateItem(UpdateItemEnhancedRequest)}.
     * <p>
     * <b>Partial updates</b><br>Each delete or put call is atomic, but the operation as a whole is not.
     * If individual operations fail due to exceeded provisional throughput internal DynamoDb processing failures, the failed
     * requests can be retrieved through the result, see {@link BatchWriteResult}.
     * <p>
     * There are some conditions that cause the whole batch operation to fail. These include non-existing tables, erroneously
     * defined primary key attributes, attempting to put and delete the same item as well as referring more than once to the same
     * hash and range (sort) key.
     * <p>
     * This operation calls the low-level DynamoDB API BatchWriteItem operation. Consult the BatchWriteItem documentation for
     * further details and constraints, current limits of data to write and/or delete, how to handle partial updates and retries
     * and under which conditions the operation will fail.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * BatchWriteResult batchResult = enhancedClient.batchWriteItem(
     *     BatchWriteItemEnhancedRequest.builder()
     *                                  .writeBatches(WriteBatch.builder(FirstItem.class)
     *                                                          .mappedTableResource(firstItemTable)
     *                                                          .addPutItem(PutItemEnhancedRequest.builder().item(item1).build())
     *                                                          .addDeleteItem(DeleteItemEnhancedRequest.builder()
     *                                                                                                  .key(key2)
     *                                                                                                  .build())
     *                                                          .build(),
     *                                                WriteBatch.builder(SecondItem.class)
     *                                                          .mappedTableResource(secondItemTable)
     *                                                          .addPutItem(PutItemEnhancedRequest.builder().item(item3).build())
     *                                                          .build())
     *                                  .build()).join();
     * }
     * </pre>
     *
     * @param request A {@link BatchWriteItemEnhancedRequest} containing keys and items grouped by tables.
     * @return a {@link CompletableFuture} of {@link BatchWriteResult}, containing any unprocessed requests.
     */
    default CompletableFuture<BatchWriteResult> batchWriteItem(BatchWriteItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Puts and/or deletes multiple items in one or more tables. BatchWriteItem is a composite operation where the request
     * contains one batch of (a mix of) {@link PutItemEnhancedRequest} and {@link DeleteItemEnhancedRequest} per targeted table.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined in the
     * {@link BatchWriteItemEnhancedRequest}.
     * <p>
     * <b>Note: </b> BatchWriteItem cannot update items. Instead, use the individual updateItem operation
     * {@link DynamoDbAsyncTable#updateItem}}.
     * <p>
     * <b>Partial updates</b><br>Each delete or put call is atomic, but the operation as a whole is not.
     * If individual operations fail due to exceeded provisional throughput internal DynamoDb processing failures, the failed
     * requests can be retrieved through the result, see {@link BatchWriteResult}.
     * <p>
     * There are some conditions that cause the whole batch operation to fail. These include non-existing tables, erroneously
     * defined primary key attributes, attempting to put and delete the same item as well as referring more than once to the same
     * hash and range (sort) key.
     * <p>
     * This operation calls the low-level DynamoDB API BatchWriteItem operation. Consult the BatchWriteItem documentation for
     * further details and constraints, current limits of data to write and/or delete, how to handle partial updates and retries
     * and under which conditions the operation will fail.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link BatchWriteItemEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * BatchWriteResult batchResult = enhancedClient.batchWriteItem(r -> r.writeBatches(
     *     WriteBatch.builder(FirstItem.class)
     *               .mappedTableResource(firstItemTable)
     *               .addPutItem(i -> i.item(item1))
     *               .addDeleteItem(i -> i.key(key2))
     *               .build(),
     *     WriteBatch.builder(SecondItem.class)
     *               .mappedTableResource(secondItemTable)
     *               .addPutItem(i -> i.item(item3))
     *               .build())).join();
     * }
     * </pre>
     *
     * @param requestConsumer a {@link Consumer} of {@link BatchWriteItemEnhancedRequest} containing keys and items grouped by
     *                        tables.
     * @return a {@link CompletableFuture} of {@link BatchWriteResult}, containing any unprocessed requests.
     */
    default CompletableFuture<BatchWriteResult> batchWriteItem(Consumer<BatchWriteItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves multiple items from one or more tables in a single atomic transaction. TransactGetItem is a composite operation
     * where the request contains a set of get requests, each containing a table reference and a {@link GetItemEnhancedRequest}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined in the
     * {@link TransactGetItemsEnhancedRequest}.
     * <p>
     * DynamoDb will reject a call to TransactGetItems if the call exceeds limits such as provisioned throughput or allowed size
     * of items, if the request contains errors or if there are conflicting operations accessing the same item, for instance
     * updating and reading at the same time.
     * <p>
     * This operation calls the low-level DynamoDB API TransactGetItems operation. Consult the TransactGetItems documentation for
     * further details and constraints.
     * <p>
     * Examples:
     * <pre>
     * {@code
     *
     * List<TransactGetResultPage> results = enhancedClient.transactGetItems(
     *            TransactGetItemsEnhancedRequest.builder()
     *                                           .addGetItem(firstItemTable, GetItemEnhancedRequest.builder().key(key1).build())
     *                                           .addGetItem(firstItemTable, GetItemEnhancedRequest.builder().key(key2).build())
     *                                           .addGetItem(firstItemTable, GetItemEnhancedRequest.builder().key(key3).build())
     *                                           .addGetItem(secondItemTable, GetItemEnhancedRequest.builder().key(key4).build())
     *                                           .build()).join();
     * }
     * </pre>
     *
     * @param request A {@link TransactGetItemsEnhancedRequest} containing keys with table references.
     * @return a {@link CompletableFuture} containing a list of {@link Document} with the results.
     */
    default CompletableFuture<List<Document>> transactGetItems(TransactGetItemsEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves multiple items from one or more tables in a single atomic transaction. TransactGetItem is a composite operation
     * where the request contains a set of get requests, each containing a table reference and a {@link GetItemEnhancedRequest}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined in the
     * {@link TransactGetItemsEnhancedRequest}.
     * <p>
     * DynamoDb will reject a call to TransactGetItems if the call exceeds limits such as provisioned throughput or allowed size
     * of items, if the request contains errors or if there are conflicting operations accessing the same item, for instance
     * updating and reading at the same time.
     * <p>
     * This operation calls the low-level DynamoDB API TransactGetItems operation. Consult the TransactGetItems documentation for
     * further details and constraints.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link TransactGetItemsEnhancedRequest#builder()}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     *
     * List<TransactGetResultPage> results =  = enhancedClient.transactGetItems(
     *     r -> r.addGetItem(firstItemTable, i -> i.key(k -> k.partitionValue(0)))
     *           .addGetItem(firstItemTable, i -> i.key(k -> k.partitionValue(1)))
     *           .addGetItem(firstItemTable, i -> i.key(k -> k.partitionValue(2)))
     *           .addGetItem(secondItemTable, i -> i.key(k -> k.partitionValue(0)))).join();
     * }
     * </pre>
     *
     * @param requestConsumer a {@link Consumer} of {@link TransactGetItemsEnhancedRequest} containing keys with table
     *                        references.
     * @return a {@link CompletableFuture} containing a list of {@link Document} with the results.
     */
    default CompletableFuture<List<Document>> transactGetItems(
        Consumer<TransactGetItemsEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes and/or modifies multiple items from one or more tables in a single atomic transaction. TransactGetItem is a
     * composite operation where the request contains a set of action requests, each containing a table reference and one of the
     * following requests:
     * <ul>
     *     <li>Condition check of item - {@link ConditionCheck}</li>
     *     <li>Delete item - {@link DeleteItemEnhancedRequest}</li>
     *     <li>Put item - {@link PutItemEnhancedRequest}</li>
     *     <li>Update item - {@link UpdateItemEnhancedRequest}</li>
     * </ul>
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link TransactWriteItemsEnhancedRequest}.
     * <p>
     * DynamoDb will reject a call to TransactWriteItems if the call exceeds limits such as provisioned throughput or allowed size
     * of items, if the request contains errors or if there are conflicting operations accessing the same item. If the request
     * contains condition checks that aren't met, this will also cause rejection.
     * <p>
     * This operation calls the low-level DynamoDB API TransactWriteItems operation. Consult the TransactWriteItems documentation
     * for further details and constraints, current limits of data to write and/or delete and under which conditions the operation
     * will fail.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * enhancedClient.transactWriteItems(
     *     TransactWriteItemsEnhancedRequest.builder()
     *                                      .addPutItem(firstItemTable, PutItemEnhancedRequest.builder().item(item1).build())
     *                                      .addDeleteItem(firstItemTable, DeleteItemEnhancedRequest.builder().key(key2).build())
     *                                      .addConditionCheck(firstItemTable,
     *                                                         ConditionCheck.builder()
     *                                                                       .key(key3)
     *                                                                       .conditionExpression(conditionExpression)
     *                                                                       .build())
     *                                      .addUpdateItem(secondItemTable,
     *                                                     UpdateItemEnhancedRequest.builder().item(item4).build())
     *                                      .build()).join();
     * }
     * </pre>
     *
     * @param request A {@link BatchWriteItemEnhancedRequest} containing keys grouped by tables.
     * @return a {@link CompletableFuture} of {@link Void}.
     */
    default CompletableFuture<Void> transactWriteItems(TransactWriteItemsEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes and/or modifies multiple items from one or more tables in a single atomic transaction. TransactGetItem is a
     * composite operation where the request contains a set of action requests, each containing a table reference and one of the
     * following requests:
     * <ul>
     *     <li>Condition check of item - {@link ConditionCheck}</li>
     *     <li>Delete item - {@link DeleteItemEnhancedRequest}</li>
     *     <li>Put item - {@link PutItemEnhancedRequest}</li>
     *     <li>Update item - {@link UpdateItemEnhancedRequest}</li>
     * </ul>
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link TransactWriteItemsEnhancedRequest}.
     * <p>
     * DynamoDb will reject a call to TransactWriteItems if the call exceeds limits such as provisioned throughput or allowed size
     * of items, if the request contains errors or if there are conflicting operations accessing the same item. If the request
     * contains condition checks that aren't met, this will also cause rejection.
     * <p>
     * This operation calls the low-level DynamoDB API TransactWriteItems operation. Consult the TransactWriteItems documentation
     * for further details and constraints, current limits of data to write and/or delete and under which conditions the operation
     * will fail.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link TransactWriteItemsEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * enhancedClient.transactWriteItems(r -> r.addPutItem(firstItemTable, i -> i.item(item1))
     *                                         .addDeleteItem(firstItemTable, i -> i.key(k -> k.partitionValue(2)))
     *                                         .addConditionCheck(firstItemTable, i -> i.key(key3)
     *                                                                                  .conditionExpression(conditionExpression))
     *                                         .addUpdateItem(secondItemTable, i -> i.item(item4))).join();
     * }
     * </pre>
     *
     * @param requestConsumer a {@link Consumer} of {@link TransactWriteItemsEnhancedRequest} containing keys and items grouped by
     *                        tables.
     * @return a {@link CompletableFuture} of {@link Void}.
     */
    default CompletableFuture<Void> transactWriteItems(Consumer<TransactWriteItemsEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }


    /**
     * Writes and/or modifies multiple items from one or more tables in a single atomic transaction. TransactWriteItem is a
     * composite operation where the request contains a set of action requests, each containing a table reference and one of the
     * following requests:
     * <ul>
     *     <li>Condition check of item - {@link ConditionCheck}</li>
     *     <li>Delete item - {@link DeleteItemEnhancedRequest}</li>
     *     <li>Put item - {@link PutItemEnhancedRequest}</li>
     *     <li>Update item - {@link UpdateItemEnhancedRequest}</li>
     * </ul>
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link TransactWriteItemsEnhancedRequest}.
     * <p>
     * DynamoDb will reject a call to transactWriteItemsWithResponse if the call exceeds limits such as provisioned throughput
     * or allowed size
     * of items, if the request contains errors or if there are conflicting operations accessing the same item. If the request
     * contains condition checks that aren't met, this will also cause rejection.
     * <p>
     * This operation calls the low-level DynamoDB API TransactWriteItems operation. Consult the TransactWriteItems documentation
     * for further details and constraints, current limits of data to write and/or delete and under which conditions the operation
     * will fail.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * enhancedClient.transactWriteItemsWithResponse(
     *     TransactWriteItemsEnhancedRequest.builder()
     *                                      .addPutItem(firstItemTable, PutItemEnhancedRequest.builder().item(item1).build())
     *                                      .addDeleteItem(firstItemTable, DeleteItemEnhancedRequest.builder().key(key2).build())
     *                                      .addConditionCheck(firstItemTable,
     *                                                         ConditionCheck.builder()
     *                                                                       .key(key3)
     *                                                                       .conditionExpression(conditionExpression)
     *                                                                       .build())
     *                                      .addUpdateItem(secondItemTable,
     *                                                     UpdateItemEnhancedRequest.builder().item(item4).build())
     *                                      .build()).join();
     * }
     * </pre>
     *
     * @param request A {@link BatchWriteItemEnhancedRequest} containing keys grouped by tables.
     * @return a {@link CompletableFuture} of {@link TransactWriteItemsEnhancedResponse}.
     */
    default CompletableFuture<TransactWriteItemsEnhancedResponse> transactWriteItemsWithResponse(
        TransactWriteItemsEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes and/or modifies multiple items from one or more tables in a single atomic transaction. TransactWriteItem is a
     * composite operation where the request contains a set of action requests, each containing a table reference and one of the
     * following requests:
     * <ul>
     *     <li>Condition check of item - {@link ConditionCheck}</li>
     *     <li>Delete item - {@link DeleteItemEnhancedRequest}</li>
     *     <li>Put item - {@link PutItemEnhancedRequest}</li>
     *     <li>Update item - {@link UpdateItemEnhancedRequest}</li>
     * </ul>
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link TransactWriteItemsEnhancedRequest}.
     * <p>
     * DynamoDb will reject a call to transactWriteItemsWithResponse if the call exceeds limits such as provisioned throughput
     * or allowed size
     * of items, if the request contains errors or if there are conflicting operations accessing the same item. If the request
     * contains condition checks that aren't met, this will also cause rejection.
     * <p>
     * This operation calls the low-level DynamoDB API TransactWriteItems operation. Consult the TransactWriteItems documentation
     * for further details and constraints, current limits of data to write and/or delete and under which conditions the operation
     * will fail.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link TransactWriteItemsEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * enhancedClient.transactWriteItemsWithResponse(r -> r.addPutItem(firstItemTable, i -> i.item(item1))
     *                                         .addDeleteItem(firstItemTable, i -> i.key(k -> k.partitionValue(2)))
     *                                         .addConditionCheck(firstItemTable, i -> i.key(key3)
     *                                                                                  .conditionExpression(conditionExpression))
     *                                         .addUpdateItem(secondItemTable, i -> i.item(item4))).join();
     * }
     * </pre>
     *
     * @param requestConsumer a {@link Consumer} of {@link TransactWriteItemsEnhancedRequest} containing keys and items grouped by
     *                        tables.
     * @return a {@link CompletableFuture} of {@link Void}.
     */
    default CompletableFuture<TransactWriteItemsEnhancedResponse> transactWriteItemsWithResponse(
        Consumer<TransactWriteItemsEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }


    /**
     * Creates a default builder for {@link DynamoDbEnhancedAsyncClient}.
     */
    static Builder builder() {
        return DefaultDynamoDbEnhancedAsyncClient.builder();
    }

    /**
     * Creates a {@link DynamoDbEnhancedClient} with a default {@link DynamoDbAsyncClient}
     */
    static DynamoDbEnhancedAsyncClient create() {
        return builder().build();
    }

    /**
     * The builder definition for a {@link DynamoDbEnhancedAsyncClient}.
     */
    @NotThreadSafe
    interface Builder extends DynamoDbEnhancedResource.Builder {
        /**
         * The regular low-level SDK client to use with the enhanced client.
         *
         * @param dynamoDbClient an initialized {@link DynamoDbAsyncClient}
         */
        Builder dynamoDbClient(DynamoDbAsyncClient dynamoDbClient);

        @Override
        Builder extensions(DynamoDbEnhancedClientExtension... dynamoDbEnhancedClientExtensions);

        @Override
        Builder extensions(List<DynamoDbEnhancedClientExtension> dynamoDbEnhancedClientExtensions);

        /**
         * Builds an enhanced client based on the settings supplied to this builder
         *
         * @return An initialized {@link DynamoDbEnhancedAsyncClient}
         */
        DynamoDbEnhancedAsyncClient build();
    }
}
