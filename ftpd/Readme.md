ftpd
====

A somewhat edited copy of Michael Lecuyer's
AXL FTP Server, based on version 3.09, from 1998-2002.
Edits include:

* Add a working EPASV command, required by modern FTP clients

* Remove most of the support for anonymous logins

* Interface with `java.nio.Path` instead of `java.io.File`,
  allowing for fronting for example `mysqlfs` in this project

* Fix a couple of smaller bugs, like the date format in xferlog
  and the source port for active mode connections


The original code was distributed under the LGPL license, so
this module is also covered by the same `license.txt`.


Maven Central
-------------

Maven dependency:

```
<dependency>
  <groupId>com.impact</groupId>
  <artifactId>vfs-ftpd</artifactId>
  <version>1.0</version>
</dependency>
```

Gradle:

```
implementation 'com.impact:vfs-ftpd:1.0'
```
