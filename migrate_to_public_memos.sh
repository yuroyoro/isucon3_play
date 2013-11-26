#!/bin/bash
mysql -u isucon isucon <<SQL
CREATE TABLE public_count (
  cnt int(11) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE public_memos (
  id int(11) NOT NULL AUTO_INCREMENT,
  memo int(11) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

ALTER TABLE memos ADD COLUMN ( title VARCHAR(255) );

INSERT INTO public_count(cnt) VALUES(20540);
INSERT INTO public_memos(memo) SELECT id FROM memos WHERE is_private = 0 ORDER BY id;

SQL
# ) | mysql -u root -proot isucon 2>/dev/null
#
