package com.atlassian.jira.plugins.slack.compat;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class ServiceDeskCompatibilityDispatcherTest {
    ServiceDeskCompatibilityDispatcher target;

    @Before
    public void setUp() throws Exception {
        target = new ServiceDeskCompatibilityDispatcher();
    }

    @Test
    public void isServiceDeskInstalledReturnsTrue() {
        assertThat(target.isServiceDeskInstalled(), equalTo(true));
    }

    @Test
    public void getHelperReturnsNonEmptyHelper() {
        assertThat(target.getHelper().get(), sameInstance(ServiceDesk3CompatibilityHelper.getInstance()));
    }
}
