package com.atlassian.jira.plugins.slack.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.ao.ProjectConfigurationAO;
import com.atlassian.plugins.slack.api.ConversationKey;
import net.java.ao.Query;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DefaultConfigurationDaoTest {
    @Mock
    private ActiveObjects ao;
    @Mock
    private CacheManager cacheManager;

    @Mock
    private ProjectConfigurationAO projectConfigurationAO;
    @Mock
    private ProjectConfigurationAO projectConfigurationAO2;
    @Mock
    private ProjectConfiguration projectConfiguration;
    @Mock
    private Cache<Long, List<ProjectConfiguration>> cache;

    @Captor
    private ArgumentCaptor<Query> query;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultConfigurationDao target;

    @Test
    public void isAnyProjectConfigured_shouldQueryValue() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenReturn(cache);
        when(cache.getKeys()).thenReturn(Collections.emptyList());
        when(ao.count(ProjectConfigurationAO.class)).thenReturn(0, 1);

        target = new DefaultConfigurationDao(ao, cacheManager);

        assertThat(target.isAnyProjectConfigured(), is(false));
        assertThat(target.isAnyProjectConfigured(), is(true));
    }

    @Test
    public void isAnyProjectConfigured_shouldReturnProjectThenUseCache() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> cache);
        when(cache.getKeys()).thenReturn(Collections.singleton(1L));
        when(cache.get(1L)).thenReturn(Arrays.asList(projectConfigurationAO, projectConfigurationAO2));

        target = new DefaultConfigurationDao(ao, cacheManager);

        assertThat(target.isAnyProjectConfigured(), is(true));
        verify(ao, never()).count(any());
    }

    @Test
    public void getById() {
        ProjectConfigurationAO[] list = {projectConfigurationAO};
        when(ao.find(ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_ID + " = ?", 1L))
                .thenReturn(list);

        target = new DefaultConfigurationDao(ao, cacheManager);
        ProjectConfigurationAO result = target.getById(1L);

        assertThat(result, sameInstance(projectConfigurationAO));
    }

    @Test
    public void findByProjectId() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> {
                    final CacheLoader<Long, List<ProjectConfiguration>> loader = args.getArgument(1);
                    when(cache.get(any())).thenAnswer(args2 -> loader.load(args2.getArgument(0)));
                    return cache;
                });

        ProjectConfigurationAO[] configurationsList = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ?", 1L))
                .thenReturn(configurationsList);

        target = new DefaultConfigurationDao(ao, cacheManager);
        List<ProjectConfiguration> result = target.findByProjectId(1L);

        assertThat(result, containsInAnyOrder(projectConfigurationAO, projectConfigurationAO2));
    }

    @Test
    public void findByChannel() {
        ProjectConfigurationAO[] configurationsList = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_TEAM_ID + " = ? AND " + ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ?", "T", "C"))
                .thenReturn(configurationsList);

        target = new DefaultConfigurationDao(ao, cacheManager);
        List<ProjectConfiguration> result = target.findByChannel(new ConversationKey("T", "C"));

        assertThat(result, containsInAnyOrder(projectConfigurationAO, projectConfigurationAO2));
    }

    @Test
    public void findByTeam() {
        ProjectConfigurationAO[] configurationsList = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_TEAM_ID + " = ?", "T"))
                .thenReturn(configurationsList);

        target = new DefaultConfigurationDao(ao, cacheManager);
        List<ProjectConfiguration> result = target.findByTeam("T");

        assertThat(result, containsInAnyOrder(projectConfigurationAO, projectConfigurationAO2));
    }

    @Test
    public void findMuted() {
        ProjectConfigurationAO[] configurationsList = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_NAME + " = ?", DefaultProjectConfigurationManager.IS_MUTED))
                .thenReturn(configurationsList);

        target = new DefaultConfigurationDao(ao, cacheManager);
        List<ProjectConfiguration> result = target.findMuted();

        assertThat(result, containsInAnyOrder(projectConfigurationAO, projectConfigurationAO2));
    }

    @Test
    public void getProjectConfigurations() {
        ProjectConfigurationAO[] configurationsList = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(same(ProjectConfigurationAO.class), query.capture())).thenReturn(configurationsList);

        target = new DefaultConfigurationDao(ao, cacheManager);
        List<ProjectConfiguration> result = target.getProjectConfigurations(1, 2);

        assertThat(result, containsInAnyOrder(projectConfigurationAO, projectConfigurationAO2));
    }

    @Test
    public void insertProjectConfiguration() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> cache);
        when(ao.create(eq(ProjectConfigurationAO.class), mapCaptor.capture())).thenReturn(projectConfigurationAO);
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration.getName()).thenReturn("N");
        when(projectConfiguration.getValue()).thenReturn("V");

        target = new DefaultConfigurationDao(ao, cacheManager);
        ProjectConfiguration result = target.insertProjectConfiguration(projectConfiguration);


        assertThat(result, sameInstance(projectConfigurationAO));
        verify(cache).remove(7L);

        Map<String, Object> map = mapCaptor.getValue();
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_PROJECT_ID, 7L));
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_TEAM_ID, "T"));
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_CHANNEL_ID, "C"));
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID, "G"));
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_NAME, "N"));
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_VALUE, "V"));
        assertThat(map, hasEntry(ProjectConfigurationAO.COLUMN_NAME_UNIQUE_CONSTRAINT, "N:G"));
    }

    @Test
    public void updateProjectConfigurationWithId() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> cache);
        ProjectConfigurationAO[] list = {projectConfigurationAO};
        when(ao.find(ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_ID + " = ?", 1L))
                .thenReturn(list);
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration.getName()).thenReturn("N");
        when(projectConfiguration.getValue()).thenReturn("V");

        target = new DefaultConfigurationDao(ao, cacheManager);
        target.updateProjectConfiguration(1L, projectConfiguration);

        verify(cache).remove(7L);
        verify(projectConfigurationAO).setProjectId(7L);
        verify(projectConfigurationAO).setTeamId("T");
        verify(projectConfigurationAO).setChannelId("C");
        verify(projectConfigurationAO).setConfigurationGroupId("G");
        verify(projectConfigurationAO).setName("N");
        verify(projectConfigurationAO).setValue("V");
        verify(projectConfigurationAO).setNameUniqueConstraint("N:G");
        verify(projectConfigurationAO).save();
    }

    @Test
    public void updateProjectConfiguration() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> cache);
        ProjectConfigurationAO[] list = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_NAME + " = ?",
                7L, "C", "G", "N")
        ).thenReturn(list);
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration.getName()).thenReturn("N");
        when(projectConfiguration.getValue()).thenReturn("V");

        target = new DefaultConfigurationDao(ao, cacheManager);
        target.updateProjectConfiguration(projectConfiguration);

        verify(cache).remove(7L);
        verify(projectConfigurationAO).setProjectId(7L);
        verify(projectConfigurationAO).setTeamId("T");
        verify(projectConfigurationAO).setChannelId("C");
        verify(projectConfigurationAO).setConfigurationGroupId("G");
        verify(projectConfigurationAO).setName("N");
        verify(projectConfigurationAO).setValue("V");
        verify(projectConfigurationAO).setNameUniqueConstraint("N:G");
        verify(projectConfigurationAO).save();
    }

    @Test
    public void deleteProjectConfiguration() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> cache);
        ProjectConfigurationAO[] list = {projectConfigurationAO};
        when(ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_NAME + " = ?",
                7L, "C", "G", "N")
        ).thenReturn(list);
        when(projectConfigurationAO.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration.getName()).thenReturn("N");

        target = new DefaultConfigurationDao(ao, cacheManager);
        target.deleteProjectConfiguration(projectConfiguration);

        verify(cache).remove(7L);
        verify(ao).delete(projectConfigurationAO);
    }

    @Test
    public void deleteProjectConfigurationGroup() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> cache);
        ProjectConfigurationAO[] list = {projectConfigurationAO};
        when(ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID + " = ?",
                7L, "C", "G")
        ).thenReturn(list);
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");

        target = new DefaultConfigurationDao(ao, cacheManager);
        target.deleteProjectConfigurationGroup(projectConfiguration);

        verify(cache).remove(7L);
        verify(ao).delete(list);
    }

    @Test
    public void deleteAllConfigurations() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenReturn(cache);
        when(ao.deleteWithSQL(ProjectConfigurationAO.class, "TEAM_ID = ?", "T")).thenReturn(1);

        target = new DefaultConfigurationDao(ao, cacheManager);
        int result = target.deleteAllConfigurations("T");

        assertThat(result, is(1));
        verify(cache).removeAll();
        verify(ao).deleteWithSQL(ProjectConfigurationAO.class, "TEAM_ID = ?", "T");
    }

    @Test
    public void findByProjectConfigurationGroupId() {
        when(cacheManager.getCache(eq(DefaultConfigurationDao.class.getName() + ".slack-project-configurations"),
                (CacheLoader<Long, List<ProjectConfiguration>>) any()))
                .thenAnswer(args -> {
                    final CacheLoader<Long, List<ProjectConfiguration>> loader = args.getArgument(1);
                    when(cache.get(any())).thenAnswer(args2 -> loader.load(args2.getArgument(0)));
                    return cache;
                });

        ProjectConfigurationAO[] configurationsList = {projectConfigurationAO, projectConfigurationAO2};
        when(ao.find(ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ?", 1L))
                .thenReturn(configurationsList);
        when(projectConfigurationAO.getConfigurationGroupId()).thenReturn("G1");
        when(projectConfigurationAO2.getConfigurationGroupId()).thenReturn("G2");

        target = new DefaultConfigurationDao(ao, cacheManager);

        assertThat(target.findByProjectConfigurationGroupId(1L, "G1"), contains(projectConfigurationAO));
        assertThat(target.findByProjectConfigurationGroupId(1L, "G2"), contains(projectConfigurationAO2));
    }
}
