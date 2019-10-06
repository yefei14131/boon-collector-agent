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

package org.apache.skywalking.apm.plugin.mybatis.interceptor;

import com.google.gson.Gson;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.constant.TagConstant;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.define.StatementEnhanceInfos;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author yefei
 * @date: 2019/10/3
 */
public class StatementsExecuteInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(StatementsExecuteInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {
        try {
            StatementEnhanceInfos cacheObject = (StatementEnhanceInfos)objInst.getSkyWalkingDynamicField();
            if (cacheObject != null) {
                ConnectionInfo connectInfo = cacheObject.getConnectionInfo();
                AbstractSpan span = ContextManager.createExitSpan(buildOpeationName(connectInfo.getDBType(), objInst.getClass().getSimpleName(), method.getName()), connectInfo.getDatabasePeer());
                SpanLayer.asDB(span);
                Tags.DB_TYPE.set(span, "sql");
                Tags.DB_INSTANCE.set(span, connectInfo.getDatabaseName());
                Tags.DB_STATEMENT.set(span, cacheObject.getSql());
                span.setComponent(ComponentsDefine.MYBATIS);
                TagConstant.REQ_DATA.set(span, cacheObject.getSql());

                if (Config.Plugin.MySQL.TRACE_SQL_PARAMETERS) {
                    final Object[] parameters = cacheObject.getParameters();
                    if (parameters != null && parameters.length > 0) {
                        int maxIndex = cacheObject.getMaxIndex();
                        String parameterString = buildParameterString(parameters, maxIndex);
                        int sqlParametersMaxLength = Config.Plugin.MySQL.SQL_PARAMETERS_MAX_LENGTH;
                        if (sqlParametersMaxLength > 0 && parameterString.length() > sqlParametersMaxLength) {
                            parameterString = parameterString.substring(0, sqlParametersMaxLength) + "...";
                        }
                        TagConstant.SQL_PARAMETERS.set(span, parameterString);
                    }
                }
            } else {
                logger.debug("mybatis statement execute, cacheObject is null");
            }

        } catch (Exception e) {
            logger.error(e, "mybatis routing statement before error");
        }
    }

    private String buildOpeationName(String componentName, String statementClassName, String methodName) {
        return "Mybatis/" + componentName + "/" + statementClassName.replace("Handler", "") + "/" + methodName;
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        AbstractSpan span = ContextManager.activeSpan();
        try {
            if (span != null) {

                if (ret instanceof List && ((List) ret).size() > 0) {
                    TagConstant.RESP_DATA.set(span, new Gson().toJson(ret));
                    TagConstant.RESP_CLASS.set(span, ((List) ret).get(0).getClass().getName());
                } else {
                    TagConstant.RESP_DATA.set(span, ret.toString());
                    TagConstant.RESP_CLASS.set(span, ret.getClass().getName());
                }

            }

        } catch (Exception e) {
            logger.error(e,"mybatis statement execute error");
        } finally {
            ContextManager.stopSpan();
            return ret;
        }

    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }

    private String buildParameterString(Object[] parameters, int maxIndex) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for (int i = 0; i < maxIndex; i++) {
            Object parameter = parameters[i];
            if (!first) {
                builder.append(",");
            }
            builder.append(parameter);
            first = false;
        }
        builder.append("]");
        return builder.toString();
    }
}
