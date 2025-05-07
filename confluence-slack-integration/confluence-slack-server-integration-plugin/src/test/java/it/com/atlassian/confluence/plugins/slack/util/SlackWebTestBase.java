package it.com.atlassian.confluence.plugins.slack.util;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.plugins.slack.test.RuleAdaptorExtension;
import com.atlassian.webdriver.testing.annotation.WindowSize;
import com.atlassian.webdriver.testing.rule.SessionCleanupRule;
import com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule;
import com.atlassian.webdriver.testing.rule.WindowSizeRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

@Slf4j
@WindowSize(width = 1260, height = 768)
public abstract class SlackWebTestBase extends SlackFunctionalTestBase {
    @RegisterExtension
    protected static ConfluenceWebTestExtension confluence = new ConfluenceWebTestExtension(() -> {
        final ProductInstance instance = client.instance();
        System.setProperty("test.url", instance.getBaseUrl());
        return instance;
    });

    @RegisterExtension
    static RuleAdaptorExtension windowSizeRule = new RuleAdaptorExtension(new WindowSizeRule());
    @RegisterExtension
    static RuleAdaptorExtension sessionCleanupRule = new RuleAdaptorExtension(new SessionCleanupRule());
    @RegisterExtension
    static RuleAdaptorExtension webDriverScreenshotRule = new RuleAdaptorExtension(new WebDriverScreenshotRule());

    protected void closeAuiFlags(final long sleepMsBeforeChecking) {
        try {
            Thread.sleep(sleepMsBeforeChecking);
        } catch (InterruptedException e) {
            throw new RuntimeException("Waiting before AUI flags check failed", e);
        }

        WebDriver driver = confluence.get().getTester().getDriver();
        List<WebElement> flags = driver.findElements(By.className("aui-flag"));
        log.debug("{} open AUI flags found", flags.size());

        if (flags.isEmpty()) {
            return;
        }

        // attempt #1. try to close them using JS. doesn't work sometimes for unknown reasons
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        executor.executeScript("AJS.$('.aui-flag').each(function(it) { this.close(); } );");

        // attempt #2. click on flag 'close' buttons
        flags = driver.findElements(By.className("aui-flag"));
        for (WebElement flag : flags) {
            try {
                flag.findElement(By.className("aui-close-button")).click();
            } catch (NoSuchElementException | ElementNotInteractableException e) {
                // ignore intentionally; the flag is already closed
            }
        }

        // verify that all of them were closed
        for (int i = 0; i < flags.size(); i++) {
            WebElement flag = flags.get(i);
            Poller.waitUntilFalse("AUI flag failed to be closed", Conditions.forSupplier(5 * 1000, flag::isDisplayed));

            log.debug("AUI flag #{} isn't displayed anymore", i);
        }
    }
}
