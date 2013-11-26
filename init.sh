#!/bin/bash

# add index and intitalize extended tables
# mysql -u isucon isucon <<SQL
mysql -u root -proot isucon <<SQL
ALTER TABLE memos ADD INDEX (is_private, created_at);
ALTER TABLE memos ADD INDEX (user, created_at);
ALTER TABLE memos ADD INDEX (user, is_private, created_at);

UPDATE public_count SET cnt = 20540;
DELETE FROM public_memos WHERE id > 20540;
ALTER TABLE public_memos AUTO_INCREMENT = 20541;
ALTER TABLE memos ADD COLUMN ( title VARCHAR(255) );
UPDATE memos SET title = substring_index(content, "\n", 1);
SQL

# run initializer
curl http://localhost/init

wget -O /dev/null -q '127.0.0.1/'
wget -O /dev/null -q '127.0.0.1/signin'
(for s in `seq 1 210`; do wget -O /dev/null -q 127.0.0.1/recent/$s ;done)
echo "done"
