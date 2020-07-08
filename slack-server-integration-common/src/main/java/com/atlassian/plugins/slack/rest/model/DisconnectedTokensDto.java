package com.atlassian.plugins.slack.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
