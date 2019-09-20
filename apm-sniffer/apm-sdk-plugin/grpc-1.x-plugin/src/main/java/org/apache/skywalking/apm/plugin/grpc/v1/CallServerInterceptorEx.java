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
import io.grpc.*;
import org.apache.skywalking.apm.agent.core.context.*;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;
import java.util.HashMap;
import java.util.Map;
import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.SERVER;
import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.STREAM_REQUEST_OBSERVER_ON_NEXT_OPERATION_NAME;
import static org.apache.skywalking.apm.plugin.grpc.v1.OperationNameFormatUtil.formatOperationName;

/**
 * @author yefei
 * @date: 2019/9/20
 */
public class CallServerInterceptorEx extends CallServerInterceptor {
    private static final ILog logger = LogManager.getLogger(CallServerInterceptor.class);

    @Override
    public ServerCall.Listener interceptCall(ServerCall call, Metadata headers, ServerCallHandler handler) {
        Map<String, String> headerMap = new HashMap<String, String>();
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
            return new ServerCallListenerEx(handler.startCall(new ServerCallEx(call, entrySpan), headers), call.getMethodDescriptor(), contextSnapshot);

        } finally {
            ContextManager.stopSpan();
        }
    }

    public class ServerCallEx extends ForwardingServerCall.SimpleForwardingServerCall<Message, Message> {

        private final MethodDescriptor descriptor;
        private final String operationPrefix;
        private final AbstractSpan entrySpan;

        public ServerCallEx(io.grpc.ServerCall delegate, AbstractSpan entrySpan) {
            super(delegate);
            this.descriptor = delegate.getMethodDescriptor();
            this.operationPrefix = formatOperationName(descriptor) + SERVER;
            this.entrySpan = entrySpan;
        }

        @Override
        public void close(Status status, Metadata trailers) {
            delegate().close(status, trailers);
            entrySpan.asyncFinish();
            ContextManager.stopSpan();
        }
    }

    public class ServerCallListenerEx extends ServerCallListener {

        private final ContextSnapshot contextSnapshot;
        private final String operationPrefix;

        protected ServerCallListenerEx(ServerCall.Listener delegate, MethodDescriptor descriptor, ContextSnapshot contextSnapshot) {
            super(delegate, descriptor, contextSnapshot);
            this.contextSnapshot = contextSnapshot;
            this.operationPrefix = OperationNameFormatUtil.formatOperationName(descriptor) + SERVER;
        }

        @Override
        public void onMessage(Object message) {
            try {
                ContextManager.createLocalSpan(operationPrefix + STREAM_REQUEST_OBSERVER_ON_NEXT_OPERATION_NAME)
                        .setLayer(SpanLayer.RPC_FRAMEWORK).setComponent(ComponentsDefine.GRPC);
                ContextManager.continued(contextSnapshot);
                delegate().onMessage(message);
            } catch (Throwable t) {
                ContextManager.activeSpan().errorOccurred().log(t);
            }
        }
    }
}
