mysqlfs
=======

A NIO file system based on two MySQL tables. One for the directory
structure and one for the block storage. The block size was
arbitrarily chosen to be 8kB, which is probably too small for any
meaningful production use, but easy to change. There are probably
many opportunities for optimization. There is no form of caching
used anywhere.

There are limitations to the operations we support. Random access file
operations are supported only through a hack, where a temporary file
is written to the local file system. The actual random access happens
there, and on `close()` the file is copied back to the database
structure. That means any kind of concurrent access will most likely
lead to misery. In fact, concurrent operations aren't catered for in
any particular way, so there are likely scenarios where tha could lead
to trouble. On the other hand nothing is really cached, and the
database's transactional operations will guard against many of the
possible races.

Watches are not implemented at all. The primary use case is to read
and write files in their entirety, for example for serving them up in
a web like interface (WebDAV) or FTP.

The idea behind all this is that a typical clustered webapp needs some
form of shared storage. Much of it fits well in the relational
database, but some things are better accessed in a streaming fashion,
like files. For availability and disaster recovery we tend to
replicate our databases, and have rather nice systems for that. On the
file system side we have NFS, but operations teams typically don't
like to be responsible for wide area NFS setups. So, implementing the
file system interface with the DB lets us have only the DB
replication/backup/etc. to deal with operationally. Database snapshots
for testing and such also conveniently include file data as well as
table data.

Also, LOB operations in the JDBC interface are no fun. Often we end up
running out of memory or have to resort to some form of non standard
interface methods, perhaps both. The MySQL JDBC driver uses its own
hacks to implement the LOB operations, which is less than ideal.

Using a fairly standard file system inteface also makes it easier to
switch out different implementations. For example we can bolt a WebDAV
front end onto the mysql file system to mount it remotely in a standard
`macos` mount (Command+K in the Finder). I'm sure there is similar
support in Linux and in Microsoft Windows.


Any use of this code assumes the following two tables already present:

```
create table direntry (
  id int unsigned not null primary key auto_increment,
  parent int unsigned,
  type varchar(20),
  name varchar(500),
  content_type varchar(100),
  size int unsigned,
  ctime datetime,
  mtime datetime,
  atime datetime,
  KEY idx_parent_name (parent, name));

create table blocks (
  dir int unsigned,
  seq int unsigned,
  data VARBINARY(8192),
  PRIMARY KEY (dir, seq));

```

If desired the table names can be prefixed, if you need to adhere to a
naming convention. For example if you want the table to be named
`iram_fs_direntry` instead of just `direntry`, you can call the setter
on the file system provider: `provider.setTablePrefix("iram_fs_");`.


Portablility:
-------------

This depends on MySQL, but porting to any other relational DB should be
relatively easy. The code makes use of the MySQL specific `REPLACE`
statement, for convenience. Replacing that with `UPDATE ... ON DUPLICATE KEY UPDATE`
or simply a `DELETE` followed by `INSERT` should also work.

Another convenience is Spring Framework's `JdbcTemplate`, from which a handful
of methods are used. If the Spring dependency is a problem that will have to be
rewritten.
