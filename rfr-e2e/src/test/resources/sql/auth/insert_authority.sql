insert into `authority` (user_id, authority)
select id, :authority
from `user`
where username = :username
