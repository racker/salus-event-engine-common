/*
 * Copyright 2020 Rackspace US, Inc.
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

package com.rackspace.salus.event.model.kapacitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class KapacitorEvent{
    private String id;
    private int duration;
    private String previousLevel;
    private EventData data;
    private String level;
    private boolean recoverable;
    private String details;
    private Date time;
    private String message;

    @Data
    public static class EventData {
        private List<SeriesItem> series;
    }

    @Data
    public static class SeriesItem{
        private String name;
        private List<String> columns;
        private List<Object> values;
    }

    // This should match the details defined by TickScriptBuilder.
    // TODO: might need to update this for more non-alphanumeric chars
    // TODO: is zone 'null' or blank for agent?
    // e.g. 537730-cpu-23cf9954-9b13-47e6-96b8-94b1c9b02009:system_monitor_id=1000-2222-3333-4444,
    //      system_monitoring_zone=null,system_resource_id=development:0
    public static final Pattern taskFieldsRegex = Pattern.compile(
        "^(?<tenantId>[a-zA-Z0-9\\_]+)\\-(?<measurement>[a-zA-Z]+)\\-(?<taskId>[a-zA-Z0-9\\-]+)\\:"
            + "[a-zA-Z\\_]+\\=(?<monitorId2>[a-zA-Z0-9\\-]+),"
            + "[a-zA-Z\\_]+\\=(?<zoneId>[a-zA-Z0-9\\-]+),"
            + "[a-zA-Z\\_]+\\=(?<resourceId>[a-zA-Z0-9\\-\\:]+)");

    /**
     * A unique key used to base state changes on.
     */
    public String getAlertGroupId() {
        return String.format("%s:%s:%s:%s",
            getTenantId(),
            getResourceId(),
            getMonitorId(),
            getTaskId());
    }

    /**
     * Get the tenant id of the event.
     *
     * Underscores are used in kapacitor ids as colons are invalid.
     *
     * @return The tenant id of the event.
     */
    public String getTenantId() {
        return taskFieldsRegex.matcher(this.id).group("tenantId")
            .replace('_', ':');
    }

    /**
     * @return The resource id of the event.
     */
    public String getResourceId() {
        return taskFieldsRegex.matcher(this.id).group("resourceId");
    }

    /**
     * @return The monitor id of the event.
     */
    public UUID getMonitorId() {
        String monitorId = taskFieldsRegex.matcher(this.id).group("monitorId");
        try {
            return UUID.fromString(monitorId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return The task id of the event.
     */
    public UUID getTaskId() {
        String taskId = taskFieldsRegex.matcher(this.id).group("taskId");
        try {
            return UUID.fromString(taskId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return The zone id of the event; null if an agent event.
     */
    public String getZoneId() {
        String zoneId = taskFieldsRegex.matcher(this.id).group("zoneId");
        return !Objects.equals(zoneId, "null") ? zoneId : null;
    }
}
