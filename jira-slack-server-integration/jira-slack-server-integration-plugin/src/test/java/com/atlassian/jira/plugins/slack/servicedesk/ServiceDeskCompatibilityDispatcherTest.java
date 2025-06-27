package com.atlassian.jira.plugins.slack.servicedesk;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

public class ServiceDeskCompatibilityDispatcherTest {
    ServiceDeskCompatibilityDispatcher target;

    @Before
    public void setUp() {
        target = new ServiceDeskCompatibilityDispatcher();
    }

    @Test
    public void isServiceDeskInstalledReturnsTrue() {
        assertThat(target.isServiceDeskInstalled(), equalTo(true));
    }

    @Test
    public void getHelperReturnsNonEmptyHelper() {
        assertThat(target.getHelper().get(), sameInstance(DefaultServiceDeskCompatibilityHelper.getInstance()));
    }
}
