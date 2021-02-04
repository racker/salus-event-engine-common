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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rackspace.salus.telemetry.entities.EventEngineTask;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.Comparator;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.ComparisonExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.LogicalExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.LogicalExpression.Operator;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.StateExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.TaskState;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventProcessorTest {

  private static final String WARNING_MESSAGE = "usage getting too high";

  @Mock
  StateChangeHandler stateChangeHandler;

  @Test
  public void testInterpolateMessage_typical() {
    final EventProcessor eventProcessor = new EventProcessor();

    final EventProcessorInput input = new EventProcessorInput(
        Instant.parse("2007-12-03T10:15:30.00Z"), "zone",
        Map.of("usage", 12)
    );
    final StateExpression result1 = new StateExpression().setMessage("Usage = #{usage} .");
    final String result = eventProcessor.interpolateMessage(
        result1.getMessage(), input.getMetrics()
    );

    assertThat(result).isEqualTo("Usage = 12 .");
  }

  @Test
  public void testInterpolateMessage_missing() {
    final EventProcessor eventProcessor = new EventProcessor();

    final EventProcessorInput input = new EventProcessorInput(
        Instant.parse("2007-12-03T10:15:30.00Z"), "zone",
        Map.of("usage", 12)
    );
    final StateExpression result1 = new StateExpression().setMessage("other = #{other} .");
    final String result = eventProcessor.interpolateMessage(
        result1.getMessage(), input.getMetrics()
    );

    assertThat(result).isEqualTo("other = undefined .");
  }

  @Test
  public void testProcess() {
    final EventProcessor eventProcessor = new EventProcessor();

    // Builds a task with
    // warning and critical state expressions
    // expected quorum of 2 zones
    // consecutive count of 1
    EventProcessorContext context = buildTestContext();

    final List<EventProcessorInput> inputs = List.of(
        // state starts as indeterminate (null)
        buildInput("2007-12-03T10:15:30.00Z", "public/west", 25, 50),
        buildInput("2007-12-03T10:15:31.00Z", "public/east", 31, 50),
        // -> OK since quorum is 2 zones and metrics don't match either state expression
        buildInput("2007-12-03T10:15:32.00Z", "public/west", 60, 50),
        buildInput("2007-12-03T10:15:33.00Z", "public/east", 61, 50),
        // -> WARNING
        buildInput("2007-12-03T10:15:34.00Z", "public/west", 90, 50),
        buildInput("2007-12-03T10:15:35.00Z", "public/east", 91, 50),
        // -> CRITICAL
        buildInput("2007-12-03T10:15:36.00Z", "public/west", 65, 50),
        buildInput("2007-12-03T10:15:37.00Z", "public/east", 66, 50),
        // -> WARNING
        buildInput("2007-12-03T10:15:36.00Z", "public/west", 65, 10),
        buildInput("2007-12-03T10:15:37.00Z", "public/east", 66, 9)
        // -> CRITICAL
    );

    final Iterator<EventProcessorInput> iterator = inputs.iterator();
    EventProcessorInput input;

    // Process each input and verify transitions (or not)

    processNextInput(eventProcessor, context, iterator);
    verifyHandlerNotCalled();

    input = processNextInput(eventProcessor, context, iterator);
    verifyHandlerCalled(null, TaskState.OK, null);

    processNextInput(eventProcessor, context, iterator);
    verifyHandlerNotCalled();

    input = processNextInput(eventProcessor, context, iterator);
    verifyHandlerCalled(
        TaskState.OK, TaskState.WARNING, WARNING_MESSAGE);

    processNextInput(eventProcessor, context, iterator);
    verifyHandlerNotCalled();

    input = processNextInput(eventProcessor, context, iterator);
    verifyHandlerCalled(
        TaskState.WARNING, TaskState.CRITICAL,
        "Usage of 91 is too high or idle of 50 is too low");

    processNextInput(eventProcessor, context, iterator);
    verifyHandlerNotCalled();

    input = processNextInput(eventProcessor, context, iterator);
    verifyHandlerCalled(
        TaskState.CRITICAL, TaskState.WARNING, WARNING_MESSAGE);

    processNextInput(eventProcessor, context, iterator);
    verifyHandlerNotCalled();

    input = processNextInput(eventProcessor, context, iterator);
    verifyHandlerCalled(
        TaskState.WARNING, TaskState.CRITICAL,
        "Usage of 66 is too high or idle of 9 is too low");
  }

  private EventProcessorInput processNextInput(EventProcessor eventProcessor, EventProcessorContext context,
                                               Iterator<EventProcessorInput> iterator) {
    EventProcessorInput input = iterator.next();
    eventProcessor.process(context, input, stateChangeHandler);
    return input;
  }

  private void verifyHandlerCalled(TaskState from, TaskState to,
                                   String expectedMessage) {
    verify(stateChangeHandler).handleStateChange(
        argThat(multiTransition -> {
          assertThat(multiTransition.getOverall().getFrom()).isEqualTo(from);
          assertThat(multiTransition.getOverall().getTo()).isEqualTo(to);
          return true;
        }),
        expectedMessage != null ? eq(expectedMessage) : isNull()
    );
    reset(stateChangeHandler);
  }

  private void verifyHandlerNotCalled() {
    verifyNoInteractions(stateChangeHandler);
  }

  private EventProcessorInput buildInput(String timestamp, String zone, int usage,
                                         int idle) {
    return new EventProcessorInput(
        Instant.parse(timestamp), zone, Map.of(
        "usage", usage,
        "idle", idle
    )
    );
  }

  private EventProcessorContext buildTestContext() {
    final String tenantId = randomAlphanumeric(10);
    final String resourceId = randomAlphanumeric(5);
    final String monitorId = randomAlphanumeric(5);

    final EventEngineTask task = new EventEngineTask()
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
                    .setState(TaskState.CRITICAL)
                    .setExpression(new LogicalExpression()
                        .setOperator(Operator.OR)
                        .setExpressions(List.of(
                            new ComparisonExpression()
                                .setInput("usage")
                                .setComparator(Comparator.GREATER_THAN)
                                .setComparisonValue(75),
                            new ComparisonExpression()
                                .setInput("idle")
                                .setComparator(Comparator.LESS_THAN_OR_EQUAL_TO)
                                .setComparisonValue(10)
                        ))
                    )
                    .setMessage("Usage of #{usage} is too high or idle of #{idle} is too low"),
                new StateExpression()
                    .setState(TaskState.WARNING)
                    .setExpression(new ComparisonExpression()
                        .setInput("usage")
                        .setComparator(Comparator.GREATER_THAN)
                        .setComparisonValue(50)
                    )
                    .setMessage(WARNING_MESSAGE)
            ))
        );

    return EventProcessorContextBuilder.fromTask(task);
  }
}