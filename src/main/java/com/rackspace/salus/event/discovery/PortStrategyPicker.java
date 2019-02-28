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
import com.google.common.net.HostAndPort;
import com.rackspace.salus.event.discovery.DiscoveryProperties.PortStrategy;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This {@link EventEnginePicker} uses a simple port-offset strategy based on the replica instance
 * number. The configured host will be used for all of the computed {@link HostAndPort} results.
 */
@SuppressWarnings("UnstableApiUsage")
class PortStrategyPicker implements EventEnginePicker {

  private final HashFunction hashFunction;
  private final DiscoveryProperties properties;

  PortStrategyPicker(DiscoveryProperties properties, HashFunction hashFunction) {
    this.properties = properties;
    this.hashFunction = hashFunction;
  }

  @Override
  public EngineInstance pickRecipient(String tenantId, String resourceId,
                                      String collectionName) {
    final HashCode hashCode = hashFunction.newHasher()
        .putString(tenantId, StandardCharsets.UTF_8)
        .putString(resourceId, StandardCharsets.UTF_8)
        .putString(collectionName, StandardCharsets.UTF_8)
        .hash();

    final int choice = Hashing.consistentHash(hashCode, properties.getPartitions());

    final int port = properties.getPortStrategy().getStartingPort() + choice;

    return new EngineInstance(properties.getPortStrategy().getHost(), port, choice);
  }

  @Override
  public Collection<EngineInstance> pickAll() {
    final PortStrategy portStrategy = properties.getPortStrategy();

    return IntStream.range(0, properties.getPartitions())
        .mapToObj(partition -> new EngineInstance(
            portStrategy.getHost(),
            partition+ portStrategy.getStartingPort(),
            partition
        ))
        .collect(Collectors.toList());
  }

  @Override
  public EngineInstance pickUsingPartition(int partition) {
    final PortStrategy portStrategy = properties.getPortStrategy();
    return new EngineInstance(
        portStrategy.getHost(),
        portStrategy.getStartingPort() + partition,
        partition
    );
  }

  @Override
  public EngineMove computeMove(String tenantId, String collectionName, int fromPartitions,
                                int toPartitions) {
    final HashCode hashCode = hashFunction.newHasher()
        .putString(tenantId, StandardCharsets.UTF_8)
        .putString(collectionName, StandardCharsets.UTF_8)
        .hash();

    final int fromChoice = Hashing.consistentHash(hashCode, fromPartitions);
    final int toChoice = Hashing.consistentHash(hashCode, toPartitions);

    if (fromChoice != toChoice) {
      return new EngineMove(
          pickUsingPartition(fromChoice),
          pickUsingPartition(toChoice)
      );
    }
    else {
      return null;
    }
  }
}
