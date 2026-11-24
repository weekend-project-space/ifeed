package org.bitmagic.ifeed.infrastructure.spec;
/**
 * @author yangrd
 * @date 2025/11/8
 **/

public final class Spec {

    private Spec() {}

    /** 创建针对 T 的 Builder */
    public static <T> SpecBuilder<T> on(Class<T> entityType) {
        return new SpecBuilder<>();
    }

    /** 快捷创建（泛型推断） */
    public static <T> SpecBuilder<T> on() {
        return new SpecBuilder<>();
    }
}