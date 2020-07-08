package com.atlassian.plugins.slack.api.notification;

import com.atlassian.plugins.slack.api.descriptor.SlackNotificationDescriptor;
import com.atlassian.sal.api.message.I18nResolver;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationType {
    private final String key;
    private final String label;
    private final String context;
    private final String category;
    private final int weight;
    private final Optional<SlackNotification<Object>> notification;
    private final boolean activeByDefault;

    @JsonCreator
    public NotificationType(@JsonProperty("key") String key,
                            @JsonProperty("label") String label,
                            @JsonProperty("context") String context,
                            @JsonProperty("category") String category,
                            @JsonProperty("weight") int weight,
                            @JsonProperty("activeByDefault") boolean activeByDefault) {
        this(key, label, context, category, weight, activeByDefault, Optional.empty());
    }

    public NotificationType(String key,
                            String label,
                            String context,
                            Optional<SlackNotification<Object>> notification) {
        this(key, label, context, null, 1000, false, notification);
    }

    public NotificationType(String key,
                            String label,
                            String context,
                            String category,
                            int weight,
                            boolean activeByDefault,
                            Optional<SlackNotification<Object>> notification) {
        this.key = key;
        this.label = label;
        this.context = context;
        this.category = category;
        this.weight = weight;
        this.activeByDefault = activeByDefault;
        this.notification = notification;
    }

    public static NotificationType fromModuleDescriptor(final SlackNotificationDescriptor moduleDescriptor,
                                                        final I18nResolver i18nResolver,
                                                        final SlackNotification<Object> notification) {
        return new NotificationType(
                moduleDescriptor.getValue(),
                i18nResolver.getText(moduleDescriptor.getI18nNameKey()),
                moduleDescriptor.getContext(),
                moduleDescriptor.getCategory(),
                moduleDescriptor.getWeight(),
                moduleDescriptor.isActiveByDefault(),
                Optional.of(notification));
    }

    @JsonProperty
    public String getKey() {
        return key;
    }

    @JsonProperty
    public String getLabel() {
        return label;
    }

    @JsonProperty
    public String getContext() {
        return context;
    }

    @JsonProperty
    public String getCategory() {
        return category;
    }

    @JsonProperty
    public int getWeight() {
        return weight;
    }

    @JsonProperty
    public boolean isActiveByDefault() {
        return activeByDefault;
    }

    @JsonIgnore
    public Optional<SlackNotification<Object>> getNotification() {
        return notification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationType that = (NotificationType) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
