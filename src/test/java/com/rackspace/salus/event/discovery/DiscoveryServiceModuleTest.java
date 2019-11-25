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

import static org.junit.Assert.assertThat;

import com.rackspace.salus.event.discovery.DiscoveryProperties.KubernetesStrategy;
import com.rackspace.salus.event.discovery.DiscoveryProperties.PortStrategy;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class DiscoveryServiceModuleTest {

  ConfigurableApplicationContext applicationContext = new StaticApplicationContext();

  @Test
  public void testPortStrategyConfigured() {
    PortStrategy portStrategy = new PortStrategy();
    DiscoveryProperties properties = new DiscoveryProperties();
    properties.setPortStrategy(portStrategy);

    final DiscoveryServiceModule discoveryServiceModule = new DiscoveryServiceModule(properties, applicationContext);
    final EventEnginePicker picker = discoveryServiceModule.eventEnginePicker();

    assertThat(picker, Matchers.instanceOf(PortStrategyPicker.class));
  }

  @Test
  public void testKubernetesStrategyConfigured() {
    KubernetesStrategy kubernetesStrategy = new KubernetesStrategy();
    DiscoveryProperties properties = new DiscoveryProperties();
    properties.setKubernetesStrategy(kubernetesStrategy);

    final DiscoveryServiceModule discoveryServiceModule = new DiscoveryServiceModule(properties, applicationContext);
    final EventEnginePicker picker = discoveryServiceModule.eventEnginePicker();

    assertThat(picker, Matchers.instanceOf(KubernetesServiceEndpointPicker.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testTooManyConfigured() {
    PortStrategy portStrategy = new PortStrategy();
    KubernetesStrategy kubernetesStrategy = new KubernetesStrategy();
    DiscoveryProperties properties = new DiscoveryProperties();
    properties.setPortStrategy(portStrategy);
    properties.setKubernetesStrategy(kubernetesStrategy);

    final DiscoveryServiceModule discoveryServiceModule = new DiscoveryServiceModule(properties, applicationContext);

    discoveryServiceModule.eventEnginePicker(); // should fail
  }

  @Test(expected = IllegalStateException.class)
  public void testNoneConfigured() {
    DiscoveryProperties properties = new DiscoveryProperties();

    final DiscoveryServiceModule discoveryServiceModule = new DiscoveryServiceModule(properties, applicationContext);

    discoveryServiceModule.eventEnginePicker(); // should fail
  }
}