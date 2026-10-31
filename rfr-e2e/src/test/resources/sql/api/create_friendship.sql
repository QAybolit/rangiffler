insert into `friendship` (id, requester_id, addressee_id, status, created_at, responded_at)
select UUID_TO_BIN(UUID(), true),
       requester.id,
       addressee.id,
       :status,
       now(6),
       case when :status = 'ACCEPTED' then now(6) end
from `user` requester
         join `user` addressee on addressee.username = :addressee
where requester.username = :requester
