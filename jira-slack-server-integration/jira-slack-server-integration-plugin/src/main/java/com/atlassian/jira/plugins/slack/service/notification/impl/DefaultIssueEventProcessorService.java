package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.ProjectConfigurationGroupSelector;
import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationGroupSelectorDTO;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilterService;
import com.atlassian.jira.plugins.slack.service.notification.IssueEventProcessorService;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.util.CommentUtil;
import com.atlassian.jira.plugins.slack.util.PluginConstants;
import com.atlassian.jira.project.Project;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.size;

/**
 * Responsible for the processing of an IssueEvent, which may cause notifications to be sent to Slack
 */
@Service
public class DefaultIssueEventProcessorService implements IssueEventProcessorService {
    private static final Logger log = LoggerFactory.getLogger(DefaultIssueEventProcessorService.class);

    private final ConfigurationDAO configurationDAO;
    private final ProjectConfigurationManager projectConfigurationManager;
    private final IssueFilterService filterService;
    private final SlackLinkManager slackLinkManager;

    @Autowired
    public DefaultIssueEventProcessorService(final ConfigurationDAO configurationDAO,
                                             final ProjectConfigurationManager projectConfigurationManager,
                                             final IssueFilterService filterService,
                                             final SlackLinkManager slackLinkManager) {
        this.configurationDAO = configurationDAO;
        this.projectConfigurationManager = projectConfigurationManager;
        this.filterService = filterService;
        this.slackLinkManager = slackLinkManager;
    }

    @Override
    public List<NotificationInfo> getNotificationsFor(final JiraIssueEvent event) {
        final EventMatcherType eventMatcher = event.getEventMatcher();

        if (!isListenerEnabledFor(eventMatcher)) {
            log.debug("Listener is not enabled for [{}]", eventMatcher);
            return Collections.emptyList();
        }

        final Issue issue = event.getIssue();
        final Project project = issue.getProjectObject();
        final Comment comment = event.getComment().orElse(null);
        final List<ProjectConfiguration> configurations = getConfigurationFor(project.getId(), issue, comment, eventMatcher);

        log.debug("Finding Configurations for event [{}] : {}", eventMatcher.name(), size(configurations));

        return configurations.stream()
                .filter(Objects::nonNull)
                .map(projectConfiguration -> new ProjectConfigurationGroupSelectorDTO(
                        projectConfiguration.getProjectId(),
                        projectConfiguration.getConfigurationGroupId()))
                .filter(selector -> {
                    final List<ProjectConfiguration> filterConfigs = getConfigurationFiltersFor(selector);
                    return CollectionUtils.isEmpty(filterConfigs) || filterService.apply(event, filterConfigs);
                })
                .flatMap(selector -> getNotificationInfos(selector).stream())
                .collect(Collectors.toList());
    }

    /**
     * Iterates over all the configurations of this project and validates how many configurations apply to that event
     * <p/>
     * NOTE : This event is performing better if we maintain the project configurations in cache. If not, a query
     * directly to the database should be better
     *
     * @param projectId    the project id
     * @param eventMatcher the event type
     * @return a list of project configurations
     */
    private List<ProjectConfiguration> getConfigurationFor(final Long projectId,
                                                           final Issue issue,
                                                           final @Nullable Comment comment,
                                                           final EventMatcherType eventMatcher) {
        Map<String, List<ProjectConfiguration>> configurationsByGroupId = configurationDAO.findByProjectId(projectId).stream()
                .collect(Collectors.groupingBy(ProjectConfiguration::getConfigurationGroupId));
        return configurationsByGroupId.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .noneMatch(config -> DefaultProjectConfigurationManager.IS_MUTED.equals(config.getName())))
                .filter(entry -> {
                    // skip sending notifications for restricted comments
                    if (EventMatcherType.ISSUE_COMMENTED == eventMatcher) {
                        boolean isRestrictedComment = CommentUtil.isRestricted(comment);
                        boolean shouldSkipRestricted = entry.getValue().stream()
                                .map(ProjectConfiguration::getName)
                                .anyMatch(DefaultProjectConfigurationManager.SKIP_RESTRICTED_COMMENTS::equalsIgnoreCase);
                        if (isRestrictedComment && shouldSkipRestricted) {
                            log.debug("Skipping sending notification for restricted comment. Issue: {}, comment ID: {}",
                                    issue.getKey(), comment.getId());
                            return false;
                        }
                    }
                    return true;
                })
                .flatMap(entry -> entry.getValue().stream())
                .filter(configuration -> {

                    final String name = configuration.getName();
                    final EventMatcherType matcherType = EventMatcherType.fromName(name);

                    return matcherType != null && matcherType == eventMatcher &&
                            matcherType.getMatcher().matches(issue, configuration);
                })
                .collect(Collectors.toList());
    }

    private boolean isListenerEnabledFor(EventMatcherType eventMatcher) {
        return (eventMatcher != null) && PluginConstants.EVENT_TYPES.contains(eventMatcher);
    }

    /**
     * It will get the grouping id and will search for filters inside the filter
     *
     * @param projectConfigurationGroupSelector project configuration group selector
     * @return a list of filters or an empty list
     */
    private List<ProjectConfiguration> getConfigurationFiltersFor(ProjectConfigurationGroupSelector projectConfigurationGroupSelector) {
        final List<ProjectConfiguration> configs = configurationDAO.findByProjectConfigurationGroupId(
                projectConfigurationGroupSelector.getProjectId(),
                projectConfigurationGroupSelector.getProjectConfigurationGroupId());

        return configs.stream()
                .filter(configuration -> configuration != null && EventFilterType.fromName(configuration.getName()) != null)
                .collect(Collectors.toList());
    }

    /**
     * Find all the notification details (channel name, notification config...) for a project configuration group.
     *
     * @param projectConfigurationGroupSelector project configuration group selector
     * @return a list of notification details or an empty list
     */
    private List<NotificationInfo> getNotificationInfos(ProjectConfigurationGroupSelector projectConfigurationGroupSelector) {
        final List<ProjectConfiguration> configs = configurationDAO.findByProjectConfigurationGroupId(
                projectConfigurationGroupSelector.getProjectId(),
                projectConfigurationGroupSelector.getProjectConfigurationGroupId());

        final Map<String, NotificationInfo> map = new HashMap<>();
        for (ProjectConfiguration config : configs) {
            final String channelId = config.getChannelId();
            final NotificationInfo notificationInfo = map.get(channelId);
            if (notificationInfo == null) {
                slackLinkManager.getLinkByTeamId(config.getTeamId()).forEach(
                        link -> map.put(channelId, new NotificationInfo(
                                link,
                                channelId,
                                null,
                                null,
                                projectConfigurationManager.getOwner(config).orElse(null),
                                projectConfigurationManager.getVerbosity(config).orElse(Verbosity.EXTENDED))));
            }
        }

        return new ArrayList<>(map.values());
    }
}
