package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilter;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilterService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Gathers all filters and determines whether a Jira event should generate a notification or not, which should occur
 * only if all filters pass.
 * E.g.: {@link JqlIssueFilter} filters out issue events that don't match the JQL query if one is defined.
 */
@Service
public class DefaultIssueFilterService implements IssueFilterService {
    /**
     * This map is made in the constructor cause it is easier than map it in spring context xml
     * <p/>
     * The Maps.uniqueIndex guards ourselves from duplicated filters
     */
    private final Map<EventFilterType, IssueFilter> filters;

    @Autowired
    public DefaultIssueFilterService(final List<IssueFilter> filters) {
        this.filters = Maps.uniqueIndex(filters, issueFilter -> issueFilter != null ? issueFilter.getEventFilterType() : null);
    }

    /**
     * Returns true if all filters match.
     */
    @Override
    public boolean apply(final JiraIssueEvent event, Collection<ProjectConfiguration> configurations) {
        final List<IssueFilterConfiguration> filtersToRun = getFiltersToRunFor(configurations);
        return filtersToRun.stream().allMatch(issueFilter ->
                issueFilter != null && issueFilter.getFilter().apply(event, issueFilter.getConfiguration().getValue()));
    }

    /**
     * Returns the issue filters with the configurations to run
     *
     * @param configurations the configurations
     * @return a list of match between IssueFilter + ProjectConfiguration
     */
    private List<IssueFilterConfiguration> getFiltersToRunFor(Collection<ProjectConfiguration> configurations) {
        final ImmutableList.Builder<IssueFilterConfiguration> builder = ImmutableList.builder();
        for (ProjectConfiguration config : configurations) {
            final EventFilterType key = EventFilterType.fromName(config.getName());

            if (key != null) {
                final IssueFilter filter = filters.get(key);

                if (filter != null) {
                    builder.add(new IssueFilterConfiguration(filter, config));
                }
            }
        }
        return builder.build();
    }
}

