package com.feng.engine.invoker;

/**
 * 服务间接口调用器
 *
 * @since 2024/3/19
 */
public class HSFInvoker {

    public static HSFInvoker create(String testServiceName, String s) {
        return new HSFInvoker();
    }

    public Object get(Class<String> stringClass) {
        return "success";
    }

    public Object asyncGet(Class<String> stringClass, ResponseListener listener) {
        return "success";
    }
}
