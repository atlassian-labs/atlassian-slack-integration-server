package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;
import com.github.seratch.jslack.api.model.Attachment;

import java.util.Objects;

public class QuestionEvent implements ConfluenceSlackEvent, BaseSlackEvent {
    private final Space space;
    private final ConfluenceUser user;
    private final QuestionType questionType;
    private final String link;
    private final Attachment attachment;

    public QuestionEvent(final Space space,
                         final ConfluenceUser user,
                         final QuestionType questionType,
                         final String link,
                         final Attachment attachment) {
        this.space = space;
        this.user = user;
        this.questionType = questionType;
        this.link = link;
        this.attachment = attachment;
    }

    @Override
    public Space getSpace() {
        return space;
    }

    @Override
    public ConfluenceUser getUser() {
        return user;
    }

    @Override
    public String getLink() {
        return link;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public boolean isAnswer() {
        return questionType.equals(QuestionType.ANSWER);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestionEvent)) return false;
        final QuestionEvent that = (QuestionEvent) o;
        return Objects.equals(space, that.space) &&
                Objects.equals(user, that.user) &&
                questionType == that.questionType &&
                Objects.equals(link, that.link) &&
                Objects.equals(attachment, that.attachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, user, questionType, link, attachment);
    }

    @Override
    public String toString() {
        return "QuestionEvent{" +
                "space=" + space +
                ", user=" + user +
                ", questionType=" + questionType +
                ", link='" + link + '\'' +
                ", attachment=" + attachment +
                '}';
    }
}
