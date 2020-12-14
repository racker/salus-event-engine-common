package com.rackspace.salus.event.statemachines;

@FunctionalInterface
public interface StateHolderFactory<S> {
  StateHolder<S> create();
}
