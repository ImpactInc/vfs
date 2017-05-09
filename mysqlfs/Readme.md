Assumes these tables already present:

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

create table blocks (dir int unsigned, seq int unsigned, data VARBINARY(8192), PRIMARY KEY (dir, seq));

```
