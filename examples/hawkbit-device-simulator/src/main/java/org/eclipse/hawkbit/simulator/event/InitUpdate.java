/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.event;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;

/**
 * Event description which indicates the initialization of an update.
 * 
 * @author Michael Hirsch
 *
 */
public class InitUpdate {

    private final AbstractSimulatedDevice device;

    /**
     * Creates new progress update event.
     * 
     * @param device
     *            the device which progress has been updated
     */
    public InitUpdate(final AbstractSimulatedDevice device) {
        this.device = device;
    }

    /**
     * @return the device of the event
     */
    public AbstractSimulatedDevice getDevice() {
        return device;
    }

}
