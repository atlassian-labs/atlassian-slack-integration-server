package com.atlassian.jira.plugins.slack.servicedesk;

import com.atlassian.jira.project.Project;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.SingleValueOperand;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FixRequestTypeOperandVisitorTest {
    @Mock
    Project project;
    @Mock
    ServiceDeskCompatibilityDispatcher dispatcher;
    @Mock
    ServiceDeskCompatibilityHelper helper;

    private MockedStatic<ServiceDeskCompatibilityDispatcher> mockedDispatcher;
    FixRequestTypeOperandVisitor target;

    @Before
    public void setUp() {
        target = new FixRequestTypeOperandVisitor(project);
    }

    @After
    public void tearDown() {
        if (mockedDispatcher != null) {
            mockedDispatcher.close();
        }
    }

    @Test
    public void acceptSubstitutesRequestTypeName() {
        String sdName = "someSdName";
        String sdKey = "someSdKey";

        mockedDispatcher = mockStatic(ServiceDeskCompatibilityDispatcher.class);
        mockedDispatcher.when(ServiceDeskCompatibilityDispatcher::getInstance).thenReturn(dispatcher);
        when(dispatcher.isServiceDeskInstalled()).thenReturn(true);
        when(dispatcher.getHelper()).thenReturn(Optional.of(helper));
        when(helper.buildRequestTypeNameToKeyMap(project)).thenReturn(ImmutableMap.of(sdName, sdKey));
        SingleValueOperand operand = new SingleValueOperand(sdName);

        Operand fixedOperand = operand.accept(target);

        assertThat(fixedOperand, notNullValue());
        assertThat(fixedOperand.getDisplayString(), equalTo("\"" + sdKey + "\""));
    }
}
