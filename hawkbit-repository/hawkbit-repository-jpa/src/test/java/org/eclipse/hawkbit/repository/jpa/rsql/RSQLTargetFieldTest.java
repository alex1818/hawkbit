/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter target")
public class RSQLTargetFieldTest extends AbstractJpaIntegrationTest {

    @Before
    public void seuptBeforeTest() {

        final DistributionSet ds = testdataFactory.createDistributionSet("AssignedDs");

        final Target target = entityFactory.generateTarget("targetId123");
        target.setDescription("targetId123");
        final TargetInfo targetInfo = target.getTargetInfo();
        targetInfo.getControllerAttributes().put("revision", "1.1");
        ((JpaTargetInfo) target.getTargetInfo()).setUpdateStatus(TargetUpdateStatus.PENDING);

        targetManagement.createTarget(target);
        final JpaTarget target2 = new JpaTarget("targetId1234");
        target2.setDescription("targetId1234");
        final TargetInfo targetInfo2 = new JpaTargetInfo(target2);
        targetInfo2.getControllerAttributes().put("revision", "1.2");
        target2.setTargetInfo(targetInfo2);
        targetManagement.createTarget(target2);
        targetManagement.createTarget(new JpaTarget("targetId1235"));
        targetManagement.createTarget(new JpaTarget("targetId1236"));

        final TargetTag targetTag = tagManagement.createTargetTag(new JpaTargetTag("Tag1"));
        tagManagement.createTargetTag(new JpaTargetTag("Tag2"));
        tagManagement.createTargetTag(new JpaTargetTag("Tag3"));
        tagManagement.createTargetTag(new JpaTargetTag("Tag4"));

        targetManagement.assignTag(Arrays.asList(target.getControllerId(), target2.getControllerId()), targetTag);

        deploymentManagement.assignDistributionSet(ds.getId(), target.getControllerId());
    }

    @Test
    @Description("Test filter target by (controller) id")
    public void testFilterByParameterId() {
        assertRSQLQuery(TargetFields.ID.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.ID.name() + "==target*", 4);
        assertRSQLQuery(TargetFields.ID.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.ID.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.ID.name() + "=out=(targetId123,notexist)", 3);
    }

    @Test
    @Description("Test filter target by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(TargetFields.NAME.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.NAME.name() + "==target*", 4);
        assertRSQLQuery(TargetFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.NAME.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.NAME.name() + "=out=(targetId123,notexist)", 3);
    }

    @Test
    @Description("Test filter target by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==target*", 2);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "=out=(targetId123,notexist)", 1);
    }

    @Test
    @Description("Test filter target by controller id")
    public void testFilterByParameterControllerId() {
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==target*", 4);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "=out=(targetId123,notexist)", 3);
    }

    @Test
    @Description("Test filter target by status")
    public void testFilterByParameterUpdateStatus() {
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "==pending", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "!=pending", 3);
        try {
            assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "==noExist*", 0);
            fail("RSQLParameterUnsupportedFieldException was expected since update status unknown");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            // test ok - exception was excepted
        }
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=in=(pending,error)", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=out=(pending,error)", 3);
    }

    @Test
    @Description("Test filter target by attribute")
    public void testFilterByAttribute() {
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1.1", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1*", 2);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==noExist*", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=in=(1.1,notexist)", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=out=(1.1)", 1);
    }

    @Test
    @Description("Test filter target by assigned ds name")
    public void testFilterByAssignedDsName() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==AssignedDs", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==A*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==noExist*", 0);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=in=(AssignedDs,notexist)", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=out=(AssignedDs,notexist)", 0);
    }

    @Test
    @Description("Test filter target by assigned ds version")
    public void testFilterByAssignedDsVersion() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==" + TestdataFactory.DEFAULT_VERSION, 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==*1*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==noExist*", 0);
        assertRSQLQuery(
                TargetFields.ASSIGNEDDS.name() + ".version=in=(" + TestdataFactory.DEFAULT_VERSION + ",notexist)", 1);
        assertRSQLQuery(
                TargetFields.ASSIGNEDDS.name() + ".version=out=(" + TestdataFactory.DEFAULT_VERSION + ",notexist)", 0);
    }

    @Test
    @Description("Test filter target by tag")
    public void testFilterByTag() {
        assertRSQLQuery(TargetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "==T*", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "=out=(Tag1,notexist)", 0);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expcetedTargets) {
        final Page<Target> findTargetPage = targetManagement.findTargetsAll(rsqlParam, new PageRequest(0, 100));
        final long countTargetsAll = findTargetPage.getTotalElements();
        assertThat(findTargetPage).isNotNull();
        assertThat(countTargetsAll).isEqualTo(expcetedTargets);
    }
}
