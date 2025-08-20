package com.atlassian.plugins.slack.api.descriptor.factory;

import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.spring.scanner.annotation.export.ModuleType;
import com.atlassian.plugins.slack.api.descriptor.SlackNotificationDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ModuleType(ListableModuleDescriptorFactory.class)
public class SlackNotificationDescriptorFactory extends SingleModuleDescriptorFactory<SlackNotificationDescriptor> {
    @Autowired
    public SlackNotificationDescriptorFactory(final HostContainer hostContainer) {
        super(hostContainer, "slack-notification-type", SlackNotificationDescriptor.class);
    }
}
