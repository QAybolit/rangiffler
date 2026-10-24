delete
from `like`
where user_id in (select id from `user` where username = :username)
