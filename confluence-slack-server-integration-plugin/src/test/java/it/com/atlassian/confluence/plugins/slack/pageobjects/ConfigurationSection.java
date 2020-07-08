package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

public class ConfigurationSection extends ConfluenceAbstractPageComponent {
    private static final String ADD_BUTTON_ID = "slack-space-to-channel-add";
    private static final String MAPPING_TABLE_CLASS_NAME = "slack-channel-mapping-list";
    private static final String CHANNEL_SELECTOR_ID = "s2id_slack-space-to-channel-add-select";
    private final PageElement container;

    public ConfigurationSection(PageElement container) {
        this.container = container;
    }

    public MappingRow clickAddButton() {
        container.find(By.id(ADD_BUTTON_ID)).click();
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
