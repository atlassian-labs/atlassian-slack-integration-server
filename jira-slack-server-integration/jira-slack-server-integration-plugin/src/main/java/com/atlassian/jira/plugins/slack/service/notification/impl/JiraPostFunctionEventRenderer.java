package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.JiraPostFunctionEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.MessageRendererException;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.util.JsonPrimitiveReferenceInsertionEventHandler;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Attachment;
import com.google.common.base.Strings;
import io.atlassian.fugue.Either;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("jiraPostFunctionEventRenderer")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class JiraPostFunctionEventRenderer extends AbstractEventRenderer<JiraPostFunctionEvent> {
    private static final Logger log = LoggerFactory.getLogger(JiraPostFunctionEventRenderer.class);

    private final I18nResolver i18nResolver;
    private final AttachmentHelper attachmentHelper;
    private final CustomFieldManager customFieldManager;

    @Override
    protected List<SlackNotification> doRender(final JiraPostFunctionEvent event,
                                               final List<NotificationInfo> notificationInfos) {
        try {
            final ChatPostMessageRequestBuilder renderedMessage = event.isHavingErrors()
                    ? renderErrorNotification(event)
                    : renderNotification(event);
            return notificationInfos.stream().map(notificationInfo -> new SlackNotification(
                    notificationInfo,
                    renderedMessage
                            .channel(notificationInfo.getChannelId())
                            .threadTs(notificationInfo.getThreadTimestamp())
                            .build()))
                    .collect(Collectors.toList());
        } catch (MessageRendererException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean canRender(PluginEvent pluginEvent) {
        return (pluginEvent instanceof JiraPostFunctionEvent);
    }

    private ChatPostMessageRequestBuilder renderNotification(final JiraPostFunctionEvent event) {
        try {
            return (Strings.isNullOrEmpty(event.getCustomMessageFormat()))
                    ? renderStandardNotification(event)
                    : renderCustomNotification(event);
        } catch (Exception e) {
            log.info("Could not render notification", e);
            return renderErrorNotification(event);
        }
    }

    public Either<Throwable, ChatPostMessageRequestBuilder> renderPreviewNotification(final JiraPostFunctionEvent event) {
        try {
            return Either.right(Strings.isNullOrEmpty(event.getCustomMessageFormat())
                    ? renderStandardNotification(event)
                    : renderCustomNotification(event));
        } catch (Exception e) {
            log.debug("Could not render notification", e);
            return Either.left(e);
        }
    }

    private ChatPostMessageRequestBuilder renderErrorNotification(final JiraPostFunctionEvent event) {
        return ChatPostMessageRequest.builder()
                .mrkdwn(true)
                .attachments(Collections.singletonList(Attachment.builder()
                        .text(i18nResolver.getText(
                                "slack.notification.configerror",
                                attachmentHelper.projectUrl(event.getIssue().getProjectObject().getKey())))
                        .color(event.isHavingErrors() ? "danger" : null)
                        .mrkdwnIn(Arrays.asList("text", "pretext"))
                        .build()
                ));
    }

    private ChatPostMessageRequestBuilder renderStandardNotification(final JiraPostFunctionEvent event) {
        return ChatPostMessageRequest.builder()
                .mrkdwn(true)
                .attachments(Collections.singletonList(attachmentHelper.buildIssueAttachment(
                        null,
                        event.getIssue(),
                        null)
                ));
    }

    private ChatPostMessageRequestBuilder renderCustomNotification(final JiraPostFunctionEvent event) throws Exception {
        final Issue issue = event.getIssue();

        Map<String, Object> context = new HashMap<>();
        context.put("user", event.getActor());
        context.put("issue", issue);
        context.put("project", issue.getProjectObject());
        context.put("status", issue.getStatus());
        context.put("priority", issue.getPriority());
        context.put("issueType", issue.getIssueType());
        context.put("creator", issue.getCreator());
        context.put("assignee", issue.getAssignee());
        context.put("reporter", issue.getReporter());
        context.put("action", event.getActionName());
        context.put("firstStep", event.getFirstStepName());
        context.put("endStep", event.getEndStepName());
        context.put("customFields", customFieldManager.getCustomFieldObjects(issue).stream()
                .flatMap(field -> Stream.of(Pair.of(field.getId(), new CustomFieldWrapper(field, issue)),
                        Pair.of(field.getFieldName(), new CustomFieldWrapper(field, issue))))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (field1, field2) -> field1)));

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");

        // prevent attempts to access system classes via reflection
        // Samples of forbidden code:
        // - "".getClass().forName("com.MyClass")
        // - "".getClass().getClassLoader().loadClass("com.MyClass")
        velocityEngine.setProperty("runtime.introspector.uberspect",
                "org.apache.velocity.util.introspection.SecureUberspector");

        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new JsonPrimitiveReferenceInsertionEventHandler());

        VelocityContext vc = new VelocityContext(context);
        vc.attachEventCartridge(ec);

        StringWriter sw = new StringWriter();
        if (velocityEngine.evaluate(vc, sw, "renderCustomNotification", event.getCustomMessageFormat())) {
            return ChatPostMessageRequest.builder()
                    .mrkdwn(true)
                    .text(sw.toString())
                    .attachments(Collections.singletonList(attachmentHelper.buildIssueAttachment(
                            null,
                            issue,
                            null)
                    ));
        } else {
            return renderErrorNotification(event);
        }
    }

    @Value
    public static class CustomFieldWrapper {
        CustomField field;
        Issue issue;

        public String getFieldName() {
            return field.getFieldName();
        }

        public String getName() {
            return field.getName();
        }

        public Object getValue() {
            return field.getValue(issue);
        }
    }
}
