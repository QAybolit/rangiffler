#!/bin/bash

cd "$(dirname "$0")" || exit 1

if docker inspect rfr-mysql >/dev/null 2>&1 \
  && [ -z "$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' rfr-mysql)" ]; then
  echo '### Remove rfr-mysql created without docker compose (data stays in mysql-volume) ###'
  docker rm -f rfr-mysql
fi

echo '### Run mysql db ###'
docker compose up -d --wait
