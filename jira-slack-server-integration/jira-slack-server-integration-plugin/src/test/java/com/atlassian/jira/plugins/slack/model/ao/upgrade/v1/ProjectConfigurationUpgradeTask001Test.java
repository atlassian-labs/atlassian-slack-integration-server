package com.atlassian.jira.plugins.slack.model.ao.upgrade.v1;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ModelVersion;
import net.java.ao.Query;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectConfigurationUpgradeTask001Test {
    public static final int LIMIT = 1;

    @Mock
    ActiveObjects ao;
    @Mock
    ProjectConfigurationAOV001 projectConfig1;
    @Mock
    ProjectConfigurationAOV001 projectConfig2;
    @Captor
    ArgumentCaptor<String> valueCaptor;
    @Captor
    ArgumentCaptor<Query> queryCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    private ProjectConfigurationUpgradeTask001 target;

    private ModelVersion version = ModelVersion.valueOf("0");

    @Before
    public void setUp() throws Exception {
        target = new ProjectConfigurationUpgradeTask001(LIMIT);
    }

    @Test
    public void getModelReturnsExpectedValue() {
        ModelVersion version = target.getModelVersion();

        assertThat(version.toString(), equalTo("1"));
    }

    @Test
    public void upgradeCopiesValueToNewField() {
        String value = "someValue";

        when(ao.find(eq(ProjectConfigurationAOV001.class), any(Query.class)))
                .thenReturn(new ProjectConfigurationAOV001[] {projectConfig1}, new ProjectConfigurationAOV001[] {});
        when(projectConfig1.getValue()).thenReturn(value);

        target.upgrade(version, ao);

        verify(projectConfig1).setValueUnlimited(valueCaptor.capture());
        assertThat(valueCaptor.getValue(), equalTo(value));
        verify(projectConfig1).save();
    }

    @Test
    public void upgradeHandlesMultiplePages() {
        when(ao.find(eq(ProjectConfigurationAOV001.class), any(Query.class)))
                .thenReturn(new ProjectConfigurationAOV001[] {projectConfig1}, new ProjectConfigurationAOV001[] {projectConfig2},
                        new ProjectConfigurationAOV001[] {});

        target.upgrade(version, ao);

        verify(ao, times(3)).find(eq(ProjectConfigurationAOV001.class), queryCaptor.capture());
        List<Query> queries = queryCaptor.getAllValues();
        assertThat(queries, hasSize(3));
        assertThat(queries.get(0), hasProperty("orderClause", equalTo("ID")));
        assertThat(queries.get(0), hasProperty("limit", equalTo(LIMIT)));
        assertThat(queries.get(0), hasProperty("offset", equalTo(0)));
        assertThat(queries.get(1), hasProperty("limit", equalTo(LIMIT)));
        assertThat(queries.get(1), hasProperty("offset", equalTo(1)));
        assertThat(queries.get(2), hasProperty("limit", equalTo(LIMIT)));
        assertThat(queries.get(2), hasProperty("offset", equalTo(2)));

        verify(projectConfig1).save();
        verify(projectConfig2).save();
    }
}
