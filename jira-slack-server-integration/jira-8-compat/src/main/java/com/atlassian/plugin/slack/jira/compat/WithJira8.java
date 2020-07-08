package com.atlassian.plugin.slack.jira.compat;

import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.Callable;

@UtilityClass
public class WithJira8 {
    public static boolean isJira8OrGreater() {
        final BuildUtilsInfo buildUtilsInfo = new BuildUtilsInfoImpl();
        return buildUtilsInfo.getVersionNumbers()[0] >= 8;
    }

    public static <T> Optional<T> withJira8(final Callable<T> doThisIfJira8) throws Exception {
        return isJira8OrGreater() ? Optional.ofNullable(doThisIfJira8.call()) : Optional.empty();
    }
}
