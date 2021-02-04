package com.rackspace.salus.event.statemachines;

/**
 *
 * @param <S> type of state value
 */
public interface StateHolder<S> {

  /**
   * Processes an incoming state.
   * @param input the incoming state to evaluate
   * @return if non-null, indicates that the state transitioned to the returned state and a
   * subsequent call to {@link #getState()} will return this same state
   */
  StateTransition<S> process(S input);

  /**
   * @return the currently held state or null if indeterminate
   */
  S getState();
}
