package io.github.easy4j.hermes;
/**
 * <p>Hermes HTTP 对话响应模式。</p>
 *
 * <p>区分完整响应与流式响应语义，默认使用兼容的完整响应模式。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public enum HttpResponseMode {

    /**
     * 等待服务端完成后返回类型化完整响应。
     */
    BLOCKING,

    /**
     * 通过 SSE 持续返回文本和事件增量。
     */
    STREAM,

    /**
     * 由调用方法或请求参数自动选择完整响应或流式响应。
     */
    AUTO
}
