package com.feng.engine.invoker;

import org.junit.Assert;
import org.junit.Test;

public class HSFInvokerTest {
    @Test
    public void getString_work_if_path_right() {
        HSFInvoker invoker = HSFInvoker.create("TestServiceName", "/rest/testService/path");
        String result = (String)invoker.get(String.class);
        Assert.assertEquals("success", result);
    }

    @Test
    public void asyncGetString_work_if_path_right() {
        HSFInvoker invoker = HSFInvoker.create("TestServiceName", "/rest/testService/path");
        ResponseListener listener = new ResponseListener();
        String result = (String)invoker.asyncGet(String.class, listener);
        Assert.assertEquals("success", result);
    }
}