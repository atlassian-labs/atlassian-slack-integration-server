package com.atlassian.plugins.slack.rest.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class DisconnectedTokensDto {
    @XmlElement
    private final String disconnectedSlackUserId;
    @XmlElement
    private final boolean isAnyLinkDisconnected;

    public DisconnectedTokensDto(final String disconnectedSlackUserId,
                                 final boolean isAnyLinkDisconnected) {
        this.disconnectedSlackUserId = disconnectedSlackUserId;
        this.isAnyLinkDisconnected = isAnyLinkDisconnected;
    }

    public String getDisconnectedSlackUserId() {
        return disconnectedSlackUserId;
    }

    public boolean isAnyLinkDisconnected() {
        return isAnyLinkDisconnected;
    }
}
