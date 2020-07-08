package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.dao.DedicatedChannelDAO;
import com.atlassian.jira.plugins.slack.model.event.ShowBotAddedHelpEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.api.webhooks.MemberJoinedChannelSlackEvent;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service
public class MemberJoinedEventListener extends AutoSubscribingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(MemberJoinedEventListener.class);

    private final ConfigurationDAO configurationDAO;
    private final DedicatedChannelDAO dedicatedChannelDAO;
    private final TaskExecutorService taskExecutorService;
    private final TaskBuilder taskBuilder;

    @Autowired
    public MemberJoinedEventListener(final EventPublisher eventPublisher,
                                     final ConfigurationDAO configurationDAO,
                                     final DedicatedChannelDAO dedicatedChannelDAO,
                                     final TaskExecutorService taskExecutorService,
                                     final TaskBuilder taskBuilder) {
        super(eventPublisher);
        this.configurationDAO = configurationDAO;
        this.dedicatedChannelDAO = dedicatedChannelDAO;
        this.taskExecutorService = taskExecutorService;
        this.taskBuilder = taskBuilder;
    }

    @EventListener
    public void memberJoined(@Nonnull final MemberJoinedChannelSlackEvent event) {
        logger.debug("Got MemberJoinedChannelSlackEvent event");
        if (event.getSlackEvent().getSlackLink().getBotUserId().equals(event.getUser())) {
            if (configurationDAO.findByChannel(event.getChannel()).isEmpty()
                    && dedicatedChannelDAO.findMappingsForChannel(event.getChannel()).isEmpty()) {
                //send initial help message if there are no configurations for this channel
                final NotificationInfo notificationInfo = new NotificationInfo(
                        event.getSlackEvent().getSlackLink(),
                        event.getChannel(),
                        null,
                        null,
                        "",
                        Verbosity.EXTENDED);
                final SendNotificationTask task = taskBuilder.newSendNotificationTask(
                        new ShowBotAddedHelpEvent(
                                event.getSlackEvent().getSlackLink(),
                                event.getChannel()
                        ),
                        notificationInfo,
                        taskExecutorService);
                taskExecutorService.submitTask(task);
            }
        }
    }
}
