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
package org.apache.skywalking.apm.agent.core.constant;

import org.apache.skywalking.apm.agent.core.context.tag.StringTag;

/**
 * @author yefei
 * @date: 2019/9/30
 */
public class TagConstant {
    public static final StringTag REQ_DATA = new StringTag(99,"req.data", true);
    public static final StringTag RESP_DATA = new StringTag(98,"resp.data", true);
    public static final StringTag RESP_CLASS = new StringTag(97,"resp.class", true);
    public static final StringTag AGENT_DEBUG = new StringTag(96,"agent.debug");
    public static final StringTag SQL_PARAMETERS = new StringTag(95,"db.sql.parameters", true);
}
