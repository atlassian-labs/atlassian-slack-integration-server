package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.jira.pageobjects.components.DropdownSelect;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import jakarta.annotation.Nonnull;
import org.openqa.selenium.By;

public class ChannelSelector extends DropdownSelect {
    @ElementBy(id = "slack-project-to-channel-add-select-field")
    private PageElement input;
    private final PageElement container;

    /**
     * @param container element that encapsulates the Select2 input and current selections. Does not contain the
     *                  dropdown list
     */
    public ChannelSelector(@Nonnull PageElement container) {
        super(container, By.className("drop-menu"), By.id("slack-project-to-channel-add-select-suggestions"));
        this.container = container;
    }

    public void waitForChannelOption(String channelName) {
        Poller.waitUntilFalse(dropDown().find(By.id(channelName)).timed().isPresent());
    }

    public void selectChannel(String channelName) {
        waitForChannelOption(channelName);

        // try and retry
        pickOptionFixed(channelName);
        if (!isChannelSelected(channelName)) {
            // it's probably flakiness so let's try one more time
            flakyWait();
            pickOptionFixed(channelName);
        }
        if (!isChannelSelected(channelName)) {
            // it's probably flakiness so let's fallback to set value directly
            flakyWait();
            input.clear().type(channelName).type("\n");
        }
    }

    private void flakyWait() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isChannelSelected(String channelName) {
        try {
            // check if it was really selected
            Poller.waitUntilTrue(input.timed().hasValue(channelName));
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    // fixed version of Atlassian-provided DropdownSelect.pickOption()
    // it starts freezing in Jira 9.0
    private void pickOptionFixed(String optionId) {
        this.open();
        By selector = By.cssSelector(String.format(".aui-list-item-li-%s", optionId));
        this.activateSelection(selector);
        this.getDropDownItem(selector).click();
        this.waitForClose();
    }

    private void activateSelection(By by) {
        PageElement selection = this.elementFinder.find(by);
        this.actions.moveToElement(selection).moveByOffset(1, 0).perform();
    }
}
