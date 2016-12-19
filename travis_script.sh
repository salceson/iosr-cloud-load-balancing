#!/usr/bin/env bash

docker network create iosr

docker run -d --network iosr --name supervisor iosr/supervisor

echo 'Waiting for supervisor startup'
for i in `seq 1 10`; do
  printf '.'
  sleep 1
done

docker run -d -v /var/run/docker.sock:/var/run/docker.sock --network iosr --name monitoring iosr/monitoring

echo 'Waiting for monitoring startup'
for i in `seq 1 10`; do
  printf '.'
  sleep 1
done

docker run -d --network iosr --name frontend iosr/frontend

echo 'Waiting for startup'
for i in `seq 1 30`; do
  printf '.'
  sleep 1
done

docker run -d --network iosr --name tester iosr/tester

echo 'Waiting for tests'
for i in `seq 1 10`; do
  printf '.'
  sleep 1
done

echo 'Supervisor logs'
docker logs supervisor
echo 'Monitoring logs'
docker logs monitoring
echo 'Tester logs'
docker logs tester
