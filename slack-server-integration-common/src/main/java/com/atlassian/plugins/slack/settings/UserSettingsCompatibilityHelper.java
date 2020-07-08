package com.atlassian.plugins.slack.settings;

import com.atlassian.sal.api.usersettings.UserSettings;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;

/**
 * Reason for this class: UserSettings using Fugue === PITA
 * <p>
 * Different versions of our products provide different versions of Fugue.
 * Confluence 5, Jira 7 and Bitbucket 5 have com.atlassian.fugue.
 * Jira 8 and Bitbucket 6 have io.atlassian.fugue.
 * <p>
 * UserSettings is an API that makes use of Fugue directly. Whatever version of Fugue we compile our code with will
 * break the other. Also, since the plugin embed io.atlassian.fugue, there's no way to use product version of
 * its classes directly. Whatever combination we try we'll get a java.lang.LinkageError or similar.
 * <p>
 * Therefore, the only way to use provided io.atlassian.fugue classes without conflicting with our own embedded
 * classes is using reflection.
 * <p>
 * Alternatively, we could write another custom plugin that would provide a better interface for UserSettings for each
 * product version. That's just more cumbersome and probably slower than a couple of invocations via Reflection API.
 */
@UtilityClass
public class UserSettingsCompatibilityHelper {
    private static Method getBooleanMethod = MethodUtils
            .getAccessibleMethod(UserSettings.class, "getBoolean", String.class);
    private static Method getStringMethod = MethodUtils
            .getAccessibleMethod(UserSettings.class, "getString", String.class);
    private static Method newGetOrElse = MethodUtils
            .getAccessibleMethod(getBooleanMethod.getReturnType(), "getOrElse", Object.class);
    private static boolean isNewFugue = getBooleanMethod
            .getReturnType()
            .getPackage()
            .getName()
            .startsWith("io");

    public static boolean getBoolean(final UserSettings userSettings, final String key) {
        if (isNewFugue) {
            try {
                final Object option = getBooleanMethod.invoke(userSettings, key);
                return (boolean) newGetOrElse.invoke(option, Boolean.FALSE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return userSettings.getBoolean(key).getOrElse(false);
    }

    public static String getString(final UserSettings userSettings, final String key, final String defaultValue) {
        if (isNewFugue) {
            try {
                final Object option = getStringMethod.invoke(userSettings, key);
                return (String) newGetOrElse.invoke(option, defaultValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return userSettings.getString(key).getOrElse((String) null);
    }
}
