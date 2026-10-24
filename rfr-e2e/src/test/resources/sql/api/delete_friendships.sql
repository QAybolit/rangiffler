delete
from `friendship`
where requester_id in (select id from `user` where username = :username)
   or addressee_id in (select id from `user` where username = :username)
