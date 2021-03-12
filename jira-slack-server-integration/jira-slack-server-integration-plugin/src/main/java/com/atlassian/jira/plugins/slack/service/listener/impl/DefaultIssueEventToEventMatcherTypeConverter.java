package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.jira.event.issue.IssueChangedEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.service.listener.IssueEventToEventMatcherTypeConverter;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultIssueEventToEventMatcherTypeConverter implements IssueEventToEventMatcherTypeConverter {
    private final ChangeLogExtractor changeLogExtractor;

    @Autowired
    public DefaultIssueEventToEventMatcherTypeConverter(final ChangeLogExtractor changeLogExtractor) {
        this.changeLogExtractor = changeLogExtractor;
    }

    @Override
    public Collection<EventMatcherType> match(IssueEvent issueEvent) {

        Collection<EventMatcherType> eventMatcherTypes = new HashSet<>();

        eventMatcherTypes.addAll(matchUpdatesEvent(issueEvent));

        if (issueEvent.getComment() != null) {
            eventMatcherTypes.add(EventMatcherType.ISSUE_COMMENTED);
        }

        EventMatcherType type = EventMatcherType.fromJiraEventId(issueEvent.getEventTypeId());

        if (type != null) {
            eventMatcherTypes.add(type);
        }

        return eventMatcherTypes;
    }

    private Collection<EventMatcherType> matchUpdatesEvent(final IssueEvent issueEvent) {
        final List<ChangeLogItem> changes = changeLogExtractor.getChanges(issueEvent);
        final Set<String> updatedFields = changes.stream().map(ChangeLogItem::getField).collect(Collectors.toSet());

        log.debug("Updated fields detected for IssueEvent on issue key={}, eventTypeId={}: {}", issueEvent.getIssue().getKey(),
                issueEvent.getEventTypeId(), updatedFields);

        Collection<EventMatcherType> detectedMatchers = Collections.emptyList();
        if (updatedFields.contains(ChangeLogExtractor.ASSIGNEE_FIELD_NAME)) {
            detectedMatchers = Collections.singletonList(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
        }

        return detectedMatchers;
    }

    @Override
    public Collection<EventMatcherType> match(final IssueChangedEvent event) {
        final Collection<ChangeItemBean> changeItems = event.getChangeItems();
        final Set<String> updatedFields = changeItems.stream().map(ChangeItemBean::getField).collect(Collectors.toSet());

        log.debug("Updated fields detected for IssueChangedEvent on issue key={}: {}", event.getIssue().getKey(), updatedFields);

        Collection<EventMatcherType> detectedMatchers = Collections.emptyList();
        if (updatedFields.contains(ChangeLogExtractor.STATUS_FIELD_NAME)) {
            detectedMatchers = Collections.singletonList(EventMatcherType.ISSUE_TRANSITIONED);
        }

        return detectedMatchers;
    }
}
