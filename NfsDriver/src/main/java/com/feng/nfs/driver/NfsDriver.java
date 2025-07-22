package com.feng.nfs.driver;

import com.feng.engine.filesystem.FileSystem;
import com.feng.engine.filesystem.FsDriver;
import java.lang.reflect.Method;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * TODO
 *
 * @since 2024/3/16
 */
public class NfsDriver implements FsDriver {
    @Override
    public FileSystem getFileSystem(long projectId) {
        return NfsFileSystemProxy.getFileSystemInstance(projectId);
    }

    private static class NfsFileSystemProxy implements MethodInterceptor {
        public static FileSystem getFileSystemInstance(long projectId) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(NfsFileSystem.class);
            enhancer.setCallback(new NfsFileSystemProxy());
            return (FileSystem)enhancer.create(new Class[] {long.class}, new Long[] {projectId});
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            return null;
        }
    }
}
