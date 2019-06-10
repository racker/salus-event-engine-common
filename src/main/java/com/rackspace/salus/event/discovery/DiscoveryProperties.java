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

    /**
     * Specifies the endpoint where the Kubernetes API can be accessed. The default is the typical
     * URL that works for pods deployed within the cluster to be queried. The service account
     * that accesses the API will need to be given get, list, and watch verb access to the resources
     * endpoints and services.
     * Can also be set with the env variable EVENT_DISCOVERY_KUBERNETESSTRATEGY_APIURL
     */
    String apiUrl = "https://kubernetes";
    /**
     * Can also be set with the env variable EVENT_DISCOVERY_KUBERNETESSTRATEGY_SERVICENAME
     */
    @NotEmpty
    String serviceName = "kapacitor";
    int readTimeout = 20;
  }
}
