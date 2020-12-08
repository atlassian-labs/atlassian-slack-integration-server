package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.service.listener.IssueEventToEventMatcherTypeConverter;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

    private Collection<EventMatcherType> matchUpdatesEvent(IssueEvent issueEvent) {

        Collection<EventMatcherType> eventMatcherTypes = new ArrayList<>();

        final List<ChangeLogItem> changes = changeLogExtractor.getChanges(issueEvent);
        final List<String> updatedFields = changes.stream().map(ChangeLogItem::getField).collect(Collectors.toList());

        log.debug("Updated fields detected for issue key={}, eventTypeId={}: {}", issueEvent.getIssue().getKey(),
                issueEvent.getEventTypeId(), updatedFields);

        for (String field : updatedFields) {
            if (ChangeLogExtractor.ASSIGNEE_FIELD_NAME.equals(field)) {
                eventMatcherTypes.add(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
            } else if (ChangeLogExtractor.STATUS_FIELD_NAME.equals(field)) {
                eventMatcherTypes.add(EventMatcherType.ISSUE_TRANSITIONED);
            }
        }

        return eventMatcherTypes;
    }
}
