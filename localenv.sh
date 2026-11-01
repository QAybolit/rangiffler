#!/bin/bash

cd "$(dirname "$0")" || exit 1

existing=$(docker ps -aq --filter 'name=^/rfr-mysql$')
managed=$(docker ps -aq --filter 'name=^/rfr-mysql$' --filter 'label=com.docker.compose.project')

if [ -n "$existing" ] && [ -z "$managed" ]; then
  echo '### Remove rfr-mysql created without docker compose (data stays in mysql-volume) ###'
  docker rm -f rfr-mysql
fi

echo '### Run mysql db ###'
docker compose up -d --wait
