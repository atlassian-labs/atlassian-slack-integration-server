package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.bitbucket.element.IdOption;
import com.google.inject.Inject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openqa.selenium.By;

public class ChannelOption extends IdOption {
    @Inject
    public PageBinder pageBinder;

    public ChannelOption(@Nonnull PageElement container) {
        super(container);
    }

    @Nullable
    @Override
    public String getId() {
        return container.getText();
    }

    public boolean isPrivate() {
        return container.find(By.className("private-channel")).isPresent();
    }
}
