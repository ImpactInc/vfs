webdav
======

Adapter for Apache Jackrabbit's WebDAV module to work
with a `java.nio.Path` based repository, i.e. a basic
file system. The goal for this has been to enable the
`macos` webdav mount (Command + K in finder) to be able to
work with a remote file system, like `mysqlfs` in this
project.

The biggest challenges have been:

* Jackrabbit's repository has a notion of a "workspace"
  in addition to a path. Here the workspace is just
  another (top level) directory. Unfortunately one is
  needed, so you can't access just `/`.

* Jackrabbit's "resource" knows whether it is a "blob"
  or a "collection".  Thus the API is no distiction between
  creating a file versus a directory. On we WebDAV protocol
  side there is a difference, i.e. MKCOL versus PUT, so
  There's a bit of hackery in place to carry that through
  to `SimpleDavResource.addMember()`.
  The gist of it is a dynamic proxy on `InputContext` to
  expose a new property: "METHOD".


The Jackrabbit software is distributed under the Apache
license, so this module is also governed by that `LICENSE.txt`.
