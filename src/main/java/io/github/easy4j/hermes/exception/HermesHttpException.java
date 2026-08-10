package io.github.easy4j.hermes.exception;
import lombok.Getter;

/**
 * <p>Hermes HTTP 调用异常。</p>
 *
 * <p>保存 HTTP 状态码与响应体，或包装网络、序列化和异步执行失败。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Getter
public class HermesHttpException extends HermesException {
    /**
     * HTTP 响应状态码。
     */
    private final int statusCode;
    /**
     * HTTP 失败响应体。
     */
    private final String responseBody;

    /**
     * <p>创建 HermesHttpException 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param statusCode HTTP 响应状态码
     * @param responseBody HTTP 失败响应体
     * @since 1.0.0
     */
    public HermesHttpException(int statusCode, String responseBody) {
        super("Hermes HTTP error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * <p>创建 HermesHttpException 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param message 异常或诊断消息
     * @param cause 导致当前异常的原始原因
     * @since 1.0.0
     */
    public HermesHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }
}
