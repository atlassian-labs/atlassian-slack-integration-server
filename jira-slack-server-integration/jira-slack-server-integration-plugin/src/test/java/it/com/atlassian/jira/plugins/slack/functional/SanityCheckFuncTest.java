package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.plugins.slack.test.RequestMatchers.success;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@SkipCacheCheck
public class SanityCheckFuncTest extends SlackFunctionalTestBase {

    public static final String LICENCE_FOR_TESTING = "AAAB8w0ODAoPeNp9Uk2P2jAQvedXWOoNydmELVKLFKlL4u7SLglKQj+27cEkA3gb7GjssMu/rwnQl\n" +
            "s9DDvHMvPfmvXmTN0BGfE08n3jdftfv927J/SgnXc9/58wRQC5UXQO6j6IAqYGVwgglAxbnLB2nw\n" +
            "4w5cbOcAiaziQbUge85oZKGFybmSwjKmiMKvfjATcW1Fly6hVo64waLBdcQcQPBhot6Per5zo4lX\n" +
            "9fQjofJaMTScHj3uC+x11rgup0b3z7sudiIi+oSWQa4AhxGweD+fU6/Tb68pZ+fnh7owPO/Os8Cu\n" +
            "VujKpvCuJsfqtXMvHAE1+KKFQQGG3A+2cp412XJeQjSHLVkzVQXKOrWn/bljH/nNmslXPa30+nES\n" +
            "U4/Jikdp0k0CfNhEtNJxmwhCBGsFSWZrolZANmhECYLVQISu9gzFIb8WBhT/+zf3MyVe2DOTbWdo\n" +
            "LCd+OWSSBGpDCmFNiimjQGLLDQxihSNNmppU3Yd67c0ILksjhOxqsKU3eUsooPvG4kXUrli/MlF7\n" +
            "dayEU7kb6lepJOxOLAf7XneFmkfCuCp95nh+LdwhfegL8E5l0LzNo4IVlApi0Vy0GZvs9O6b+vHZ\n" +
            "xzBv0toB3Yuk5lCwuualHs8fSD0/3NqdZ48nBd+5bjYilfNdokZr6zmP7TmY5YwLAIUNq8MbmR8G\n" +
            "faV9ulfLz1K+3g9j1YCFDeq7aYROMQbwMIvHimNt7/bJCCIX02nj";

    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA, LICENCE_FOR_TESTING);
    }

    @Test
    public void openIssuePage() {
        Response pageHtml = client.admin().visitPage("browse/PRO-1");
        assertThat(
                pageHtml,
                success(containsString("[PRO-1] This is your first task")));
    }

    @Test
    public void openGlobalConfigurationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/configure"),
                success(containsString("<h1>Slack integration</h1>")));
    }

    @Test
    public void openPersonalNotificationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/personal-notifications"),
                success(containsString("When issues that are assigned to me are updated")));
    }
}
