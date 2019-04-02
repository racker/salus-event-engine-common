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

import com.google.common.hash.HashFunction;
import com.google.common.net.HostAndPort;
import com.rackspace.salus.event.discovery.DiscoveryProperties.PortStrategy;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This {@link EventEnginePicker} uses a simple port-offset strategy based on the replica instance
 * number. The configured host will be used for all of the computed {@link HostAndPort} results.
 */
@SuppressWarnings("UnstableApiUsage")
class PortStrategyPicker extends EventEnginePicker {

  private final PortStrategy properties;

  PortStrategyPicker(PortStrategy properties, HashFunction hashFunction) {
    super(hashFunction);
    this.properties = properties;
  }

  @Override
  public EngineInstance pickRecipient(String tenantId, String resourceId,
                                      String collectionName) throws NoPartitionsAvailableException {
    final int choice = pickPartition(tenantId, resourceId, collectionName);

    final int port = properties.getStartingPort() + choice;

    return new EngineInstance(properties.getHost(), port, choice);
  }

  @Override
  public Collection<EngineInstance> pickAll() {
    return IntStream.range(0, properties.getPartitions())
        .mapToObj(partition -> new EngineInstance(
            properties.getHost(),
            partition+ properties.getStartingPort(),
            partition
        ))
        .collect(Collectors.toList());
  }

  @Override
  protected int getPartitions() {
    return properties.getPartitions();
  }

}
