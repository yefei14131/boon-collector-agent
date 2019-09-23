/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.grpc.v1;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.StackExtraTracingSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.HashMap;

import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.SERVER;
import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.STREAM_REQUEST_OBSERVER_ON_NEXT_OPERATION_NAME;

/**
 * @author yefei
 * @date: 2019/9/20
 */
public class CallServerInterceptorEx extends CallServerInterceptor {
    private static final ILog logger = LogManager.getLogger(CallServerInterceptorEx.class);

    @Override
    public ServerCall.Listener interceptCall(ServerCall call, Metadata headers, ServerCallHandler handler) {
        HashMap<String, String> headerMap = new HashMap<String, String>();
        for (String key : headers.keys()) {
            if (!key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                String value = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
                headerMap.put(key, value);
            }
        }

        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            String contextValue = headerMap.get(next.getHeadKey());
            if (!StringUtil.isEmpty(contextValue)) {
                next.setHeadValue(contextValue);
            }
        }

        final AbstractSpan entrySpan = ContextManager.createEntrySpan(OperationNameFormatUtil.formatOperationName(call.getMethodDescriptor()), contextCarrier);
        entrySpan.setComponent(ComponentsDefine.GRPC).setLayer(SpanLayer.RPC_FRAMEWORK);
        entrySpan.prepareForAsync();
        ContextSnapshot contextSnapshot = ContextManager.capture();
        try {
            return new ServerCallListenerEx(handler.startCall(new ServerCallEx(call, entrySpan), headers), call.getMethodDescriptor(), contextSnapshot, entrySpan);

        } finally {
            ContextManager.stopSpan();
        }
    }

    public class ServerCallEx extends ForwardingServerCall.SimpleForwardingServerCall<Message, Message> {
        private final AbstractSpan entrySpan;

        public ServerCallEx(io.grpc.ServerCall delegate, AbstractSpan entrySpan) {
            super(delegate);
            this.entrySpan = entrySpan;
        }

        @Override
        public void close(Status status, Metadata trailers) {
            delegate().close(status, trailers);
            entrySpan.asyncFinish();
            // close onMessage span
            ContextManager.stopSpan();
        }

        @Override
        public void sendMessage(Message message) {
            try {
                ((StackExtraTracingSpan)entrySpan).setRespData(JsonFormat.printer().print(message));
            } catch (Exception e) {
                logger.error(e, "grpc server parse resp message error");
            } finally {
                delegate().sendMessage(message);
            }
        }
    }

    public class ServerCallListenerEx extends ServerCallListener {

        private final ContextSnapshot contextSnapshot;
        private final AbstractSpan entrySpan;
        private final String operationPrefix;


        protected ServerCallListenerEx(ServerCall.Listener delegate, MethodDescriptor descriptor, ContextSnapshot contextSnapshot, AbstractSpan entrySpan) {
            super(delegate, descriptor, contextSnapshot);
            this.contextSnapshot = contextSnapshot;
            this.operationPrefix = OperationNameFormatUtil.formatOperationName(descriptor) + SERVER;
            this.entrySpan = entrySpan;
        }

        @Override
        public void onMessage(Message message) {
            try {
                ContextManager.createLocalSpan(operationPrefix + STREAM_REQUEST_OBSERVER_ON_NEXT_OPERATION_NAME)
                        .setLayer(SpanLayer.RPC_FRAMEWORK).setComponent(ComponentsDefine.GRPC);
                ContextManager.continued(contextSnapshot);
                ((StackExtraTracingSpan)entrySpan).setReqData(JsonFormat.printer().print(message));
            } catch (Throwable t) {
                ContextManager.activeSpan().errorOccurred().log(t);
            } finally {
                delegate().onMessage(message);
            }
        }
    }
}
