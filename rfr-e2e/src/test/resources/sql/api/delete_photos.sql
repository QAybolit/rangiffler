delete
from `photo`
where user_id in (select id from `user` where username = :username)
