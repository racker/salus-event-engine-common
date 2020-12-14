package com.rackspace.salus.event.statemachines;

import static org.assertj.core.api.Assertions.assertThat;

import com.rackspace.salus.event.statemachines.MultiStateTransition.Observation;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuorumStateMachineTest {

  @Test
  void processPartialQuorum() {
    QuorumStateMachine<String, String> sm = new QuorumStateMachine<>(
        2,
        LatchingStateHolder::new
    );

    // ZoneA | ZoneB | ZoneC | ZoneD -> internal state

    // *CRITICAL | ? | ? | ? -> ?
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    // CRITICAL | ? | ? | ? -> ?
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    // CRITICAL | *CRITICAL | ? | ? -> CRITICAL
    assertThat(sm.process("zoneB", "CRITICAL"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(transition(null, "CRITICAL"))
                .setObservations(Map.of(
                    "zoneA", observation("CRITICAL"),
                    "zoneB", observation("CRITICAL")
                ))
        );
    // CRITICAL | CRITICAL | *CRITICAL | ? -> CRITICAL
    assertThat(sm.process("zoneC", "CRITICAL"))
        .isNull();
    // CRITICAL | CRITICAL | CRITICAL | *WARNING -> CRITICAL
    assertThat(sm.process("zoneD", "WARNING"))
        .isNull();
    // *WARNING | CRITICAL | CRITICAL | WARNING -> WARNING
    assertThat(sm.process("zoneA", "WARNING"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(transition("CRITICAL", "WARNING"))
                .setObservations(Map.of(
                    "zoneA", observation("WARNING"),
                    "zoneB", observation("CRITICAL"),
                    "zoneC", observation("CRITICAL"),
                    "zoneD", observation("WARNING")
                ))
        );
  }

  private StateTransition<Object> transition(String from, String to) {
    return new StateTransition<>(from, to);
  }

  private Observation<Object> observation(String state) {
    return new Observation<>().setState(state);
  }

  @Test
  void processQuorumOfOne() {
    QuorumStateMachine<String, String> sm = new QuorumStateMachine<>(
        1,
        LatchingStateHolder::new
    );

    // ZoneA | ZoneB | ZoneC | ZoneD -> internal state

    // *CRITICAL | ? | ? | ? -> ?
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(transition(null, "CRITICAL"))
                .setObservations(Map.of(
                    "zoneA", observation("CRITICAL")
                ))
        );
    // CRITICAL | ? | ? | ? -> ?
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    // CRITICAL | *CRITICAL | ? | ? -> CRITICAL
    assertThat(sm.process("zoneB", "CRITICAL"))
        .isNull();
    // CRITICAL | CRITICAL | *CRITICAL | ? -> CRITICAL
    assertThat(sm.process("zoneC", "CRITICAL"))
        .isNull();
    // CRITICAL | CRITICAL | CRITICAL | *WARNING -> WARNING
    assertThat(sm.process("zoneD", "WARNING"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(transition("CRITICAL", "WARNING"))
                .setObservations(Map.of(
                    "zoneA", observation("CRITICAL"),
                    "zoneB", observation("CRITICAL"),
                    "zoneC", observation("CRITICAL"),
                    "zoneD", observation("WARNING")
                ))
        );
    // *WARNING | CRITICAL | CRITICAL | WARNING -> WARNING
    assertThat(sm.process("zoneA", "WARNING"))
        .isNull();
  }

  @Test
  void processQuorumOfAll() {
    QuorumStateMachine<String, String> sm = new QuorumStateMachine<>(
        4,
        LatchingStateHolder::new
    );

    // ZoneA | ZoneB | ZoneC | ZoneD -> internal state

    // *CRITICAL | ? | ? | ? -> ?
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    // CRITICAL | ? | ? | ? -> ?
    assertThat(sm.process("zoneA", "CRITICAL"))
        .isNull();
    // CRITICAL | *CRITICAL | ? | ? -> ?
    assertThat(sm.process("zoneB", "CRITICAL"))
        .isNull();
    // CRITICAL | CRITICAL | *CRITICAL | ? -> ?
    assertThat(sm.process("zoneC", "CRITICAL"))
        .isNull();
    // CRITICAL | CRITICAL | CRITICAL | *WARNING -> ?
    assertThat(sm.process("zoneD", "WARNING"))
        .isNull();
    // CRITICAL | CRITICAL | CRITICAL | *CRITICAL -> CRITICAL
    assertThat(sm.process("zoneD", "CRITICAL"))
        .isNotNull()
        .isEqualTo(
            new MultiStateTransition<>()
                .setOverall(transition(null, "CRITICAL"))
                .setObservations(Map.of(
                    "zoneA", observation("CRITICAL"),
                    "zoneB", observation("CRITICAL"),
                    "zoneC", observation("CRITICAL"),
                    "zoneD", observation("CRITICAL")
                ))
        );
    // *WARNING | CRITICAL | CRITICAL | CRITICAL -> CRITICAL
    assertThat(sm.process("zoneA", "WARNING"))
        .isNull();
  }
}