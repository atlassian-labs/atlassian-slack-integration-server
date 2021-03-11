package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.jira.event.issue.IssueChangedEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.service.listener.IssueEventToEventMatcherTypeConverter;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
        for (ChangeLogItem change : changes) {
            String field = change.getField();
            if (ChangeLogExtractor.ASSIGNEE_FIELD_NAME.equals(field)) {
                eventMatcherTypes.add(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
            }
        }

        return eventMatcherTypes;
    }

    @Override
    public Collection<EventMatcherType> match(IssueChangedEvent event) {
        Collection<ChangeItemBean> changeItems = event.getChangeItems();
        Collection<EventMatcherType> detectedMatchers = Collections.emptyList();

        for (ChangeItemBean changeItem : changeItems) {
            if (ChangeLogExtractor.STATUS_FIELD_NAME.equals(changeItem.getField())) {
                detectedMatchers = Collections.singletonList(EventMatcherType.ISSUE_TRANSITIONED);
                break;
            }
        }

        return detectedMatchers;
    }
}
