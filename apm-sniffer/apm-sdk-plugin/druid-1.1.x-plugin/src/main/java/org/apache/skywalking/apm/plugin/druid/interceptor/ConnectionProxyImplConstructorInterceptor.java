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

package org.apache.skywalking.apm.plugin.druid.interceptor;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * @author yefei
 * @date: 2019/10/3
 */
public class ConnectionProxyImplConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final ILog logger = LogManager.getLogger(ConnectionProxyImplConstructorInterceptor.class);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        Object realConnection = allArguments[1];
        logger.info("druid connection proxy impl real connection, realConnection type: {}", realConnection.getClass().getName());
        logger.info("druid connection proxy impl real connection, realConnection instanceof EnhancedInstance: {}", realConnection instanceof EnhancedInstance);
        if (realConnection instanceof EnhancedInstance) {
            objInst.setSkyWalkingDynamicField(((EnhancedInstance) realConnection).getSkyWalkingDynamicField());
        }
    }
}
