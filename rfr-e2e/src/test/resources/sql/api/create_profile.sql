insert into `user` (username, country_id)
select :username, id
from `country`
where code = 'ru'
