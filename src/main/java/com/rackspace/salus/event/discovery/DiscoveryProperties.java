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

import javax.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("event.discovery")
@Data
public class DiscoveryProperties {

  int hashFunctionSeed = 0;

  PortStrategy portStrategy;

  KubernetesStrategy kubernetesStrategy;

  @Data
  public static class PortStrategy {
    int partitions = 1;

    int startingPort = 9192;

    String host = "localhost";
  }

  @Data
  public static class KubernetesStrategy {
    String apiUrl = "https://kubernetes";
    @NotEmpty
    String serviceName = "kapacitor";
    int readTimeout = 20;
  }
}
