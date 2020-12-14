package com.rackspace.salus.event.statemachines;

import com.rackspace.salus.event.statemachines.MultiStateTransition.Observation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements a state-machine that maintains a {@link StateHolder} instance per entry
 * of type {@link E} where requested, quorum number of entries need to enter a new state for the
 * overall state to transition.
 * @param <S> type of state value
 * @param <E> type of quorum entry identifier
 */
public class QuorumStateMachine<S,E> {

  private final int quorum;
  private final StateHolderFactory<S> stateHolderFactory;
  private final Map<E, StateHolder<S>> entries = new ConcurrentHashMap<>();
  private final LatchingStateHolder<S> overall = new LatchingStateHolder<>();

  public QuorumStateMachine(int quorum,
                            StateHolderFactory<S> stateHolderFactory) {
    if (quorum <= 0) {
      throw new IllegalArgumentException("quorum must be greater than zero");
    }
    this.quorum = quorum;
    this.stateHolderFactory =
        stateHolderFactory != null ? stateHolderFactory : LatchingStateHolder::new;
  }

  public MultiStateTransition<S, E> process(E entry, S input) {
    if (entry == null) {
      throw new IllegalArgumentException("entry cannot be null");
    }
    if (input == null) {
      throw new IllegalArgumentException("input cannot be null");
    }

    final StateHolder<S> entryState = entries.computeIfAbsent(entry,
        e -> stateHolderFactory.create());

    final StateTransition<S> result = entryState.process(input);
    if (result != null) {
      return evaluateQuorum(result);
    } else {
      return null;
    }
  }

  private MultiStateTransition<S, E> evaluateQuorum(StateTransition<S> transition) {
    final long countInNewState = entries.values().stream()
        .map(StateHolder::getState)
        .filter(s -> s.equals(transition.getTo()))
        .count()
        ;

    if (countInNewState >= quorum) {
      final StateTransition<S> overall = this.overall.process(transition.getTo());
      if (overall != null) {
        return new MultiStateTransition<S,E>()
            .setOverall(overall)
            .setObservations(collectObservations());
      }
    }
    return null;
  }

  private Map<E, Observation<S>> collectObservations() {
    return entries.entrySet().stream()
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> new Observation<S>()
                .setState(entry.getValue().getState())
        ));
  }

}
