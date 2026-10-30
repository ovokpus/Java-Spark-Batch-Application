#!/bin/sh
# Runs the batch pipeline once:
#   1. ingesterJob      sales.json -> SALES (mode=overwrite, so a re-run does not double count)
#   2. salesSummaryJob  for each date x product -> SALES_SUMMARY (idempotent per seller/date/item)
# Every execution is recorded in JOB_RUN. Pass arguments to run a single job instead, e.g.
#   docker compose run --rm batch jobName=salesSummaryJob sellerId=Joe,Beth date=20201111000000 product=Beer
set -e
job() { java $JAVA_OPTS -cp "/app/classes:/app/lib/*" com.jmb.batchapp.SparkBatchApp "$@"; }

if [ "$#" -gt 0 ]; then job "$@"; exit $?; fi

SELLERS="${SELLERS:-Joe,Beth,Janet}"
DATES="${DATES:-20201111000000 20201112000000 20201113000000}"
PRODUCTS="${PRODUCTS:-Beer Food Car}"

echo ">> ingesterJob"
job jobName=ingesterJob clientId=client1 readFormat=json fileName=sales mode=overwrite

for d in $DATES; do
  for p in $PRODUCTS; do
    echo ">> salesSummaryJob date=$d product=$p"
    job jobName=salesSummaryJob sellerId="$SELLERS" date="$d" product="$p"
  done
done
echo ">> pipeline finished"
