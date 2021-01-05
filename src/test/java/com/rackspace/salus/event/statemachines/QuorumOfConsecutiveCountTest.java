package com.rackspace.salus.event.statemachines;

import static org.assertj.core.api.Assertions.assertThat;

import com.rackspace.salus.event.statemachines.ConsecutiveCountStateMachine.StateSpec;
import com.rackspace.salus.event.statemachines.MultiStateTransition.Observation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class QuorumOfConsecutiveCountTest {

  @Test
  void combination() {
    var stateSpecs = List.of(
        StateSpec.of("OK", 1),
        StateSpec.of("CRITICAL", 2)
    );

    final QuorumStateMachine<String, String> sm = new QuorumStateMachine<>(
        2,
        () -> new ConsecutiveCountStateMachine<>(stateSpecs)
    );

    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    assertThat(sm.process("zoneB", "CRITICAL"))
        .isNull();
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    assertThat(sm.process("zoneB", "CRITICAL"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(new StateTransition<>(null, "CRITICAL"))
                .setObservations(Map.of(
                    "zoneA", new Observation<>().setState("CRITICAL"),
                    "zoneB", new Observation<>().setState("CRITICAL")
                ))
        );
    assertThat(sm.process("zoneA", "OK"))
        .isNull();
    assertThat(sm.process("zoneB", "OK"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(new StateTransition<>("CRITICAL", "OK"))
                .setObservations(Map.of(
                    "zoneA", new Observation<>().setState("OK"),
                    "zoneB", new Observation<>().setState("OK")
                ))
        );
  }
}
