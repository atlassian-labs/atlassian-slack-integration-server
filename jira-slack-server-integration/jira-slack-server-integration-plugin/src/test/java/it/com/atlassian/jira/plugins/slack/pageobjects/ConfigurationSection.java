package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import javax.inject.Inject;

public class ConfigurationSection {
    private static final String ADD_BUTTON_ID = "slack-project-to-channel-add";
    private static final String MAPPING_TABLE_CLASS_NAME = "slack-channel-mapping-list";
    private static final String CHANNEL_SELECTOR_ID = "slack-project-to-channel-add-select-single-select";

    @Inject
    private PageBinder pageBinder;
    private final PageElement container;

    public ConfigurationSection(PageElement container) {
        this.container = container;
    }

    public MappingRow clickAddButton() {
        final PageElement addButton = container.find(By.id(ADD_BUTTON_ID));
        Poller.waitUntilTrue(addButton.timed().isEnabled());
        addButton.click();
        return getMappingTable().getExpandedRow();
    }

    public MappingTable getMappingTable() {
        return pageBinder.bind(MappingTable.class, container.find(By.className(MAPPING_TABLE_CLASS_NAME)));
    }

    public void selectChannel(String channelName) {
        PageElement channelSelectorContainer = container.find(By.id(CHANNEL_SELECTOR_ID));
        final ChannelSelector channelSelector = pageBinder.bind(ChannelSelector.class, channelSelectorContainer);
        channelSelector.selectChannel(channelName);
    }
}
