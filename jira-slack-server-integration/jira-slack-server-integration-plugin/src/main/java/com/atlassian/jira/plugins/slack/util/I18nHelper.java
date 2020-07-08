package com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;

import java.io.Serializable;

import static com.google.common.base.Suppliers.memoize;

public final class I18nHelper {
    private static Supplier<I18nResolver> i18nResolverSupplier = getMemoizedI18nResolverSupplier();

    private I18nHelper() {
    }

    public static String getText(final String key) {
        return i18nResolverSupplier.get().getRawText(key);
    }

    public static String getText(final String key, final Serializable... arguments) {
        return i18nResolverSupplier.get().getText(key, arguments);
    }

    @VisibleForTesting
    public static void resetCache() {
        i18nResolverSupplier = getMemoizedI18nResolverSupplier();
    }

    private static Supplier<I18nResolver> getMemoizedI18nResolverSupplier() {
        return memoize(() -> ComponentAccessor.getOSGiComponentInstanceOfType(I18nResolver.class));
    }
}
