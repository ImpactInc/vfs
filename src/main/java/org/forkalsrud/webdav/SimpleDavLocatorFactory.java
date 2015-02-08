package org.forkalsrud.webdav;

import org.apache.jackrabbit.webdav.AbstractLocatorFactory;

/**
 * Created by knut on 15/02/05.
 */
public class SimpleDavLocatorFactory extends AbstractLocatorFactory {

    FilesystemMapper fileSystemMapper;

    public SimpleDavLocatorFactory(String urlPathPrefix, String fileSystemPrefix) {
        super(urlPathPrefix);
        fileSystemMapper = new FilesystemMapper(fileSystemPrefix);
    }

    /**
     *
     * @param resourcePath
     * @param wspPath
     * @return
     * @see AbstractLocatorFactory#getRepositoryPath(String, String)
     */
    @Override
    protected String getRepositoryPath(String resourcePath, String wspPath) {
        if (resourcePath == null) {
            return null;
        }
        if (resourcePath.equals(wspPath)) {
            return null;
        }

        // a repository item  -> remove wspPath
        String pfx = wspPath;
        if (resourcePath.startsWith(pfx)) {
            String repositoryPath = resourcePath.substring(pfx.length());
            return (repositoryPath.length() == 0) ? "/" : repositoryPath;
        } else {
            throw new IllegalArgumentException("Unexpected format of resource path: " + resourcePath + " (workspace: " + wspPath + ")");
        }
    }

    /**
     *
     * @param repositoryPath
     * @param wspPath
     * @return
     * @see AbstractLocatorFactory#getResourcePath(String, String)
     */
    @Override
    protected String getResourcePath(String repositoryPath, String wspPath) {
        if (wspPath == null) {
            return null;
        }
        StringBuffer b = new StringBuffer(wspPath);
        if (repositoryPath != null) {
            if (!"/".equals(repositoryPath)) {
                b.append(repositoryPath);
            }
        }
        return b.toString();
    }

}
