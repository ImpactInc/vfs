vfs
===

A collection of file system related libraries.

* `mysqlfs` is a `java.nio` file system implemented on top of two DB tables.

* `webdav` is an adapter to access a `java.nio` file system using WebDAV

* `ftpd` is an adapter to access a `java.nio` file system using FTP

* `examples` is a program to showcase the aforementioned libraries working together.



Quick Start
-----------

```
./gradlew :examples:shadowJar
java -jar examples/build/libs/examples-1.jar
```

Use `--help` on the latter to get summary information of options.

Without other options the command connects as user `root` without
password to the default MySQL instance on localhost. In the `test`
schema it adds two tables, `direntry` and `blocks`.
