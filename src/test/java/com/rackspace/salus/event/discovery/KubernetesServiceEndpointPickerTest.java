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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.hash.Hashing;
import com.google.gson.reflect.TypeToken;
import com.rackspace.salus.event.discovery.DiscoveryProperties.KubernetesStrategy;
import io.kubernetes.client.JSON;
import io.kubernetes.client.models.V1Endpoints;
import io.kubernetes.client.util.Watch.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.util.StreamUtils;

public class KubernetesServiceEndpointPickerTest {

  public static final Type WATCH_ENDPOINTS_TYPE = new TypeToken<Response<V1Endpoints>>() {
  }.getType();
  private KubernetesServiceEndpointPicker picker;
  private JSON jsonParser;

  @Before
  public void setUp() {
    //noinspection UnstableApiUsage
    picker = new KubernetesServiceEndpointPicker(
        new KubernetesStrategy(),
        Hashing.murmur3_128(),
        new SyncTaskExecutor()
    );
    jsonParser = new JSON();
  }

  @Test
  public void testStartup() throws IOException {
    final Response<V1Endpoints> response = parseResponse("added");

    picker.handleWatchResponse(response);

    final Collection<EngineInstance> allInstances = picker.pickAll();
    assertThat(allInstances, hasSize(2));
    assertThat(
        allInstances,
        contains(
            allOf(
                hasProperty("host", equalTo("kapacitor-0")),
                hasProperty("port", equalTo(9092))
            ),
            allOf(
                hasProperty("host", equalTo("kapacitor-1")),
                hasProperty("port", equalTo(9092))
            )
        )
    );
  }

  @Test
  public void testInstanceDown() throws IOException {
    picker.handleWatchResponse(parseResponse("added"));
    picker.handleWatchResponse(parseResponse("modified-down")); // kapacitor-0 went down

    final Collection<EngineInstance> allInstances = picker.pickAll();
    assertThat(allInstances, hasSize(1));
    assertThat(
        allInstances,
        contains(
            allOf(
                hasProperty("host", equalTo("kapacitor-1")),
                hasProperty("port", equalTo(9092))
            )
        )
    );
  }

  @Test
  public void testServiceDeleted() throws IOException {
    picker.handleWatchResponse(parseResponse("added"));
    picker.handleWatchResponse(parseResponse("deleted"));

    final Collection<EngineInstance> allInstances = picker.pickAll();
    assertThat(allInstances, hasSize(0));
  }

  private Response<V1Endpoints> parseResponse(String name) throws IOException {
    final ClassPathResource resource = new ClassPathResource(
        String.format("/endpoints-watches/%s.json", name));

    try (InputStream in = resource.getInputStream()) {
      final String jsonText = StreamUtils.copyToString(in, StandardCharsets.UTF_8);

      return jsonParser.deserialize(jsonText, WATCH_ENDPOINTS_TYPE);
    }
  }
}