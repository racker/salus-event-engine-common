package com.rackspace.salus.event.statemachines;

import java.util.List;

/**
 * Implements a state machine where a given state needs to be observed a defined number of times
 * in a row before the overall state transitions there.
 * <p>
 * <img src="consecutive-count-state-machine.drawio.png"/>
 * </p>
 * @param <S> type of state value
 */
public class ConsecutiveCountStateMachine<S> implements StateHolder<S> {

  final List<StateSpec<S>> specs;
  S currentState;
  PendingState<S> pendingState;

  /**
   * Creates a consecutive-count state machine.
   * @param specs the possible states to process and the desired consecutive count for each
   */
  public ConsecutiveCountStateMachine(List<StateSpec<S>> specs) {
    if (specs == null || specs.isEmpty()) {
      throw new IllegalArgumentException("specs must be non-empty");
    }
    this.specs = specs;
    currentState = specs.get(0).getState();
  }

  public StateTransition<S> process(S input) {
    if (input == null) {
      throw new IllegalArgumentException("input cannot be null");
    }
    if (input.equals(currentState)) {
      pendingState = null;
      return null;
    }

    if (pendingState == null || !input.equals(pendingState.spec.state)) {
      final StateSpec<S> next = specs.stream()
          .filter(stateSpec -> stateSpec.state.equals(input))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown state name"));

      pendingState = new PendingState<S>(next);
    }

    --pendingState.remainder;

    if (pendingState.remainder <= 0) {
      final StateTransition<S> transition = new StateTransition<>(
          currentState, pendingState.spec.state);
      currentState = pendingState.spec.getState();
      pendingState = null;
      return transition;
    } else {
      return null;
    }
  }

  @Override
  public S getState() {
    return currentState;
  }

  public static class StateSpec<S> {

    final S state;
    final int consecutiveCount;

    private StateSpec(S state, int consecutiveCount) {
      if (state == null) {
        throw new IllegalArgumentException("name is required");
      }
      if (consecutiveCount < 1) {
        throw new IllegalArgumentException("consecutiveCount must be greater than 0");
      }
      this.state = state;
      this.consecutiveCount = consecutiveCount;
    }

    public static <S> StateSpec<S> of(S name) {
      return new StateSpec<>(name, 1);
    }

    public static <S> StateSpec<S> of(S name, int consecutiveCount) {
      return new StateSpec<>(name, consecutiveCount);
    }

    public S getState() {
      return state;
    }
  }

  static class PendingState<S> {

    StateSpec<S> spec;
    int remainder;

    public PendingState(StateSpec<S> spec) {
      this.spec = spec;
      remainder = spec.consecutiveCount;
    }

  }

}