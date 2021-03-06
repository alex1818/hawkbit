/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEvent;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;

/**
 * Class which contains the Event when selecting all entries of the
 * distributions table
 */
public class DistributionTableEvent extends BaseEntityEvent<DistributionSet> {

    /**
     * Constructor.
     * 
     * @param eventType
     *            the event type
     * @param entity
     *            the distribution set
     */
    public DistributionTableEvent(final BaseEntityEventType eventType, final DistributionSet entity) {
        super(eventType, entity);
    }

}
