package com.atlassian.plugins.slack.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.block.LayoutBlock;

import java.util.ArrayList;
import java.util.List;

public class DelayedSlackMessage {
    private String text;
    @JsonProperty("response_type")
    private String responseType;
    private List<Attachment> attachments = new ArrayList<>();
    private List<LayoutBlock> blocks = new ArrayList<>();

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(final List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public List<LayoutBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<LayoutBlock> blocks) {
        this.blocks = blocks;
    }
}
