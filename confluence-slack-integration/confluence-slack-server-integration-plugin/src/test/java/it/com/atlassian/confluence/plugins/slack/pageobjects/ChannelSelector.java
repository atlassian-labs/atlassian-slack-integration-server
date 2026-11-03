package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.inject.Inject;
import jakarta.annotation.Nonnull;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ChannelSelector {
    @Inject
    protected PageElementFinder finder;

    private final PageElement container;

    public ChannelSelector(@Nonnull PageElement container) {
        this.container = container;
    }

    public void selectChannel(String channelName) {
        flakyWait();
        trySelectingChannel(channelName);
        String selectedChannel = finder.find(By.id("slack-space-to-channel-add-select")).timed().getValue().byDefaultTimeout();
        if (isBlank(selectedChannel)) {
            flakyWait();
            trySelectingChannel(channelName);
        }
    }

    private void trySelectingChannel(String channelName) {
        container.click();
        Poller.waitUntilTrue(finder.find(By.cssSelector(".select2-results .public-channel")).timed().isVisible());
        final PageElement searchInput = finder.find(By.cssSelector("input.select2-focused"));
        searchInput.click().clear().type(channelName);
        // wait until the channel query has highlighted the typed channel, then confirm with an explicit Enter key
        // (typing "\n" no longer selected the highlighted result with Selenium 4 / Firefox 153)
        Poller.waitUntilTrue(finder.find(By.cssSelector(".select2-results .select2-highlighted")).timed().hasText(channelName));
        searchInput.type(Keys.ENTER);
    }

    private void flakyWait() {
        try {
            Thread.sleep(750);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
