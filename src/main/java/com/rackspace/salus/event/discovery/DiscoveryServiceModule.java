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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DiscoveryProperties.class)
public class DiscoveryServiceModule {

  private final DiscoveryProperties properties;

  @Autowired
  public DiscoveryServiceModule(DiscoveryProperties properties) {
    this.properties = properties;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Bean
  public EventEnginePicker eventEnginePicker() {
    // default kafka topic partitioning uses murmur, so that seems like a good choice here too
    final HashFunction hashFunction = Hashing.murmur3_128(properties.getHashFunctionSeed());

    if (properties.getPortStrategy() != null) {
      return new PortStrategyPicker(properties, hashFunction);
    } else {
      throw new IllegalStateException(
          "One of the pickers must be configured...only PortStrategy at this time");

    }
  }

}
