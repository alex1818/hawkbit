/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

/**
 * {@link Tag} of a {@link DistributionSet}.
 *
 */
public interface DistributionSetTag extends Tag {

    /**
     * @return {@link List} of {@link DistributionSet}s this {@link Tag} is
     *         assigned to.
     */
    List<DistributionSet> getAssignedToDistributionSet();

}
