package com.atlassian.jira.plugins.slack.model.dto;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * This class is going to handle any value that can be contained in a String and represent multiple values. This values
 * are going to work as follow 1) Can contain the word "ALL" 2) Can contain one value 3) Can contain multiple values
 * separated by comma
 */
public class MultipleValue {
    public static final String ALL = "ALL";
    public static final String SEPARATOR = ",";

    private final String rawValue;


    public MultipleValue(String rawValue) {
        this.rawValue = Strings.isNullOrEmpty(rawValue) ? ALL : rawValue;
    }

    public boolean isAll() {
        return ALL.equalsIgnoreCase(rawValue);
    }

    public String[] getValues() {

        String[] split = rawValue.split(SEPARATOR);

        // We need to trim each value to avoid spaces
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            split[i] = s.trim();
        }

        return split;
    }

    public List<String> getValuesAsList() {
        return Arrays.asList(getValues());
    }

    /**
     * Checks if the value given applies to this multiple value
     *
     * @param value the value to check
     * @return true if the value applies, false if not
     */
    public boolean apply(final String value) {

        if (isAll()) {
            return true;
        }

        // If we didn't get any value then we return false
        if (StringUtils.isBlank(value)) {
            return false;
        }

        final List<String> values = getValuesAsList();

        return values.stream().anyMatch(value::equalsIgnoreCase);
    }
}
