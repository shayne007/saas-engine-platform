package com.feng.engine.filesystem;

import java.nio.file.FileSystemException;

/**
 * 文件系统驱动抽象，通过SPI机制实现加载不同的实现类
 *
 * @since 2024/3/16
 */
public interface FsDriver {
    /**
     * 根据项目id获取文件系统
     *
     * @param projectId
     * @return
     */
    public FileSystem getFileSystem(long projectId) throws FileSystemException;
}
