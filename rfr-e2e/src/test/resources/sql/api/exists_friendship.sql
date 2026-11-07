select count(*)
from `friendship` f
         join `user` requester on requester.id = f.requester_id
         join `user` addressee on addressee.id = f.addressee_id
where requester.username = :requester
  and addressee.username = :addressee
  and f.status = :status
