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

package com.rackspace.salus.event.common;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rackspace.salus.telemetry.model.LabelNamespaces;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TagsTest {

  @Parameters
  public static Collection<String> namespaces() {
    return Arrays.asList(
        Tags.MONITORING_SYSTEM,
        Tags.RESOURCE_ID,
        Tags.RESOURCE_LABEL,
        Tags.TENANT
    );
  }

  final String tag;

  public TagsTest(String tag) {
    this.tag = tag;
  }

  @Test
  public void testTagIsNamespaced() {
    assertThat(
        tag.contains(LabelNamespaces.EVENT_ENGINE_TAGS),
        is(true)
    );
  }
}