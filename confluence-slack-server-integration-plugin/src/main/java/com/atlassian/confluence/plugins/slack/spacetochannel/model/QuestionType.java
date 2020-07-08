package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.content.CustomContentEntityObject;

import java.util.Optional;

public enum QuestionType {
    QUESTION("com.atlassian.confluence.plugins.confluence-questions:question", PageType.QUESTION),
    ANSWER("com.atlassian.confluence.plugins.confluence-questions:answer", PageType.ANSWER);

    String pluginModuleKey;
    PageType pageType;

    QuestionType(final String pluginModuleKey, final PageType pageType) {
        this.pluginModuleKey = pluginModuleKey;
        this.pageType = pageType;
    }

    public String pluginModuleKey() {
        return pluginModuleKey;
    }

    public PageType pageType() {
        return pageType;
    }

    public static Optional<QuestionType> from(final CustomContentEntityObject content) {
        if (isQuestionEvent(content)) {
            return Optional.of(QUESTION);
        }
        if (isAnswerEvent(content)) {
            return Optional.of(ANSWER);
        }
        return Optional.empty();
    }

    public static boolean isQuestionEvent(final CustomContentEntityObject content) {
        return QUESTION.pluginModuleKey.equals(content.getPluginModuleKey());
    }

    public static boolean isAnswerEvent(final CustomContentEntityObject content) {
        return ANSWER.pluginModuleKey.equals(content.getPluginModuleKey());
    }
}
