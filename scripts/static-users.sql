insert into `rangiffler-auth`.`user` (username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
select pool.username, '{bcrypt}$2y$10$S9EoL8GhxIhvplW4ImFKF.3wF3YWW7Jb.vLJcNmfKkJ81vTWsaWiS', true, true, true, true
from (
    select 'static-user-1' as username
    union all select 'static-user-2'
    union all select 'static-user-3'
) pool
where not exists (
    select 1 from `rangiffler-auth`.`user` existing where existing.username = pool.username
);

insert into `rangiffler-auth`.`authority` (user_id, authority)
select account.id, grant_type.authority
from `rangiffler-auth`.`user` account
cross join (
    select 'read' as authority
    union all select 'write'
) grant_type
where account.username in ('static-user-1', 'static-user-2', 'static-user-3')
  and not exists (
    select 1 from `rangiffler-auth`.`authority` existing
    where existing.user_id = account.id and existing.authority = grant_type.authority
);

insert into `rangiffler-api`.`user` (username, country_id)
select pool.username, country.id
from (
    select 'static-user-1' as username
    union all select 'static-user-2'
    union all select 'static-user-3'
) pool
join `rangiffler-api`.`country` country on country.code = 'ru'
where not exists (
    select 1 from `rangiffler-api`.`user` existing where existing.username = pool.username
);
