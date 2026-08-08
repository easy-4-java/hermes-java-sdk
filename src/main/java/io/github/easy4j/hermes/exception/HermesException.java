package io.github.easy4j.hermes.exception;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

public class HermesException extends RuntimeException {
    public HermesException(String message) {
        super(message);
    }
    public HermesException(String message, Throwable cause) {
        super(message, cause);
    }
}
