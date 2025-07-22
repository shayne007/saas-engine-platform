package com.feng.nfs.driver;

import com.feng.engine.filesystem.FileSystem;
import com.feng.engine.filesystem.IFile;
import java.nio.file.FileSystemException;
import java.util.List;

/**
 * NFS文件系统实现类
 *
 * @since 2024/3/16
 */
public class NfsFileSystem implements FileSystem {
    private long projectId;
    private String ip;

    public NfsFileSystem(long projectId) {
        this.projectId = projectId;
        this.ip = getNfsIp(projectId);
        mountNfs(ip);
    }

    private void mountNfs(String nfsIp) {
        // 文件服务器的提供一个共享目录：ip所在机器上的 /opt/project/{projectId}
        // 执行挂载命令 将文件服务的路径挂载至本机/media/{nfsip}/{projectId} ==> {nfsip}:/opt/project/{projectId}
        String command = "sudo ln " + "/media" + " " + getMountPoint(nfsIp) + " --symbolic";
        // Do execute command
    }

    private String getMountPoint(String ip) {
        return "";
    }

    private String getNfsIp(long projectId) {
        // 调用文件服务FileServerService接口，获取projectId相关文件的存放目录，
        // 若已存在直接返回分配的文件服务器ip，不存在则重新分配（FileServerService提供接口的内部实现逻辑）
        return "";
    }

    @Override
    public IFile getFile(String path) throws FileSystemException {
        return null;
    }

    @Override
    public List<IFile> listRemoteFiles(String path, boolean recursive) throws FileSystemException {
        return null;
    }
}
