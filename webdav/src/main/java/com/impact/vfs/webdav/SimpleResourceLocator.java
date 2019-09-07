package org.forkalsrud.webdav;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;

import java.nio.file.Path;


/**
 * Created by knut on 15/02/08.
 */
public class SimpleResourceLocator implements DavResourceLocator {

    private String prefix;
    private String workspacePath;
    private String resourcePath;
    private SimpleDavLocatorFactory factory;
    private String href;
    private Path path;

    public SimpleResourceLocator(String prefix, String workspacePath, String resourcePath, SimpleDavLocatorFactory factory) {
        this.prefix = prefix;
        this.workspacePath = workspacePath;
        this.resourcePath = resourcePath;
        this.factory = factory;

        if (resourcePath != null && resourcePath.length() > 0) {
            // check if condition is really met
            if (!resourcePath.startsWith(workspacePath)) {
                throw new IllegalArgumentException("Resource path '" + resourcePath + "' does not start with workspace path '" + workspacePath + ".");
            }
        }

        this.href = computeHref(prefix, resourcePath != null ? resourcePath : "");

        String pathRelativeToRoot = resourcePath;
        while (pathRelativeToRoot != null && pathRelativeToRoot.startsWith("/")) {
            pathRelativeToRoot = pathRelativeToRoot.substring(1);
        }
        Path root = factory.getRoot();
        this.path = pathRelativeToRoot != null ? root.resolve(pathRelativeToRoot) : root;
    }

    private String computeHref(String prefix, String resourcePath) {
        StringBuffer buf = new StringBuffer(prefix);
        // NOTE: no need to append the workspace path, since it is must
        // be part of the resource path.
        if (resourcePath != null) {
            buf.append(EncodeUtil.escapePath(resourcePath));
        }
        int length = buf.length();
        if (length == 0 || (length > 0 && buf.charAt(length - 1) != '/')) {
            buf.append("/");
        }
        return buf.toString();
    }


    public SimpleResourceLocator(SimpleResourceLocator other, Path path) {
        this.prefix = other.prefix;
        this.workspacePath = other.workspacePath;
        this.factory = other.factory;
        this.path = path;
        this.resourcePath = getRepositoryPath();
        this.href = computeHref(this.prefix, this.resourcePath);
    }

    /**
     * Return the prefix used to build the complete href of the resource as
     * required for the href XML element.
     * This includes scheme and host information as well as constant prefixes.
     * However, this must not include workspace prefix.
     *
     * @return prefix needed in order to build the href from a resource path.
     * @see #getResourcePath()
     */
    @Override
    public String getPrefix() {
        return prefix;
    }

    /**
     * Return the resource path.
     *
     * @return resource path
     */
    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Return the path of the workspace the resource identified by this
     * locator is member of.
     *
     * @return path of the workspace
     */
    @Override
    public String getWorkspacePath() {
        return workspacePath;
    }

    /**
     * Return the name of the workspace the resource identified by this
     * locator is member of.
     *
     * @return workspace name
     */
    @Override
    public String getWorkspaceName() {
        if (workspacePath != null && workspacePath.length() > 0) {
            return workspacePath.substring(1);
        }
        return null;
    }

    /**
     * Returns true if the specified locator refers to a resource within the
     * same workspace.
     *
     * @param locator
     * @return true if both paths are in the same workspace.
     */
    @Override
    public boolean isSameWorkspace(DavResourceLocator locator) {
        return locator != null && isSameWorkspace(locator.getWorkspaceName());
    }

    /**
     * Returns true if the specified workspace name equals to the workspace
     * name defined with this locator.
     *
     * @param workspaceName
     * @return true if workspace names are equal.
     */
    @Override
    public boolean isSameWorkspace(String workspaceName) {
        String thisWspName = getWorkspaceName();
        return (thisWspName == null) ? workspaceName == null : thisWspName.equals(workspaceName);
    }

    /**
     * Return the 'href' representation of this locator object. The implementation
     * should perform an URL encoding of the resource path.
     *
     * @param isCollection
     * @return 'href' representation of this path
     */
    @Override
    public String getHref(boolean isCollection) {
        return isCollection ? this.href : this.href.substring(0, this.href.length() - 1);
    }

    /**
     * Returns true if this <code>DavResourceLocator</code> represents the root
     * locator that would be requested with 'hrefPrefix'+'pathPrefix' with or
     * without a trailing '/'.
     *
     * @return true if this locator object belongs to the root resource.
     */
    @Override
    public boolean isRootLocation() {
        return getWorkspacePath() == null;
    }

    /**
     * Return the locator factory that created this locator.
     *
     * @return the locator factory
     */
    @Override
    public DavLocatorFactory getFactory() {
        return factory;
    }

    /**
     * An implementation may choose to circumvent the incompatibility of a
     * repository path with the URI path by applying an appropriate conversion.
     * This utility method allows to retrieve this transformed repository path.
     * By default this method should return the same as {@link #getResourcePath()}
     *
     * @return a repository compatible form if the resource path.
     * @see org.apache.jackrabbit.webdav.DavLocatorFactory#createResourceLocator(String, String, String, boolean)
     * that allows to build a valid <code>DavResourceLocator</code> from a given
     * repository path.
     */
    @Override
    public String getRepositoryPath() {
        Path fullPath = path.toAbsolutePath();
        return "/" + factory.getRoot().relativize(fullPath).toString();
    }

    public Path getPath() {
        return path;
    }
}
