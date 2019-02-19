/*
 * Copyright 2019 Rackspace US, Inc.
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

package com.rackspace.salus.event.discovery;

import java.util.Collection;

/**
 * This interface is implemented by configurable strategies that are responsible for locating an
 * instance of the event engine (Kapacitor) for the given aspects of the current object to
 * be processed.
 */
public interface EventEnginePicker {

  /**
   * Locates an instance that is "assigned" the given aspects. This most likely uses a consistent
   * hash strategy picking one of the known instances.
   * @param tenantId the tenant of the object to be evaulated
   * @param collectionName the collection name (aka measurement) of the object
   * @return the host and port of the instance selected
   */
  EngineInstance pickRecipient(String tenantId, String collectionName);

  /**
   * Provides all known instances for operations that need to access all
   * @return a collection of host-port pairs for all known instances
   */
  Collection<EngineInstance> pickAll();

  /**
   * Resolves the {@link EngineInstance} for the requested partition. This is useful for
   * resolving the host and port to contact given the persisted knowledge about an assigned
   * partition.
   * @param partition the engine partition
   * @return the full engine instance details for the given partition
   */
  EngineInstance pickUsingPartition(int partition);

  /**
   * Computes if a move of assigned engine instance partition is needed for the gjven
   * inputs going from one partition cound to another partition count.
   * @param fromPartitions the prior partition count
   * @param toPartitions the new partition count
   * @return a non-null move description if a move is needed
   */
  EngineMove computeMove(String tenantId, String collectionName, int fromPartitions, int toPartitions);
}
