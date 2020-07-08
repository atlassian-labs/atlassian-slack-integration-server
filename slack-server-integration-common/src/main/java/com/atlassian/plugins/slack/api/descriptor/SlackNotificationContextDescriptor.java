package com.atlassian.plugins.slack.api.descriptor;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugins.slack.api.notification.SlackNotificationContext;
import com.atlassian.util.concurrent.ResettableLazyReference;
import org.dom4j.Element;

public class SlackNotificationContextDescriptor extends AbstractModuleDescriptor<SlackNotificationContext> {
    private String value;

    private final ResettableLazyReference<SlackNotificationContext> moduleReference = new ResettableLazyReference<SlackNotificationContext>() {
        @Override
        protected SlackNotificationContext create() {
            return createModule();
        }
    };

    public SlackNotificationContextDescriptor(final ModuleFactory moduleFactory) {
        super(moduleFactory);
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);

        value = element.attributeValue("value");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getValue() {
        return value;
    }

    @Override
    public SlackNotificationContext getModule() {
        return moduleReference.get();
    }

    private SlackNotificationContext createModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
