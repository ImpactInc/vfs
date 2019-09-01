create table blocks (
  dir int unsigned,
  seq int unsigned,
  data VARBINARY(8192),
  PRIMARY KEY (dir, seq))
