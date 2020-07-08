package com.atlassian.plugins.slack.admin;

public class ConfigurationError {
    private final Exception cause;
    private final Stage stage;
    private final String message;

    public ConfigurationError(Exception cause, Stage stage, String message) {
        this.cause = cause;
        this.stage = stage;
        this.message = message;
    }

    public Exception getCause() {
        return cause;
    }

    public Stage getStage() {
        return stage;
    }

    public String getMessage() {
        return message;
    }

    public enum Stage {
        Installation(0),
        TokenGeneration(1),
        UserCreation(2),
        UserTokenGeneration(3),
        UserDetails(4);

        private final int code;

        Stage(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
