package it.com.atlassian.jira.plugins.slack.util;

import okhttp3.HttpUrl;

public class JiraFuncTestData {
    public static final String ADMIN_FULLNAME = "Administrator";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    public static final String USER_FULLNAME = "User";
    public static final String USER_USERNAME = "user";
    public static final String USER_PASSWORD = "user";

    public static final long PROJECT_ID = 10_000;
    public static final String PROJECT_KEY = "PRO";
    public static final String ISSUE_KEY = "PRO-1";
    public static final String CONFIGURATION_GROUP_ID = "0f1dd7b5-1b1b-48fd-a169-561f878a3e26";

    public static final String SAMPLE_DATA = "sample-project-two-users.zip";

    public static final HttpUrl DEFAULT_BASE_URL = HttpUrl.get("http://localhost:2990/jira");
}
