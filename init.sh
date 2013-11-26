#!/bin/bash

# add index and intitalize extended tables
# mysql -u root -proot isucon <<SQL
mysql -u isucon isucon <<SQL
ALTER TABLE memos ADD INDEX (is_private, created_at);
ALTER TABLE memos ADD INDEX (user, created_at);
ALTER TABLE memos ADD INDEX (user, is_private, created_at);

UPDATE public_count SET cnt = 20540;
DELETE FROM public_memos WHERE id > 20540;
ALTER TABLE public_memos AUTO_INCREMENT = 20541;
UPDATE memos SET title = substring_index(content, "\n", 1);
SQL

# run initializer
curl http://localhost:9000/init &


SELECT
  m.id,
  m.user,
  m.title,
  m.created_at,
  m.updated_at
FROM memos AS m
JOIN public_memos AS pm
  ON pm.memo = m.id
ORDER BY m.id LIMIT 100
