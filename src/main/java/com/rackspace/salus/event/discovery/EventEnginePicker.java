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

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * This interface is implemented by configurable strategies that are responsible for locating an
 * instance of the event engine (Kapacitor) for the given aspects of the current object to
 * be processed.
 */
public abstract class EventEnginePicker {
  private final HashFunction hashFunction;

  protected EventEnginePicker(HashFunction hashFunction) {
    this.hashFunction = hashFunction;
  }

  /**
   * Locates an instance that is "assigned" the given aspects. This most likely uses a consistent
   * hash strategy picking one of the known instances.
   * @param tenantId the tenant of the object to be evaulated
   * @param resourceId the resource ID of the data to be routed
   * @param collectionName the collection name (aka measurement) of the object
   * @return the host and port of the instance selected
   */
  public abstract EngineInstance pickRecipient(String tenantId, String resourceId, String collectionName)
      throws NoPartitionsAvailableException;

  /**
   * Provides all known instances for operations that need to access all
   * @return a collection of host-port pairs for all known instances
   */
  public abstract Collection<EngineInstance> pickAll();


  protected int pickPartition(String tenantId, String resourceId, String collectionName)
      throws NoPartitionsAvailableException {
    final int partitions = getPartitions();
    if (partitions <= 0) {
      throw new NoPartitionsAvailableException();
    }

    final HashCode hashCode = hashFunction.newHasher()
        .putString(tenantId, StandardCharsets.UTF_8)
        .putString(resourceId, StandardCharsets.UTF_8)
        .putString(collectionName, StandardCharsets.UTF_8)
        .hash();

    return Hashing.consistentHash(hashCode, partitions);
  }

  protected abstract int getPartitions();

}
