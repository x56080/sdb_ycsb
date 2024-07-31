#!/bin/bash

baseDir=$(cd $(dirname $0);cd ..;pwd)
curDir=$(cd $(dirname $0);pwd)

function help() {
  cat <<EOF
  echo "Use run.sh to run SequoiaDB YCSB tests."
  echo ""
  echo "Usage: run.sh --sdburl arg [ --sqlurl arg ]"
  echo ""
  echo "Options:"
  echo "  -h, --help         Display this help message and exit"
  echo "      --sdburl arg   Specify the connection string for the SequoiaDB cluster"
  echo "      --sqlurl arg   Specify the MySQL connection URL"
  echo ""
  echo "Examples:"
  echo "  run.sh --sdburl 'sdbadmin:sequoiadb@sdbserver1:11810,sdbserver2:11810' --sqlurl 'jdbc:mysql://localhost:3306/test?user=sdbadmin&password=sdbadmin'"
  echo ""
  echo "Note:"
  echo "  - Ensure that the specified connection URL is correct and includes necessary credentials."
  echo "  - The --sqlurl option is used to specify the MySQL connection URL when testing with MySQL as a storage backend."
EOF
}

sdburl=""
sqlurl=""
jobid=0
threads='1,16,32,64,128'
fieldcount=5
maxscanlength=10
runtime=600
recordcount=200000000

bulkInsertWookload=$curDir/workloads/workload
bulkInsertRecordcount=20000000

hasSqlurl=0

ARGS=`getopt -o h --long help,sdburl:,sqlurl: -- "$@"`

eval set -- "${ARGS}"

while true
do
  case "$1" in
    -h | --help )                           help
                                            exit 0
                                            ;;
    --sdburl)                               sdburl=$2
                                            shift 2
                                            ;;
    --sqlurl)                               sqlurl=$2
                                            hasSqlurl=1
                                            shift 2
                                            ;;
    --)                                     shift
                                            break
                                            ;;
    *)                                      echo "ERROR: Internal error!"
                                            exit 64
                                            ;;
  esac
done

if [ -z "$sdburl" ];then
  echo "Error: The 'sdburl' parameter must be specified."
  exit 1
fi

url=""
if [[ "$sdburl" == *'@'* ]]; then
  # 如果包含@，用@分割字符串，取第二部分
  sdburl=$(echo $sdburl | awk -F '@' '{print $2}')
else
  # 如果不包含@，直接使用整个字符串
  sdburl=$sdburl
fi

if [[ "$sdburl" == *','* ]]; then
  # 如果包含多个地址，取第一个
  url=$(echo $sdburl | awk -F ',' '{print $1}')
else
  # 如果只有一个地址，直接使用这个地址
  url=$sdburl
fi

sqlurlOption=''
if [ $hasSqlurl -eq 1 ]; then
  sqlurlOption='--sqlurl '$sqlurl
fi

hostname=$(echo $url | awk -F ':' '{print $1}')
port=$(echo $url | awk -F ':' '{print $2}')

## 测试 YCBS 4 个基本场景：100%READ,100%UPDATE,100%SCAN,100%INSERT
jobid=$(date +%s)
sleep 1
curl http://${hostname}:$((port+4))/ -d 'cmd=drop collectionspace&name=ycsb' 1>>/dev/null 2>&1
$baseDir/bin/prepare.sh -s $recordcount -r $runtime -t $threads -u $sdburl -p sequoiadb --fieldcount $fieldcount --maxscanlength $maxscanlength $sqlurlOption --jobid $jobid
$baseDir/bin/exectest.sh

## 测试批插场景
jobid=$(date +%s)
sleep 1
curl http://${hostname}:$((port+4))/ -d 'cmd=drop collectionspace&name=ycsb' 1>>/dev/null 2>&1
$baseDir/bin/prepare.sh -s $bulkInsertRecordcount -u $sdburl -p sequoiadb --fieldcount $fieldcount --maxscanlength $maxscanlength $sqlurlOption --jobid $jobid
$baseDir/bin/ycsb load sequoiadb -P $bulkInsertWookload -p threadcount=1 1>$curDir/log/workload_1 2>&1

jobid=$(date +%s)
sleep 1
curl http://${hostname}:$((port+4))/ -d 'cmd=drop collectionspace&name=ycsb' 1>>/dev/null 2>&1
$baseDir/bin/prepare.sh -s $bulkInsertRecordcount -u $sdburl -p sequoiadb --fieldcount $fieldcount --maxscanlength $maxscanlength $sqlurlOption --jobid $jobid
$baseDir/bin/ycsb load sequoiadb -P $bulkInsertWookload -p threadcount=16 1>$curDir/log/workload_16 2>&1

jobid=$(date +%s)
sleep 1
curl http://${hostname}:$((port+4))/ -d 'cmd=drop collectionspace&name=ycsb' 1>>/dev/null 2>&1
$baseDir/bin/prepare.sh -s $bulkInsertRecordcount -u $sdburl -p sequoiadb --fieldcount $fieldcount --maxscanlength $maxscanlength $sqlurlOption --jobid $jobid
$baseDir/bin/ycsb load sequoiadb -P $bulkInsertWookload -p threadcount=32 1>$curDir/log/workload_32 2>&1

jobid=$(date +%s)
sleep 1
curl http://${hostname}:$((port+4))/ -d 'cmd=drop collectionspace&name=ycsb' 1>>/dev/null 2>&1
$baseDir/bin/prepare.sh -s $bulkInsertRecordcount -u $sdburl -p sequoiadb --fieldcount $fieldcount --maxscanlength $maxscanlength $sqlurlOption --jobid $jobid
$baseDir/bin/ycsb load sequoiadb -P $bulkInsertWookload -p threadcount=64 1>$curDir/log/workload_64 2>&1

jobid=$(date +%s)
sleep 1
curl http://${hostname}:$((port+4))/ -d 'cmd=drop collectionspace&name=ycsb' 1>>/dev/null 2>&1
$baseDir/bin/prepare.sh -s $bulkInsertRecordcount -u $sdburl -p sequoiadb --fieldcount $fieldcount --maxscanlength $maxscanlength $sqlurlOption --jobid $jobid
$baseDir/bin/ycsb load sequoiadb -P $bulkInsertWookload -p threadcount=128 1>$curDir/log/workload_128 2>&1