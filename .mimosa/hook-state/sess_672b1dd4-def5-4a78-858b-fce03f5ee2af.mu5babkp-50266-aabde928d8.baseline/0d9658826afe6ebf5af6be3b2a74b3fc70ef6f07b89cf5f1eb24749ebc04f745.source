package io.github.easy4j.hermes;

import lombok.Data;
import okhttp3.extension.logging.HttpLogLevel;

/**
 * Hermes SDK 统一调试配置，用于控制生命周期、请求头和正文日志。
 *
 * <p>调试默认关闭。正文日志始终受长度限制，认证头和敏感令牌仍由客户端脱敏。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
public class HermesDebugConfig {

    /** 是否允许 SDK 输出调试诊断信息。 */
    private boolean enabled;

    /** 启用调试后的详细程度。 */
    private HttpLogLevel level = HttpLogLevel.BASIC;

    /** BODY 级别单项正文允许记录的最大字符数。 */
    private int maxContentLength = 2_000;

    /**
     * 判断指定级别的日志是否允许输出。
     *
     * @param required 待输出信息要求的最低级别
     * @return 调试已启用且当前级别满足要求时返回 {@code true}
     */
    public boolean allows(HttpLogLevel required) {
        return enabled && level != null && level.allows(required);
    }

    /**
     * 返回经过下限保护的正文日志长度。
     *
     * @return 至少为 1 的最大正文字符数
     */
    public int resolveMaxContentLength() {
        return Math.max(1, maxContentLength);
    }
}
