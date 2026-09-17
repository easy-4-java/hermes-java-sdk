package io.github.easy4j.hermes;
/**
 * <p>一次 Hermes HTTP 调用的取消信号接口。</p>
 *
 * <p>实现方注册底层取消动作并返回注销句柄，使业务取消能够安全传播到 OkHttp Call。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
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
