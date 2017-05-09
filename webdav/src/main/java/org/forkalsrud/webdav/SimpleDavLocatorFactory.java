package org.forkalsrud.webdav;

import java.nio.file.Path;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;


/**
 * Created by knut on 15/02/05.
 */
public class SimpleDavLocatorFactory implements DavLocatorFactory {

    private String urlPathPrefix;
    private Path root;

    public SimpleDavLocatorFactory(String urlPrefix, Path fileSystemPrefix) {
        this.urlPathPrefix = urlPrefix;
        this.root = fileSystemPrefix;
    }


    /**
     * Create a new <code>DavResourceLocator</code>.
     *
     * @param prefix String consisting of  [scheme:][//authority][path] where
     *               path defines the (imaginary) path to the {@link org.apache.jackrabbit.webdav.DavResourceLocator#isRootLocation root location}.
     * @param href   of the resource to be created. The given string may start with
     *               the 'prefix'. Please note, that in contrast to
     *               {@link org.apache.jackrabbit.webdav.DavLocatorFactory#createResourceLocator(String, String, String)} the
     *               href is expected to be URL encoded.
     * @return a new resource locator.
     */
    @Override
    public DavResourceLocator createResourceLocator(String prefix, String href) {
        if (href == null) {
            throw new IllegalArgumentException("Request handle must not be null.");
        }

        // build prefix string and remove all prefixes from the given href.
        StringBuffer b = new StringBuffer("");
        if (prefix != null && prefix.length() > 0) {
            b.append(prefix);
            if (href.startsWith(prefix)) {
                href = href.substring(prefix.length());
            }
        }
        if (urlPathPrefix != null && urlPathPrefix.length() > 0) {
            if (!b.toString().endsWith(urlPathPrefix)) {
                b.append(urlPathPrefix);
            }
            if (href.startsWith(urlPathPrefix)) {
                href = href.substring(urlPathPrefix.length());
            }
        }

        // remove trailing "/" that is present with collections
        if (href.endsWith("/")) {
            href = href.substring(0, href.length() - 1);
        }

        String resourcePath;
        String workspacePath;

        // an empty requestHandle (after removal of the "/") signifies a request
        // to the root that does not represent a repository item.
        if ("".equals(href)) {
            resourcePath = null;
            workspacePath = null;
        } else {
            resourcePath = EncodeUtil.unescape(href);
            // retrieve wspPath: look for the first slash ignoring the leading one
            int pos = href.indexOf('/', 1);
            if (pos == -1) {
                // request to a 'workspace' resource
                workspacePath = resourcePath;
            } else {
                // separate the workspace path from the resource path.
                workspacePath = EncodeUtil.unescape(href.substring(0, pos));
            }
        }

        return new SimpleResourceLocator(b.toString(), workspacePath, resourcePath, this);
    }

    /**
     * Create a new <code>DavResourceLocator</code>. This methods corresponds to
     * {@link org.apache.jackrabbit.webdav.DavLocatorFactory#createResourceLocator(String, String, String, boolean)}
     * with the flag set to true.
     *
     * @param prefix        String consisting of  [scheme:][//authority][path] where
     *                      path defines the path to the {@link org.apache.jackrabbit.webdav.DavResourceLocator#isRootLocation root location}.
     * @param workspacePath the first segment of the URIs path indicating the
     *                      workspace. The implementation may allow a empty String if workspaces
     *                      are not supported.
     * @param resourcePath  the URL decoded resource path.
     * @return a new resource locator.
     */
    @Override
    public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
        return new SimpleResourceLocator(prefix, workspacePath, resourcePath, this);
    }

    /**
     * @param prefix         String consisting of  [scheme:][//authority][path] where
     *                       path defines the path to the {@link org.apache.jackrabbit.webdav.DavResourceLocator#isRootLocation root location}.
     * @param workspacePath  the first segment of the URIs path indicating the
     *                       workspace. The implementation may allow a empty String if workspaces
     *                       are not supported.
     * @param path           the URL decoded path.
     * @param isResourcePath If true this method returns the same as
     *                       {@link org.apache.jackrabbit.webdav.DavLocatorFactory#createResourceLocator(String, String, String)},
     *                       otherwise the given path is treated as internal repository path.
     *                       The implementation may choose to implement a conversion of the repository
     *                       path to a valid resource path, e.g. (un)escaping of certain characters, due
     *                       to incompatibility with the URI definition (or vice versa). Note that
     *                       {@link org.apache.jackrabbit.webdav.DavResourceLocator#getRepositoryPath()} should in this case implement
     *                       the reverse operation.
     * @return a new resource locator.
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#getRepositoryPath()
     */
    @Override
    public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
        if (isResourcePath) {
            return createResourceLocator(prefix, workspacePath, path);
        }
        return createResourceLocator(prefix, workspacePath, getResourcePath(path, workspacePath));
    }

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


    public Path getRoot() {
        return root;
    }
}
