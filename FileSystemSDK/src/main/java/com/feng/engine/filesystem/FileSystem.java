package com.feng.engine.filesystem;

import java.nio.file.FileSystemException;
import java.util.List;

/**
 * 文件系统抽象类，提供一套统一的文件操作方法
 *
 * @since 2024/3/16
 */
public interface FileSystem {
    /**
     * 根据指定路径获取一个文件
     *
     * @param path 文件路径
     * @return 文件
     * @throws FileSystemException 文件系统异常
     */
    IFile getFile(String path) throws FileSystemException;

    /**
     * 列出指定路径下文件列表
     *
     * @param path 文件路径
     * @param recursive 是否递归
     * @return 文件列表
     * @throws FileSystemException 文件系统异常
     */
    List<IFile> listRemoteFiles(String path, boolean recursive) throws FileSystemException;
}
