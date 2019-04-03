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
import com.google.gson.reflect.TypeToken;
import com.rackspace.salus.event.discovery.DiscoveryProperties.KubernetesStrategy;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1EndpointAddress;
import io.kubernetes.client.models.V1EndpointPort;
import io.kubernetes.client.models.V1EndpointSubset;
import io.kubernetes.client.models.V1Endpoints;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;

@Slf4j
public class KubernetesServiceEndpointPicker extends EventEnginePicker implements SmartLifecycle {

  private final KubernetesStrategy properties;
  private final TaskExecutor taskExecutor;
  private ApiClient apiClient;

  // NOTE: access to this MUST be synchronized
  private final List<EngineInstance> engineInstances = new ArrayList<>();

  KubernetesServiceEndpointPicker(DiscoveryProperties.KubernetesStrategy properties,
                                  @SuppressWarnings("UnstableApiUsage") HashFunction hashFunction,
                                  TaskExecutor taskExecutor) {
    super(hashFunction);
    this.properties = properties;
    this.taskExecutor = taskExecutor;
  }

  @Override
  public EngineInstance pickRecipient(String tenantId, String resourceId, String collectionName)
      throws NoPartitionsAvailableException {
    synchronized (engineInstances) {
      final int choice = pickPartition(tenantId, resourceId, collectionName);

      return engineInstances.get(choice);
    }
  }

  @Override
  public Collection<EngineInstance> pickAll() {
    synchronized (engineInstances) {
      return new ArrayList<>(engineInstances);
    }
  }

  @Override
  protected int getPartitions() {
    synchronized (engineInstances) {
      return engineInstances.size();
    }
  }

  @Override
  public void start() {
    log.info("Starting");

    try {
      apiClient = Config.defaultClient();
      // See https://github.com/kubernetes-client/java/issues/150#issuecomment-352514928
      // Disabling the timeout entirely is what was needed to implement a long-lived watch
      // For reference, kubectl defaults to 0 `--request-timeout='0'`
      apiClient.getHttpClient().setReadTimeout(0, TimeUnit.SECONDS);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to configure default kubernetes client", e);
    }

    taskExecutor.execute(this::watchEndpoint);
  }

  private void watchEndpoint() {
    if (apiClient == null) {
      return;
    }

    final CoreV1Api api = new CoreV1Api(apiClient);

    try {
      Watch<V1Endpoints> endpointsWatch = createWatch(api);

      while (apiClient != null && endpointsWatch.hasNext()) {
        final Response<V1Endpoints> response = endpointsWatch.next();

        handleWatchResponse(response);
      }

      log.debug("Finished watching");

    } catch (ApiException|RuntimeException e) {
      log.warn("Failed during endpoints watch", e);
      taskExecutor.execute(this::watchEndpoint);
    }

  }

  private Watch<V1Endpoints> createWatch(CoreV1Api api) throws ApiException {
    return Watch.createWatch(
            apiClient,
            api.listNamespacedEndpointsCall(
                "default",
                false, null, null,
                String.format("metadata.name=%s", properties.getServiceName()),
                null, null, null, null, true, null, null
            ),
            new TypeToken<Response<V1Endpoints>>() {
            }.getType()
        );
  }

  @SuppressWarnings("WeakerAccess") // for unit testing
  void handleWatchResponse(Response<V1Endpoints> response) {
    log.debug("Got endpoints response type={}", response.type);

    switch (response.type) {
      case "ADDED":
      case "MODIFIED":
        updateInstances(response.object.getSubsets());
        break;

      case "DELETED":
        clearInstances();
        break;
    }
  }

  private void updateInstances(List<V1EndpointSubset> subsets) {

    synchronized (engineInstances) {
      engineInstances.clear();

      for (V1EndpointSubset subset : subsets) {
        for (V1EndpointPort endpointPort : subset.getPorts()) {
          final int port = endpointPort.getPort();

          for (V1EndpointAddress endpointAddress : subset.getAddresses()) {

            engineInstances.add(
                new EngineInstance(
                    endpointAddress.getHostname(),
                    port,
                    engineInstances.size()
                )
            );

          }
        }
      }

      if (log.isDebugEnabled()) {
        log.debug("Updated engine instances={}", new ArrayList<>(engineInstances));
      }
    }
  }

  private void clearInstances() {
    synchronized (engineInstances) {
      engineInstances.clear();
    }
  }

  @Override
  public void stop() {
    log.info("Stopping");
    apiClient = null;
  }

  @Override
  public boolean isRunning() {
    return apiClient != null;
  }
}
