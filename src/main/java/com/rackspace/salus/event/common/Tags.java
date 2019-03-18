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

/**
 * Contains common tags applied to metrics sent to kapacitor. Kapacitor and the others in the
 * TICK stack use the term "tag", which is equivalent to our use of the term "label".
 */
public class Tags {
  public static final String RESOURCE_ID = "resourceId";
  public static final String ACCOUNT = "account";
  public static final String ACCOUNT_TYPE = "accountType";
  public static final String RESOURCE_LABEL = "resourceLabel";
  public static final String MONITORING_SYSTEM = "monitoringSystem";
}
