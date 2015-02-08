package org.forkalsrud.webdav;

import java.io.File;

/**
 * Maps URL paths to file system paths and vice versa.
 * Heavily inspired by {@see org.apache.jackrabbit.webdav.AbstractLocatorFactory}
 * Three elements:
 * <ul>
 *     <li>prefix, in file system path</li>
 *     <li>workspace, the first path element following the prefix</li>
 *     <li>path, the remaining path elements after workspace</li>
 * </ul>
 */
public class FilesystemMapper {

    String fileSystemPrefix;
    File root;

    public FilesystemMapper(String fileSystemPrefix) {
        this.fileSystemPrefix = fileSystemPrefix;
        this.root = new File(fileSystemPrefix);
    }

    protected String getRepositoryPath(String resourcePath, String workspace) {
        StringBuilder buf = new StringBuilder(fileSystemPrefix);
        if (resourcePath != null && !"".equals(resourcePath)) {
            buf.append('/').append(resourcePath);
        }
        return buf.toString();
    }


    protected String getResourcePath(String fileSystemPath, String workspace) {
        String path = fileSystemPath;
        if (path.startsWith(fileSystemPrefix)) {
            path = path.substring(fileSystemPath.length());
        }
        if (path.startsWith(workspace)) {
            path = path.substring(workspace.length());
        }
        return path;
    }
}
