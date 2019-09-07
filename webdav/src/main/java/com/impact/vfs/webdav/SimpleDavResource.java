package org.forkalsrud.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.util.HttpDateFormat;

/**
 * The simplest of WebDAV resources, backed by a the file system with java.nio.file.Path
 * Created by knut on 15/02/05.
 */
public class SimpleDavResource implements DavResource {

    public static final String COMPLIANCE_CLASSES = DavCompliance.concatComplianceClasses(
            new String[] {
                    DavCompliance._1_,
                    DavCompliance._2_,
                    DavCompliance._3_,
            }
    );


    private SimpleResourceLocator locator;
    private DavResourceFactory factory;
    private LockManager lockManager;
    private DavSession session;

    private Path path;

    protected DavPropertySet properties = new DavPropertySet();
    protected boolean propsInitialized = false;

    public SimpleDavResource(SimpleResourceLocator locator, DavResourceFactory factory, LockManager lockManager, DavSession session) {
        this.locator = locator;
        this.factory = factory;
        this.lockManager = lockManager;
        this.session = session;
        this.path = locator.getPath();
    }

    public SimpleDavResource(SimpleDavResource other, Path path) {
        this.locator = new SimpleResourceLocator(other.locator, path);
        this.factory = other.factory;
        this.lockManager = other.lockManager;
        this.session = other.session;
        this.path = path;
    }

    /**
     * Returns a comma separated list of all compliance classes the given
     * resource is fulfilling.
     *
     * @return compliance classes
     */
    @Override
    public String getComplianceClass() {
        return COMPLIANCE_CLASSES;
    }

    /**
     * Returns a comma separated list of all METHODS supported by the given
     * resource.
     *
     * @return METHODS supported by this resource.
     */
    @Override
    public String getSupportedMethods() {
        return METHODS;
    }

    /**
     * Returns true if this webdav resource represents an existing repository item.
     *
     * @return true, if the resource represents an existing repository item.
     */
    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    /**
     * Returns true if this webdav resource has the resourcetype 'collection'.
     *
     * @return true if the resource represents a collection resource.
     */
    @Override
    public boolean isCollection() {
        return Files.isDirectory(path);
    }

    /**
     * Returns the display name of this resource.
     *
     * @return display name.
     */
    @Override
    public String getDisplayName() {
        return path.getFileName().toString();
    }

    /**
     * Returns the {@link org.apache.jackrabbit.webdav.DavResourceLocator locator} object for this webdav resource,
     * which encapsulates the information for building the complete 'href'.
     *
     * @return the locator for this resource.
     * @see #getResourcePath()
     * @see #getHref()
     */
    @Override
    public DavResourceLocator getLocator() {
        return locator;
    }

    /**
     * Returns the path of the hierarchy element defined by this <code>DavResource</code>.
     * This method is a shortcut for <code>DavResource.getLocator().getResourcePath()</code>.
     *
     * @return path of the element defined by this <code>DavResource</code>.
     */
    @Override
    public String getResourcePath() {
        return locator.getResourcePath();
    }

    /**
     * Returns the absolute href of this resource as returned in the
     * multistatus response body.
     *
     * @return href
     */
    @Override
    public String getHref() {
        return locator.getHref(isCollection());
    }

    /**
     * Return the time of the last modification or -1 if the modification time
     * could not be retrieved.
     *
     * @return time of last modification or -1.
     */
    @Override
    public long getModificationTime() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Spools the resource properties and ev. content to the specified context
     * (e.g. to respond to a 'GET' or 'HEAD' request). The context could e.g.
     * wrap the servlet response.
     *
     * @param outputContext The output context.
     * @throws java.io.IOException If an error occurs.
     */
    @Override
    public void spool(OutputContext outputContext) throws IOException {

        OutputStream dst = outputContext.getOutputStream();
        Files.copy(path, dst);
        dst.close();
    }

    /**
     * @see DavResource#getProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public DavProperty<?> getProperty(DavPropertyName name) {
        initProperties();
        return properties.get(name);
    }

    /**
     * @see DavResource#getProperties()
     */
    public DavPropertySet getProperties() {
        initProperties();
        return properties;
    }

    /**
     * @see DavResource#getPropertyNames()
     */
    public DavPropertyName[] getPropertyNames() {
        return getProperties().getPropertyNames();
    }


    /**
     * @param property
     * @throws DavException
     * @see DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     */
    public void setProperty(DavProperty<?> property) throws DavException {
        alterProperty(property);
    }

    /**
     * @param propertyName
     * @throws DavException
     * @see DavResource#removeProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        alterProperty(propertyName);
    }

    private void alterProperty(PropEntry prop) throws DavException {
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
    }




    public static String getRelativeParent(String path, int level) {
        int idx = path.length();
        while (level > 0) {
            idx = path.lastIndexOf('/', idx - 1);
            if (idx < 0) {
                return "";
            }
            level--;
        }
        return (idx == 0) ? "/" : path.substring(0, idx);
    }


    /**
     * Retrieve the resource this resource is internal member of.
     *
     * @return resource this resource is an internal member of. In case this resource
     * is the root <code>null</code> is returned.
     */
    @Override
    public DavResource getCollection() {
        DavResource parent = null;
        if (getResourcePath() != null && !getResourcePath().equals("/")) {
            String parentPath = getRelativeParent(getResourcePath(), 1);
            if (parentPath.equals("")) {
                parentPath = "/";
            }
            DavResourceLocator parentloc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), parentPath);
            try {
                parent = factory.createResource(parentloc, session);
            } catch (DavException e) {
                // should not occur
            }
        }
        return parent;
    }

    /**
     * Returns an iterator over all internal members.
     *
     * @return a {@link org.apache.jackrabbit.webdav.DavResourceIterator} over all internal members.
     */
    @Override
    public DavResourceIterator getMembers() {

        List<DavResource> list = new ArrayList<DavResource>();
        if (exists() && isCollection()) {
            try {
                for (Path child : Files.newDirectoryStream(path)) {
                    list.add(new SimpleDavResource(this, child));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new DavResourceIteratorImpl(list);
    }


    /**
     * Add the given resource as an internal member to this resource.
     *
     * @param resource     {@link org.apache.jackrabbit.webdav.DavResource} to be added as internal member.
     * @param inputContext Context providing the properties and content for the
     *                     internal member to be created or replaced.
     * @throws org.apache.jackrabbit.webdav.DavException
     */
    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_CONFLICT);
        }
        if (isLocked(this) || isLocked(resource)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        try {
            String memberPath = resource.getLocator().getRepositoryPath();
            String memberName = memberPath.substring(memberPath.lastIndexOf('/') + 1);

            Path p = this.path.resolve(memberName);
            if ("MKCOL".equalsIgnoreCase(inputContext.getProperty("METHOD"))) {
                Files.createDirectory(p);
            } else {
                InputStream src = inputContext.getInputStream();
                Files.copy(src, p, StandardCopyOption.REPLACE_EXISTING);
                src.close();
            }
        } catch (IOException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Removes the specified member from this resource.
     *
     * @param member
     * @throws org.apache.jackrabbit.webdav.DavException
     */
    @Override
    public void removeMember(DavResource member) throws DavException {
        if (!exists() || !member.exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (isLocked(this) || isLocked(member)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }

        try {
            String memberPath = member.getLocator().getRepositoryPath();
            String memberName = memberPath.substring(memberPath.lastIndexOf('/') + 1);
            
            Path p = path.resolve(memberName);
            Files.delete(p);
        } catch (IOException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Move this DavResource to the given destination resource
     *
     * @param destination
     * @throws org.apache.jackrabbit.webdav.DavException
     */
    @Override
    public void move(DavResource destination) throws DavException {
        try {
            Files.move(path, ((SimpleDavResource)destination).path);
        } catch (IOException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Copy this DavResource to the given destination resource
     *
     * @param destination
     * @param shallow
     * @throws org.apache.jackrabbit.webdav.DavException
     */
    @Override
    public void copy(DavResource destination, boolean shallow) throws DavException {

        final Path destinationFile = ((SimpleDavResource)destination).path;
        try {
            if (Files.isDirectory(path)) {
                Files.createDirectory(destinationFile);
                if (!shallow) {
                    final Path srcRoot = path;
                    final Path dstRoot = destinationFile;
    
                    Files.walkFileTree(path, new FileVisitor<Path>() {
                        
                        Path dstFor(Path src) {
                            return dstRoot.resolve(srcRoot.relativize(src));
                        }
                        
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                                Files.createDirectory(dstFor(dir));
                            return FileVisitResult.CONTINUE;
                        }
            
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.copy(file, dstFor(file));
                            return FileVisitResult.CONTINUE;
                        }
            
                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
            
                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } else if (Files.isRegularFile(path)) {
                Files.copy(path, destinationFile);
            }
        } catch (IOException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }


    /**
     * @param type
     * @param scope
     * @return true if type is {@link Type#WRITE} and scope is {@link Scope#EXCLUSIVE}
     * @see DavResource#isLockable(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope)
     */
    public boolean isLockable(Type type, Scope scope) {
        return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope);
    }

    /**
     * @see DavResource#hasLock(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope)
     */
    public boolean hasLock(Type type, Scope scope) {
        return getLock(type, scope) != null;
    }

    /**
     * @see DavResource#getLock(Type, Scope)
     */
    public ActiveLock getLock(Type type, Scope scope) {
        ActiveLock lock = null;
        if (exists() && Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope)) {
            lock = lockManager.getLock(type, scope, this);
        }
        return lock;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getLocks()
     */
    public ActiveLock[] getLocks() {
        ActiveLock writeLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        return (writeLock != null) ? new ActiveLock[]{writeLock} : new ActiveLock[0];
    }

    /**
     * @see DavResource#lock(LockInfo)
     */
    public ActiveLock lock(LockInfo lockInfo) throws DavException {
        ActiveLock lock = null;
        if (isLockable(lockInfo.getType(), lockInfo.getScope())) {
            lock = lockManager.createLock(lockInfo, this);
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Unsupported lock type or scope.");
        }
        return lock;
    }

    /**
     * @see DavResource#refreshLock(LockInfo, String)
     */
    public ActiveLock refreshLock(LockInfo lockInfo, String lockToken) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope());
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "No lock with the given type/scope present on resource " + getResourcePath());
        }
        lock = lockManager.refreshLock(lockInfo, lockToken, this);
        /* since lock has infinite lock (simple) or undefined timeout (jcr)
           return the lock as retrieved from getLock. */
        return lock;
    }

    /**
     * @see DavResource#unlock(String)
     */
    public void unlock(String lockToken) throws DavException {
        ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        } else if (lock.isLockedByToken(lockToken)) {
            lockManager.releaseLock(lockToken, this);
        } else {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    /**
     * @see DavResource#addLockManager(org.apache.jackrabbit.webdav.lock.LockManager)
     */
    public void addLockManager(LockManager lockMgr) {
        this.lockManager = lockMgr;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getFactory()
     */
    public DavResourceFactory getFactory() {
        return factory;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getSession()
     */
    public DavSession getSession() {
        return session;
    }




    /**
     * Return true if this resource cannot be modified due to a write lock
     * that is not owned by the given session.
     *
     * @return true if this resource cannot be modified due to a write lock
     */
    private boolean isLocked(DavResource res) {
        ActiveLock lock = res.getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock == null) {
            return false;
        } else {
            for (String sLockToken : session.getLockTokens()) {
                if (sLockToken.equals(lock.getToken())) {
                    return false;
                }
            }
            return true;
        }
    }





    protected void initProperties() {
        if (!exists() || propsInitialized) {
            return;
        }
    
        BasicFileAttributeView attrView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        try {
            BasicFileAttributes attrs = attrView.readAttributes();
            properties.add(new DefaultDavProperty<String>(DavPropertyName.DISPLAYNAME, getDisplayName()));

            if (attrs.isDirectory()) {
                properties.add(new ResourceType(ResourceType.COLLECTION));
                // Windows XP support
                properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "1"));
            }
            if (attrs.isRegularFile()) {
                properties.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
                // Windows XP support
                properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "0"));
            }
            HttpDateFormat fmt = HttpDateFormat.modificationDateFormat();
            long modified = attrs.lastModifiedTime() != null ? attrs.lastModifiedTime().toMillis() : 0L;
            String lastModifiedStr = fmt.format(new Date(modified));
            properties.add(new DefaultDavProperty<String>(DavPropertyName.GETLASTMODIFIED, lastModifiedStr));
            long created = attrs.creationTime() != null ? attrs.creationTime().toMillis() : 0L;
            String createdStr = fmt.format(new Date(created));
            properties.add(new DefaultDavProperty<String>(DavPropertyName.CREATIONDATE, createdStr));

            properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTLENGTH, String.valueOf(attrs.size())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* set current lock information. If no lock is set to this resource,
        an empty lock discovery will be returned in the response. */
        properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));

        /* lock support information: all locks are lockable. */
        SupportedLock supportedLock = new SupportedLock();
        supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
        properties.add(supportedLock);

        propsInitialized = true;
    }
}
