package com.rackspace.salus.event.statemachines;

import static org.assertj.core.api.Assertions.assertThat;

import com.rackspace.salus.event.statemachines.ConsecutiveCountStateMachine.StateSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsecutiveCountStateMachineTest {

  @Test
  void simple() {
    final ConsecutiveCountStateMachine<String> sm = new ConsecutiveCountStateMachine<>(List.of(
        StateSpec.of("a"),
        StateSpec.of("b"),
        StateSpec.of("c")
    ));

    assertThat(sm.process("a")).isNull();
    assertThat(sm.process("b"))
        .isNotNull()
        .isEqualTo(new StateTransition<>("a", "b"));
    assertThat(sm.process("b")).isNull();
    assertThat(sm.process("c"))
        .isNotNull()
        .isEqualTo(new StateTransition<>("b", "c"));
  }

  @Test
  void consecutiveCountStates() {
    final ConsecutiveCountStateMachine<String> sm = new ConsecutiveCountStateMachine<>(List.of(
        StateSpec.of("one", 1),
        StateSpec.of("two", 2),
        StateSpec.of("three", 3)
    ));

    // initial state, nothing
    assertThat(sm.process("one")).isNull();

    // two-state, one left
    assertThat(sm.process("two")).isNull();
    // two-state, transitioned
    assertThat(sm.process("two"))
        .isNotNull()
        .isEqualTo(new StateTransition<>("one", "two"));

    // repeat current state
    assertThat(sm.process("two")).isNull();

    // transition immediately to one-state
    assertThat(sm.process("one"))
        .isNotNull()
        .isEqualTo(new StateTransition<>("two", "one"));

    // three-state, 2 left
    assertThat(sm.process("three")).isNull();
    // three-state, 1 left
    assertThat(sm.process("three")).isNull();
    // three-state, transition
    assertThat(sm.process("three"))
        .isNotNull()
        .isEqualTo(new StateTransition<>("one", "three"));

    // repeat current state
    assertThat(sm.process("three")).isNull();
  }
}