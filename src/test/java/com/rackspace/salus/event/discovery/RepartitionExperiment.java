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
import com.google.common.hash.Hashing;
import com.rackspace.salus.event.discovery.DiscoveryProperties.PortStrategy;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is a tool for visually confirming the behavior of the Guava consistent hashing used within
 * the {@link EventEnginePicker} implementations.
 *
 * <p>
 *   The command line arguments are
 *   <ul>
 *     <li>tenantCount</li>
 *     <li>measurementCount</li>
 *     <li>startingPartitions</li>
 *     <li>endingPartitions</li>
 *   </ul>
 * </p>
 * <p>
 *   For example, when invoked with <code>100000 20 1 10</code> it prints out
 *   <pre>
 1 -> 2 : totalEntries=2000000, moves=1000287 movePercent=50.0 partitionRatio=50.0
 2 -> 3 : totalEntries=2000000, moves=666258 movePercent=33.3 partitionRatio=33.3
 3 -> 4 : totalEntries=2000000, moves=500438 movePercent=25.0 partitionRatio=25.0
 4 -> 5 : totalEntries=2000000, moves=400572 movePercent=20.0 partitionRatio=20.0
 5 -> 6 : totalEntries=2000000, moves=334282 movePercent=16.7 partitionRatio=16.7
 6 -> 7 : totalEntries=2000000, moves=285828 movePercent=14.3 partitionRatio=14.3
 7 -> 8 : totalEntries=2000000, moves=250411 movePercent=12.5 partitionRatio=12.5
 8 -> 9 : totalEntries=2000000, moves=222682 movePercent=11.1 partitionRatio=11.1
 9 -> 10 : totalEntries=2000000, moves=199676 movePercent=10.0 partitionRatio=10.0
 *   </pre>
 * </p>
 */
public class RepartitionExperiment {

  public static void main(String[] args) {
    if (args.length < 4) {
      throw new IllegalArgumentException("Requires: tenantCount measurementCount startingPartitions endingPartitions");
    }

    final int tenantCount = Integer.parseInt(args[0]);
    final int measurementCount = Integer.parseInt(args[1]);
    final int startingPartitions = Integer.parseInt(args[2]);
    final int endingPartitions = Integer.parseInt(args[3]);

    final HashFunction hashFunction = Hashing.murmur3_128();
    final DiscoveryProperties properties = new DiscoveryProperties();
    properties.setPartitions(startingPartitions);
    properties.setPortStrategy(new PortStrategy());
    properties.getPortStrategy().setHost("host");
    properties.getPortStrategy().setStartingPort(9000);

    final EventEnginePicker eventEnginePicker = new PortStrategyPicker(properties, hashFunction);

    final List<String> tenants = IntStream.range(0, tenantCount)
        .mapToObj(value -> UUID.randomUUID().toString())
        .collect(Collectors.toList());

    final List<String> measurements = IntStream.range(0, measurementCount)
        .mapToObj(value -> UUID.randomUUID().toString())
        .collect(Collectors.toList());

    for (int p = startingPartitions; p < endingPartitions; p++) {
      runScenario(p, p+1, eventEnginePicker, tenants, measurements);
    }
  }

  private static void runScenario(int fromPartitions, int toPartitions,
                                  EventEnginePicker eventEnginePicker, List<String> tenants,
                                  List<String> measurements) {
    long total = 0;
    long moves = 0;

    for (String tenant : tenants) {
      for (String measurement : measurements) {
        ++total;

        final EngineMove engineMove = eventEnginePicker
            .computeMove(tenant, measurement, fromPartitions, toPartitions);
        if (engineMove != null) {
          ++moves;
        }
      }
    }

    System.out.printf("%d -> %d : totalEntries=%d, moves=%d movePercent=%.1f partitionRatio=%.1f%n",
        fromPartitions, toPartitions, total, moves, (float)moves/total * 100f, (float)(toPartitions - fromPartitions)/toPartitions * 100f);
  }
}
