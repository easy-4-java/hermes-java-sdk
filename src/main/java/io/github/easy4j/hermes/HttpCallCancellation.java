package io.github.easy4j.hermes;

/**
 * 将业务层取消信号绑定到一次 Hermes HTTP 调用。
 */
@FunctionalInterface
public interface HttpCallCancellation {

    /**
     * 注册取消回调。
     *
     * @param callback 取消时执行的回调
     * @return 解除注册句柄
     */
    AutoCloseable onCancel(Runnable callback);
}
