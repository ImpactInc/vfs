Assumes these tables already present:

```
create table direntry (
  id int unsigned not null key auto_increment,
  parent int unsigned,
  type varchar(20),
  name varchar(500),
  content_type varchar(100),
  size int unsigned,
  data int unsigned,
  ctime datetime,
  mtime datetime,
  atime datetime);

create index idx_parent_name on direntry (parent, name);

create table filedata (
  id int unsigned not null key auto_increment,
  data longblob );
```
