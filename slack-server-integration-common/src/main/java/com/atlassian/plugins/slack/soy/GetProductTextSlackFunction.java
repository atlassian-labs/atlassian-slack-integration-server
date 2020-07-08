package com.atlassian.plugins.slack.soy;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.soy.renderer.JsExpression;
import com.atlassian.soy.renderer.SoyClientFunction;
import com.atlassian.soy.renderer.SoyServerFunction;
import com.atlassian.templaterenderer.JavaScriptEscaper;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Text function that sends the display name of the app as a first parameter
 * to be able to change the texts for Jira, Confluence, Bitbucket, etc.
 */
public class GetProductTextSlackFunction implements SoyClientFunction, SoyServerFunction<String> {
    // This is the amount of parameters we can hold in the soy function
    private static final int MAX_PARAMETERS = 20;

    private static final Set<Integer> ARGS_SIZES = Collections.unmodifiableSet(parameterRange());

    private final I18nResolver i18nResolver;
    private final ApplicationProperties applicationProperties;

    private static Set<Integer> parameterRange() {
        Set<Integer> parameterRange = new LinkedHashSet<>(MAX_PARAMETERS);
        for (int i = 1; i <= MAX_PARAMETERS; i++) {
            parameterRange.add(i);
        }
        return parameterRange;
    }

    public GetProductTextSlackFunction(
            final I18nResolver i18nResolver,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.i18nResolver = i18nResolver;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getName() {
        return "getProductText";
    }

    @Override
    public Set<Integer> validArgSizes() {
        return ARGS_SIZES;
    }

    /**
     * Does the i18n magic on the Javascript world.
     *
     * @param args the javascript arguments
     * @return the javascript expression
     */
    @Override
    public JsExpression generate(final JsExpression... args) {
        // Make sure the arguments are serializable...
        int argCount = args.length;
        String[] stringArgs = new String[argCount];
        for (int i = 0; i < argCount; i++) {
            if (i == 0) {
                String key = args[i].getText();
                // Strip the quote marks off the ends of the key string.
                if (key.length() > 2 && key.startsWith("'") && key.endsWith("'")) {
                    key = key.substring(1, key.length() - 1);
                }
                stringArgs[i] = key;
            } else {
                stringArgs[i] = args[i].getText();
            }
        }

        // Build the I18N text.
        String i18NText = getI18NText((Object[]) stringArgs);

        // Escape it.
        String escapedValue = JavaScriptEscaper.escape(i18NText);
        return new JsExpression("\"" + escapedValue + "\"");
    }

    /**
     * Does the i18n function magic in the Java world
     *
     * @param args the arguments from the Soy template. Details as follows:
     *             args[0]: the I18N property key
     *             args[1..n]: optional values to replace {1}..{n}
     * @return the message
     */
    @Override
    public String apply(final Object... args) {
        return getI18NText(args);
    }

    private String getI18NText(final Object... args) {
        final String key = String.valueOf(args[0]);
        final String displayName = applicationProperties.getDisplayName();
        final int argCount = args.length;
        if (argCount > 1) {
            Serializable[] augmentedArgs = new Serializable[argCount];
            System.arraycopy(args, 0, augmentedArgs, 0, argCount);
            augmentedArgs[0] = displayName;
            return i18nResolver.getText(key, augmentedArgs);
        } else {
            return i18nResolver.getText(key, displayName);
        }
    }

}
