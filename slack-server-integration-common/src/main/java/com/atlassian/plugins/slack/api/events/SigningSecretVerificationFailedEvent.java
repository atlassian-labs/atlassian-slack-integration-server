package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@EventName("notifications.slack.signature.verification.failed")
public class SigningSecretVerificationFailedEvent extends BaseAnalyticEvent {
    public static enum Cause {
        REQUEST_EXPIRED,
        BAD_SIGNATURE,
        NO_TEAM_ID,
        OTHER
    }

    private Cause cause;

    public SigningSecretVerificationFailedEvent(final AnalyticsContext context, final Cause cause) {
        super(context);
        this.cause = Preconditions.checkNotNull(cause);
    }

    public String getCause() {
        return cause.name();
    }
}
