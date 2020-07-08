package it.com.atlassian.confluence.plugins.slack.util;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.plugins.slack.test.RuleAdaptorExtension;
import com.atlassian.webdriver.testing.annotation.WindowSize;
import com.atlassian.webdriver.testing.rule.SessionCleanupRule;
import com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule;
import com.atlassian.webdriver.testing.rule.WindowSizeRule;
import org.junit.jupiter.api.extension.RegisterExtension;

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
}
