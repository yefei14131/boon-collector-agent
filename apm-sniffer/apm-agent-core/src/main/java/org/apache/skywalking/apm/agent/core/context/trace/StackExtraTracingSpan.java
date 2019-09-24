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

package org.apache.skywalking.apm.agent.core.context.trace;

import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.network.language.agent.v2.SpanObjectV2;

/**
 * @author yefei
 * @date: 2019/9/23
 */
public abstract class StackExtraTracingSpan extends StackBasedTracingSpan {
    protected String reqData = null;
    protected String respData = null;
//
//    public String getReqData() {
//        return reqData;
//    }

    public void setReqData(String reqData) {
        this.reqData = reqData;
    }
//
//    public String getRespData() {
//        return respData;
//    }

    public void setRespData(String respData) {
        this.respData = respData;
    }

    protected StackExtraTracingSpan(int spanId, int parentSpanId, String operationName) {
        super(spanId, parentSpanId, operationName);
    }

    protected StackExtraTracingSpan(int spanId, int parentSpanId, int operationId) {
        super(spanId, parentSpanId, operationId);
    }

    public StackExtraTracingSpan(int spanId, int parentSpanId, int operationId, int peerId) {
        super(spanId, parentSpanId, operationId, peerId);
    }

    public StackExtraTracingSpan(int spanId, int parentSpanId, int operationId, String peer) {
        super(spanId, parentSpanId, operationId, peer);
    }

    protected StackExtraTracingSpan(int spanId, int parentSpanId, String operationName, String peer) {
        super(spanId, parentSpanId, operationName, peer);
    }

    protected StackExtraTracingSpan(int spanId, int parentSpanId, String operationName, int peerId) {
        super(spanId, parentSpanId, operationName, peerId);
    }
    @Override
    public SpanObjectV2.Builder transform() {
        SpanObjectV2.Builder spanBuilder = super.transform();
        if (this.reqData != null) {
            spanBuilder.setReqData(this.reqData);
        }
        if (this.respData != null) {
            spanBuilder.setRespData(this.respData);
        }

        return spanBuilder;
    }

    public String getTag(String tagKey) {
        for (TagValuePair tag : this.tags) {
            if (tag.getKey().key().equals(tagKey)) {
                return tag.getValue();
            }
        }
        return null;
    }

    public String getTag(int tagid) {
        for (TagValuePair tag : this.tags) {
            if (tag.getKey().getId() == tagid) {
                return tag.getValue();
            }
        }
        return null;
    }
}
