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
import org.apache.skywalking.apm.agent.core.constant.TagConstant;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.CLIENT;
import static org.apache.skywalking.apm.plugin.grpc.v1.OperationNameFormatUtil.formatOperationName;

/**
 * @author yefei
 * @date: 2019/9/21
 */
public class FutureCallClientInterceptor  extends ForwardingClientCall.SimpleForwardingClientCall<Message, Message> implements CallClientInterceptor {
    private static final ILog logger = LogManager.getLogger(FutureCallClientInterceptor.class);

    private final MethodDescriptor methodDescriptor;
    private final Channel channel;
    private final String serviceName;
    private final String operationPrefix;
    private final String remotePeer;
    private AbstractSpan exitSpan = null;

    public FutureCallClientInterceptor(ClientCall delegate, MethodDescriptor method, Channel channel) {
        super(delegate);
        this.methodDescriptor = method;
        this.channel = channel;
        this.remotePeer = channel.authority();
        this.serviceName = formatOperationName(method);
        this.operationPrefix = serviceName + CLIENT;
        logger.info("FutureCallClientInterceptor: {}", serviceName);
    }


    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }


    @Override
    public void start(Listener<Message> responseListener, Metadata headers) {

        final ContextCarrier contextCarrier = new ContextCarrier();
        exitSpan = ContextManager.createExitSpan(serviceName, contextCarrier, remotePeer);
        exitSpan.setLayer(SpanLayer.RPC_FRAMEWORK);
        exitSpan.setComponent(ComponentsDefine.GRPC);
        exitSpan.prepareForAsync();
        ContextManager.capture();

        CarrierItem contextItem = contextCarrier.items();
        while (contextItem.hasNext()) {
            contextItem = contextItem.next();
            Metadata.Key<String> headerKey = Metadata.Key.of(contextItem.getHeadKey(), Metadata.ASCII_STRING_MARSHALLER);
            headers.put(headerKey, contextItem.getHeadValue());
        }

        delegate().start(new ClientCallLister(responseListener), headers);
        ContextManager.stopSpan();
    }

    @Override
    public void sendMessage(Message message) {
        try {
            exitSpan.tag(TagConstant.REQ_DATA, JsonFormat.printer().print(message));
        } catch (Exception e) {

        } finally {
            delegate().sendMessage(message);
        }
    }

    private class ClientCallLister extends ForwardingClientCallListener.SimpleForwardingClientCallListener<Message> {

        public ClientCallLister(Listener delegate) {
            super(delegate);
        }

        @Override
        public void onMessage(Message message) {
            try {
                exitSpan.tag(TagConstant.RESP_DATA, JsonFormat.printer().print(message));
            } catch (Exception e) {
                logger.error(e, "client onMessage error: {}", operationPrefix);

            } finally {
                delegate().onMessage(message);
            }
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            try {
                exitSpan.asyncFinish();
            } catch (Throwable t) {
                //ignore
            } finally {
                delegate().onClose(status, trailers);
            }
        }
    }
}
