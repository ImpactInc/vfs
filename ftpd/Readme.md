ftpd
====

A somewhat edited copy of Michael Lecuyer's
AXL FTP Server, based on version 3.09, from about 2002.
Edits include:

* Add a working EPASV command, required by modern FTP clients

* Remove most of the support for anonymous logins

* Interface with `java.nio.Path` instead of `java.io.File`,
  allowing for fronting for example `mysqlfs` in this project

* Fix a couple of smaller bugs, like the date format in xferlog
  and the source port for active mode connections

