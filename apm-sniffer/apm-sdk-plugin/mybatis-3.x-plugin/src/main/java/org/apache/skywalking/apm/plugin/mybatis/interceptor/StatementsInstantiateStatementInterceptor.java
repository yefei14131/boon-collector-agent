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

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;

/**
 * @author yefei
 * @date: 2019/10/3
 */
public class StatementsInstantiateStatementInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(StatementsInstantiateStatementInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        try {
            if (ret != null && ret instanceof EnhancedInstance) {
                Object skyWalkingDynamicField = ((EnhancedInstance) ret).getSkyWalkingDynamicField();
                if (skyWalkingDynamicField != null) {
                    objInst.setSkyWalkingDynamicField(skyWalkingDynamicField);
                } else {
                    logger.debug("mybatis instantiate statement, ret.getSkyWalkingDynamicField is null");
                }

            } else {
                Object connection = allArguments[0];
                logger.info("mybatis {}  instantiate statemen, statement is not instanceof EnhancedInstance, statement type : {}, connection type: {}", objInst.getClass().getSimpleName(), ret.getClass().getName(), connection.getClass().getName());
            }
        } catch (Exception e) {
            logger.error(e, "mybatis instantiate statement before error");
        } finally {
            return ret;
        }
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
