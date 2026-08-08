package io.github.easy4j.hermes;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

/**
 * HTTP 对话响应模式。
 */
public enum HttpResponseMode {

    /** 等待完整响应后一次性返回。 */
    BLOCKING,

    /** 消费 Provider SSE 并逐段回调。 */
    STREAM,

    /** 由调用方根据结构化输出等请求特征选择。 */
    AUTO
}
