package com.atlassian.jira.plugins.slack.compat;

import com.atlassian.jira.project.Project;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.SingleValueOperand;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.*")
@PrepareForTest({ServiceDeskCompatibilityDispatcher.class})
public class FixRequestTypeOperandVisitorTest {
    @Mock
    Project project;
    @Mock
    ServiceDeskCompatibilityDispatcher dispatcher;
    @Mock
    ServiceDeskCompatibilityHelper helper;

    FixRequestTypeOperandVisitor target;

    @Before
    public void setUp() throws Exception {
        target = new FixRequestTypeOperandVisitor(project);
    }

    @Test
    public void acceptSubstitutesRequestTypeName() {
        String sdName = "someSdName";
        String sdKey = "someSdKey";

        PowerMockito.mockStatic(ServiceDeskCompatibilityDispatcher.class);
        when(ServiceDeskCompatibilityDispatcher.getInstance()).thenReturn(dispatcher);
        when(dispatcher.isServiceDeskInstalled()).thenReturn(true);
        when(dispatcher.getHelper()).thenReturn(Optional.of(helper));
        when(helper.buildRequestTypeNameToKeyMap(project)).thenReturn(ImmutableMap.of(sdName, sdKey));
        SingleValueOperand operand = new SingleValueOperand(sdName);

        Operand fixedOperand = operand.accept(target);

        assertThat(fixedOperand, notNullValue());
        assertThat(fixedOperand.getDisplayString(), equalTo("\"" + sdKey + "\""));
    }
}
