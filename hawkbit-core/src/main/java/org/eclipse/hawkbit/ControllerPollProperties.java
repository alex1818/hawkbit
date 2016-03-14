/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Defines the polling time for the controllers in HH:MM:SS notation.
 * 
 */
@Component
@ConfigurationProperties(prefix = "hawkbit.controller")
public class ControllerPollProperties {

    /**
     * Recommended target polling time for DDI API. Final choice is up to the
     * target.
     */
    private String pollingTime;

    /**
     * Assumed time frame where the target is considered overdue when no DDI
     * polling has been registered by the update server.
     */
    private String pollingOverdueTime;
    private String maxPollingTime = "23:59:00";
    private String minPollingTime = "00:00:30";

    public String getMaxPollingTime() {
        return maxPollingTime;
    }

    public void setMaxPollingTime(final String maxPollingTime) {
        this.maxPollingTime = maxPollingTime;
    }

    public String getMinPollingTime() {
        return minPollingTime;
    }

    public void setMinPollingTime(final String minPollingTime) {
        this.minPollingTime = minPollingTime;
    }

}
