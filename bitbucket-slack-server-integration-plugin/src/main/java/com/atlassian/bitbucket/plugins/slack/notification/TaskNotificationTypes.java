package com.atlassian.bitbucket.plugins.slack.notification;

import com.atlassian.bitbucket.event.task.TaskCreatedEvent;
import com.atlassian.bitbucket.event.task.TaskDeletedEvent;
import com.atlassian.bitbucket.event.task.TaskEvent;
import com.atlassian.bitbucket.event.task.TaskUpdatedEvent;
import com.atlassian.bitbucket.task.TaskState;

public enum TaskNotificationTypes {
    CREATED("TaskCreated"),
    DELETED("TaskDeleted"),
    UPDATED("TaskUpdated"),
    RESOLVED("TaskResolved"),
    REOPENED("TaskReopened");

    private final String key;

    TaskNotificationTypes(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static TaskNotificationTypes from(final TaskEvent event) {
        if (event instanceof TaskCreatedEvent) {
            return CREATED;
        }
        if (event instanceof TaskDeletedEvent) {
            return DELETED;
        }
        if (event instanceof TaskUpdatedEvent) {
            TaskUpdatedEvent updatedEvent = (TaskUpdatedEvent) event;
            if (updatedEvent.getPreviousState() == TaskState.RESOLVED && event.getTask().getState() == TaskState.OPEN) {
                return TaskNotificationTypes.REOPENED;
            }
            if (updatedEvent.getPreviousState() == TaskState.OPEN && event.getTask().getState() == TaskState.RESOLVED) {
                return TaskNotificationTypes.RESOLVED;
            }
        }
        return TaskNotificationTypes.UPDATED;
    }
}
