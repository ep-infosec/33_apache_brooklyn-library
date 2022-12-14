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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.entity.webapp;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.sensor.EnricherSpec;
import org.apache.brooklyn.core.location.access.BrooklynAccessUtils;
import org.apache.brooklyn.entity.java.UsesJavaMXBeans;
import org.apache.brooklyn.policy.enricher.RollingTimeWindowMeanEnricher;
import org.apache.brooklyn.policy.enricher.TimeFractionDeltaEnricher;
import org.apache.brooklyn.policy.enricher.TimeWeightedDeltaEnricher;
import org.apache.brooklyn.util.time.Duration;

import com.google.common.net.HostAndPort;

public class WebAppServiceMethods implements WebAppServiceConstants {

    public static final Duration DEFAULT_WINDOW_DURATION = Duration.TEN_SECONDS;

    public static void connectWebAppServerPolicies(Entity entity) {
        connectWebAppServerPolicies(entity, DEFAULT_WINDOW_DURATION);
    }

    public static void connectWebAppServerPolicies(Entity entity, Duration windowPeriod) {
        entity.enrichers().add(EnricherSpec.create(TimeWeightedDeltaEnricher.class)
                .configure("producer", entity)
                .configure("source", REQUEST_COUNT)
                .configure("target", REQUESTS_PER_SECOND_LAST)
                .configure("unitMillis", 1000));

        if (windowPeriod!=null) {
            entity.enrichers().add(EnricherSpec.create(RollingTimeWindowMeanEnricher.class)
                    .configure("producer", entity)
                    .configure("source", REQUESTS_PER_SECOND_LAST)
                    .configure("target", REQUESTS_PER_SECOND_IN_WINDOW)
                    .configure("timePeriod", windowPeriod));
        }

        entity.enrichers().add(EnricherSpec.create(TimeFractionDeltaEnricher.class)
                .configure("producer", entity)
                .configure("source", TOTAL_PROCESSING_TIME)
                .configure("target", PROCESSING_TIME_FRACTION_LAST)
                .configure("durationPerOrigUnit", Duration.millis(1)));

        if (windowPeriod!=null) {
            entity.enrichers().add(EnricherSpec.create(RollingTimeWindowMeanEnricher.class)
                    .configure("producer", entity)
                    .configure("source", PROCESSING_TIME_FRACTION_LAST)
                    .configure("target", PROCESSING_TIME_FRACTION_IN_WINDOW)
                    .configure("timePeriod", windowPeriod));
        }
    }

    public static Set<String> getEnabledProtocols(Entity entity) {
        return entity.getAttribute(WebAppService.ENABLED_PROTOCOLS);
    }

    public static boolean isProtocolEnabled(Entity entity, String protocol) {
        for (String contender : getEnabledProtocols(entity)) {
            if (protocol.equalsIgnoreCase(contender)) {
                return true;
            }
        }
        return false;
    }

    public static String inferBrooklynAccessibleRootUrl(Entity entity) {
        if (isProtocolEnabled(entity, "https")) {
            Integer rawPort = entity.getAttribute(HTTPS_PORT);
            checkNotNull(rawPort, "HTTPS_PORT sensors not set for %s; is an acceptable port available?", entity);
            HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(entity, rawPort);
            return String.format("https://%s:%s/", hp.getHost(), hp.getPort());
        } else if (isProtocolEnabled(entity, "http")) {
            Integer rawPort = entity.getAttribute(HTTP_PORT);
            checkNotNull(rawPort, "HTTP_PORT sensors not set for %s; is an acceptable port available?", entity);
            HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(entity, rawPort);
            return String.format("http://%s:%s/", hp.getHost(), hp.getPort());
        } else {
            throw new IllegalStateException("HTTP and HTTPS protocols not enabled for "+entity+"; enabled protocols are "+getEnabledProtocols(entity));
        }
    }
}
