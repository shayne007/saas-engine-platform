package com.feng.engine.filesystem;

import java.nio.file.FileSystemException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * TODO
 *
 * @since 2024/3/17
 */
public class FileSystemManager {
    private static final Map<String, FsDriver> DRIVER_MAP = new HashMap<>();

    private static void loadDrivers() throws FileSystemException {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                ServiceLoader<FsDriver> loader = ServiceLoader.load(FsDriver.class);
                Iterator<FsDriver> iterator = loader.iterator();
                try {
                    while (iterator.hasNext()) {
                        FsDriver driver = iterator.next();
                        DRIVER_MAP.put(driver.getClass().getName(), driver);
                    }
                }catch (Throwable t){

                }
                return null;
            }
        });
    }
}
