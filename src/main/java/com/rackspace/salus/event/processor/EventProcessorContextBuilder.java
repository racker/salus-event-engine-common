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

import com.rackspace.salus.event.statemachines.ConsecutiveCountStateMachine;
import com.rackspace.salus.event.statemachines.ConsecutiveCountStateMachine.StateSpec;
import com.rackspace.salus.event.statemachines.QuorumStateMachine;
import com.rackspace.salus.event.statemachines.StateHolder;
import com.rackspace.salus.telemetry.entities.EventEngineTask;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.TaskState;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EventProcessorContextBuilder {

  public static EventProcessorContext fromTask(EventEngineTask task) {
    return new EventProcessorContext(
        task,
        buildZonedStateMachine(task.getTaskParameters())
    );
  }

  private static QuorumStateMachine<TaskState, String> buildZonedStateMachine(
      EventEngineTaskParameters taskParameters) {
    return new QuorumStateMachine<>(
        taskParameters.getZoneQuorumCount(),
        () -> buildPerLevelStateMachine(taskParameters)
    );
  }

  private static StateHolder<TaskState> buildPerLevelStateMachine(
      EventEngineTaskParameters taskParameters) {
    // For now, all states share the default consecutive count declaration.
    // This code can be later adapted to perform asymmetric consecutive-count handling
    // when a 'count' field is added to StateExpression in EventEngineTaskParameters.
    return new ConsecutiveCountStateMachine<>(
        Arrays.stream(TaskState.values())
            .map(taskState -> StateSpec.of(taskState, taskParameters.getDefaultConsecutiveCount()))
            .collect(Collectors.toList())
    );
  }

}
