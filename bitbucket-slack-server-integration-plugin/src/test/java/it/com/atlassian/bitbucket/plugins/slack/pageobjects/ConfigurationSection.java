package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.bitbucket.element.AbstractElementPageObject;
import org.openqa.selenium.By;

public class ConfigurationSection extends AbstractElementPageObject {
    private static final String ADD_BUTTON_ID = "slack-repository-to-channel-add";
    private static final String MAPPING_TABLE_CLASS_NAME = "slack-channel-mapping-list";
    private static final String CHANNEL_SELECTOR_ID = "s2id_slack-repository-to-channel-add-select";

    public ConfigurationSection(PageElement container) {
        super(container);
    }

    public MappingRow clickAddButton() {
        find(By.id(ADD_BUTTON_ID)).click();
        return getMappingTable().getExpandedRow();
    }

    public MappingTable getMappingTable() {
        return pageBinder.bind(MappingTable.class, find(By.className(MAPPING_TABLE_CLASS_NAME)));
    }

    public ChannelSelector getChannelSelector() {
        PageElement channelSelectorContainer = find(By.id(CHANNEL_SELECTOR_ID));
        final ChannelSelector channelSelector = pageBinder.bind(ChannelSelector.class, channelSelectorContainer);
        channelSelector.openOptions();
        Poller.waitUntilTrue(channelSelectorContainer.timed().isVisible());
        return channelSelector;
    }
}
