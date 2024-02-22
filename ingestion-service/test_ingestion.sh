#!/usr/bin/env bash
while true
do
  sleep 2
  curl -X POST http://localhost:3002/ingest \
  -H 'Content-Type: application/json' \
  -d '{
      "deviceId": "123",
      "deviceSync": 1,
      "stepsCount": 500 }' | jq
  echo
done
