package com.rackspace.salus.event.statemachines;

import java.util.Objects;

/**
 * A simple {@link StateHolder} that latches the last state processed.
 * @param <S> type of state value
 */
public class LatchingStateHolder<S> implements StateHolder<S> {

  S state;

  /**
   * @param input the incoming state to evaluate
   * @return the given input state if it differs from the previously latched state or null otherwise
   */
  @Override
  public StateTransition<S> process(S input) {
    if (!Objects.equals(state, input)) {
      final StateTransition<S> transition = new StateTransition<>(state, input);
      state = input;
      return transition;
    }
    return null;
  }

  @Override
  public S getState() {
    return state;
  }
}
