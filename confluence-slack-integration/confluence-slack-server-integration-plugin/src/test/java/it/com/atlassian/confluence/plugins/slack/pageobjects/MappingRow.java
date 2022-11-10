package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog2;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

public class MappingRow extends ConfluenceAbstractPageComponent {
    private final PageElement container;

    public MappingRow(PageElement container) {
        this.container = container;
    }

    public MappingRow clickDoneButton() {
        container.find(By.className("close-edit-notification")).click();
        return this;
    }

    public MappingRow clickEditButton() {
        container.find(By.className("edit-notification")).click();
        return this;
    }

    public void clickTrashButton() {
        container.find(By.className("trash-channel-mapping")).click();

        final Dialog2 dialog = pageBinder.bind(Dialog2.class, "slack-remove-mapping-dialog");
        final PageElement confirmButton = getConfirmButton(dialog);

        Poller.waitUntilTrue(confirmButton.timed().isVisible());
        confirmButton.click();
    }

    private PageElement getConfirmButton(Dialog2 dialog) {
        return dialog.getDialog().find(By.cssSelector(".dialog-button-panel button"));
    }

    public List<String> getConfiguredNotifications() {
        List<String> selectedNotifications = new ArrayList<>();
        for (PageElement checkbox : container.findAll(By.cssSelector("input[type=checkbox].notification-type:checked"))) {
            selectedNotifications.add(checkbox.getAttribute("data-notification-name"));
        }
        return selectedNotifications;
    }

    public TimedQuery<String> getChannelId() {
        return container.timed().getAttribute("data-channel-id");
    }

    public TimedQuery<String> getChannelName() {
        return container.find(By.className("channel-name")).timed().getText();
    }

    public List<String> getUnselectedNotifications() {
        List<String> selectedNotifications = new ArrayList<>();
        for (PageElement checkbox : container.findAll(By.cssSelector("input:not(:checked)"))) {
            selectedNotifications.add(checkbox.getAttribute("data-notification-name"));
        }
        return selectedNotifications;
    }

    public TimedCondition isArchived() {
        return container.find(By.cssSelector(".channel-name .aui-lozenge")).timed().isPresent();
    }

    public TimedCondition isExpanded() {
        return container.timed().hasClass("edit-active");
    }

    public TimedCondition isPrivate() {
        return Conditions.forMatcher(getChannelName(), new BaseMatcher<String>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Mapping row is not private");
            }

            @Override
            public boolean matches(Object item) {
                return item != null && item instanceof String &&
                        item.equals("Private channel (#" + getChannelId().now() + ")");
            }
        });
    }

    public TimedCondition toggleMapping(String mappingKey) {
        PageElement checkbox = container.find(By.id(mappingKey));
        return checkbox.click().timed().isSelected();
    }
}
