package com.atlassian.bitbucket.plugins.slack.model;

import com.github.seratch.jslack.api.model.Attachment;

import java.util.Objects;

public class Unfurl {
    private final String originalUrl;
    private final Attachment attachment;

    public Unfurl(final String originalUrl,
                  final Attachment attachment) {
        this.originalUrl = originalUrl;
        this.attachment = attachment;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Attachment getAttachment() {
        return attachment;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Unfurl unfurl = (Unfurl) o;
        return originalUrl.equals(unfurl.originalUrl) &&
                Objects.equals(attachment, unfurl.attachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalUrl, attachment);
    }

    @Override
    public String toString() {
        return "Unfurl{" +
                "originalUrl='" + originalUrl + '\'' +
                ", attachment=" + attachment +
                '}';
    }
}
