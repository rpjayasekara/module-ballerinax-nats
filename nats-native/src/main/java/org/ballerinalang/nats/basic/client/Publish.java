/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nats.basic.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;
import org.ballerinalang.nats.observability.NatsTracingUtil;


/**
 * Extern function to publish message to a given subject.
 *
 * @since 0.995
 */
public class Publish {

    public static Object externPublish(Environment environment, BObject clientObject,
                                       BString subject, BArray data, Object replyTo) {
        NatsTracingUtil.traceResourceInvocation(environment, clientObject, subject.getValue());
        Connection natsConnection = (Connection) clientObject.getNativeData(Constants.NATS_CONNECTION);
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) clientObject.getNativeData(Constants.NATS_METRIC_UTIL);
        byte[] byteContent = data.getBytes();
        try {
            if (TypeUtils.getType(replyTo).getTag() == TypeTags.STRING_TAG) {
                natsConnection.publish(subject.getValue(), ((BString) replyTo).getValue(), byteContent);
            } else {
                natsConnection.publish(subject.getValue(), byteContent);
            }
            natsMetricsReporter.reportPublish(subject.getValue(), byteContent.length);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            natsMetricsReporter.reportProducerError(subject.getValue(),
                                                    NatsObservabilityConstants.ERROR_TYPE_PUBLISH);
            return Utils.createNatsError(Constants.PRODUCER_ERROR +
                                                 subject.getValue() + ". " + ex.getMessage());
        }
        return null;
    }
}
