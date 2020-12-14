/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.event.processor;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

import com.rackspace.salus.telemetry.entities.EventEngineTask;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.Comparator;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.ComparisonExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.StateExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.TaskState;
import com.rackspace.salus.telemetry.entities.subtype.SalusEventEngineTask;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

/**
 * <b>NOTE</b> this test is assisted by {@link EventProcessorTest} where the same machine built
 * by {@link EventProcessorContextBuilder} gets exercised and validated.
 */
public class EventProcessorContextBuilderTest {

  @Test
  public void testFromTask() {
    final String tenantId = randomAlphanumeric(10);
    final String resourceId = randomAlphanumeric(5);
    final String monitorId = randomAlphanumeric(5);

    final EventEngineTask task = new SalusEventEngineTask()
        .setId(UUID.randomUUID())
        .setTenantId(tenantId)
        .setTaskParameters(new EventEngineTaskParameters()
            .setMetricGroup("cpu")
            .setLabelSelector(
                Map.of("resource_id", resourceId,
                    "monitor_id", monitorId
                )
            )
            .setZoneLabel("monitoring_zone_id")
            .setZoneQuorumCount(2)
            .setStateExpressions(List.of(
                new StateExpression()
                    .setState(TaskState.WARNING)
                    .setExpression(new ComparisonExpression()
                        .setInput("usage")
                        .setComparator(Comparator.GREATER_THAN)
                        .setComparisonValue(50)
                    )
                    .setMessage("usage getting too high")
            ))
        );

    final EventProcessorContext result = EventProcessorContextBuilder.fromTask(task);

    assertThat(result).isNotNull();
    assertThat(result.getTask()).isSameAs(task);
    assertThat(result.getStateMachine()).isNotNull();
  }
}