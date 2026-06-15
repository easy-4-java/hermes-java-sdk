package io.github.hiwepy.hermes.exception;

import lombok.Getter;

@Getter
public class HermesHttpException extends HermesException {
    private final int statusCode;
    private final String responseBody;

    public HermesHttpException(int statusCode, String responseBody) {
        super("Hermes HTTP error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HermesHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }
}
