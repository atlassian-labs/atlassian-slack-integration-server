package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.model.event.ShowWelcomeEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Event listener for Slack Jira issue mention events
 */
@Service
public class LinkEventListener extends AutoSubscribingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(LinkEventListener.class);

    private final ConfigurationDAO configurationDAO;
    private final AsyncExecutor asyncExecutor;
    private final TaskBuilder taskBuilder;

    @Autowired
    public LinkEventListener(final EventPublisher eventPublisher,
                             final ConfigurationDAO configurationDAO,
                             final AsyncExecutor asyncExecutor,
                             final TaskBuilder taskBuilder) {
        super(eventPublisher);
        this.configurationDAO = configurationDAO;
        this.asyncExecutor = asyncExecutor;
        this.taskBuilder = taskBuilder;
    }

    @EventListener
    public void linkWasDeleted(@Nonnull final SlackTeamUnlinkedEvent event) {
        logger.debug("Got SlackTeamUnlinkedEvent event");
        configurationDAO.deleteAllConfigurations(event.getTeamId());
    }

    @EventListener
    public void linkWasCreated(@Nonnull final SlackLinkedEvent event) {
        logger.debug("Got SlackLinkedEvent event...");

        //send welcome message
        final NotificationInfo notificationInfo = new NotificationInfo(
                event.getLink(),
                event.getLink().getUserId(),
                null,
                null,
                "",
                "",
                event.getLink().getUserId(),
                "",
                Verbosity.EXTENDED);
        ShowWelcomeEvent pluginEvent = new ShowWelcomeEvent(event.getLink().getTeamId());
        asyncExecutor.run(taskBuilder.newDirectMessageTask(pluginEvent, notificationInfo));
    }
}
