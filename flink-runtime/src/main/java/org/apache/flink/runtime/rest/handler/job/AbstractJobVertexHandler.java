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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.rest.handler.job;

import org.apache.flink.runtime.executiongraph.AccessExecutionGraph;
import org.apache.flink.runtime.executiongraph.AccessExecutionJobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.rest.handler.HandlerRequest;
import org.apache.flink.runtime.rest.handler.RestHandlerException;
import org.apache.flink.runtime.rest.handler.legacy.ExecutionGraphCache;
import org.apache.flink.runtime.rest.messages.EmptyRequestBody;
import org.apache.flink.runtime.rest.messages.JobIDPathParameter;
import org.apache.flink.runtime.rest.messages.JobVertexIdPathParameter;
import org.apache.flink.runtime.rest.messages.JobVertexMessageParameters;
import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.runtime.rest.messages.ResponseBody;
import org.apache.flink.runtime.webmonitor.RestfulGateway;
import org.apache.flink.runtime.webmonitor.retriever.GatewayRetriever;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Base class for request handlers whose response depends on a specific job vertex (defined via the
 * "{@link JobVertexIdPathParameter#KEY}" parameter) in a specific job, defined via (defined via the
 * "{@link JobIDPathParameter#KEY}" parameter).
 *
 * @param <R> the response type
 * @param <M> the message parameters type
 */
public abstract class AbstractJobVertexHandler<
                R extends ResponseBody, M extends JobVertexMessageParameters>
        extends AbstractAccessExecutionGraphHandler<R, M> {

    /**
     * Instantiates a new Abstract job vertex handler.
     *
     * @param leaderRetriever the leader retriever
     * @param timeout the timeout
     * @param responseHeaders the response headers
     * @param messageHeaders the message headers
     * @param executionGraphCache the execution graph cache
     * @param executor the executor
     */
    protected AbstractJobVertexHandler(
            GatewayRetriever<? extends RestfulGateway> leaderRetriever,
            Duration timeout,
            Map<String, String> responseHeaders,
            MessageHeaders<EmptyRequestBody, R, M> messageHeaders,
            ExecutionGraphCache executionGraphCache,
            Executor executor) {

        super(
                leaderRetriever,
                timeout,
                responseHeaders,
                messageHeaders,
                executionGraphCache,
                executor);
    }

    @Override
    protected R handleRequest(
            HandlerRequest<EmptyRequestBody> request, AccessExecutionGraph executionGraph)
            throws RestHandlerException {

        final JobVertexID jobVertexID = request.getPathParameter(JobVertexIdPathParameter.class);
        final AccessExecutionJobVertex jobVertex = executionGraph.getJobVertex(jobVertexID);

        if (jobVertex == null) {
            throw new RestHandlerException(
                    "No vertex with ID '" + jobVertexID + "' exists.",
                    HttpResponseStatus.NOT_FOUND);
        }

        return handleRequest(request, jobVertex);
    }

    /**
     * Called for each request after the corresponding {@link AccessExecutionJobVertex} has been
     * retrieved from the {@link AccessExecutionGraph}.
     *
     * @param request the request
     * @param jobVertex the execution job vertex
     * @return the response
     * @throws RestHandlerException if the handler could not process the request
     */
    protected abstract R handleRequest(
            HandlerRequest<EmptyRequestBody> request, AccessExecutionJobVertex jobVertex)
            throws RestHandlerException;
}
