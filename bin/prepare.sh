#!/bin/bash

baseDir=$(cd $(dirname $0);cd ..;pwd)
tmpConfFile="$baseDir/tmp/ycsb.tmp.conf"
loadFile="$baseDir/.load"
workloadSamplesDirPrfix=""

>$tmpConfFile

function help() {
  cat <<EOF
  echo "prepare is a utility to prepare for SequoiaDDS/SequoiaDB YCSB tests."
  echo ""
  echo "Usage: prepare [Options]"
  echo ""
  echo "Options:"
  echo "  -h, --help             Display this help message and exit"
  echo "  -r, --runtime          Set the duration of a single test scenario (default: 300s)"
  echo "  -s, --recordcount      Set the amount of basic test data (default: 1000000)"
  echo "  -u, --url              Specify the connection string for the SequoiaDDS|SequoiaDB cluster"
  echo "  -t, --threads          Instruct a set of threads (default: '1,8,16,32,64,128,256')"
  echo "  -c, --testcase         Instruct a set of test cases (default: '100%READ,100%UPDATE,100%SCAN,100%INSERT')"
  echo "  -p, --product          Specify the testing product (sequoiadb/sequoiadds)"
  echo "  -j, --jobid            Specify the job ID for CI build association (default: 0)"
  echo "      --hostlist         Specify the list of deployment hosts"
  echo "      --sqlurl           Specify the MySQL connection URL"
  echo "      --fieldcount       Specify the number of fields in a record (default: 10)"
  echo "      --maxscanlength    Specify the maximum number of records to be scanned in the scan operation (default: 1000)"
  echo ""
  echo "Examples:"
  echo "  prepare -u 'sdbadmin:sequoiadb@server1:11810,server2:11810' -t 5,10 -c 100%READ,100%UPDATE -p sequoiadb"
  echo "  prepare -u 'mongodb://server1:27017,server2:27017/ycsb?replicaSet=rs0' --sqlurl 'jdbc:mysql://localhost:3306/test?user=sdbadmin&password=sdbadmin' -p sequoiadds"
  echo ""
  echo "Note:"
  echo "  - Ensure that the specified connection URL is correct and includes necessary credentials."
  echo "  - The --hostlist option is used to specify the list of deployment hosts for product "
  echo "  - The --sqlurl option is used to specify the MySQL connection URL when testing with MySQL as a storage backend."
EOF
}

function parse_sqlurl()
{
   jdbc_url=$1
   if [ -n "$jdbc_url" ];then
      # 提取 host、port、database、user 和 password
      url=$(echo "$jdbc_url" | awk -F '?' '{print $1}')
      parameters=$(echo "$jdbc_url" | awk -F '?' '{print $2}')

      OLDIFS=$IFS
      # 设置IFS为"&"，以便按照"&"进行拆分
      IFS="&"

      # 将字符串分割为数组
      read -ra parts <<< "$parameters"
      # 遍历数组并输出
      for part in "${parts[@]}"; do
         echo $part |grep -q "user"
         if [ $? -eq 0 ];then
            user=$(echo $part|awk -F '=' '{print $2}')
         fi

         echo $part |grep -q "password"
         if [ $? -eq 0 ];then
            password=$(echo $part|awk -F '=' '{print $2}')
         fi
      done

      # 构建新的字符串
      new_url="db.url=${url}?useSSL=false"
      new_user="db.user=$user"
      new_password="db.password=$password"

      echo $new_url >>$tmpConfFile
      echo $new_user >>$tmpConfFile
      echo $new_password >>$tmpConfFile
   fi
}

function generate_mongourl() {
  local hostlist="$1"
  IFS=',' read -ra hosts <<< "$hostlist"
  local mongourl="mongodb://"
  for host in "${hosts[@]}"; do
    mongourl+="$host:27017,"
  done
  mongourl=${mongourl%,}  # Remove the trailing comma
  mongourl+="/ycsb?replicaSet=rs0"
  echo "$mongourl"
}

function generate_sequoiadburl() {
  local hostlist="$1"
  IFS=',' read -ra hosts <<< "$hostlist"
  local sdburl=""
  for host in "${hosts[@]}"; do
    sdburl+="$host:11810,"
  done
  sdburl=${sdburl%,}  # Remove the trailing comma
  echo "$sdburl"
}

maxexecutiontime=300
recordcount=1000000
fieldcount=10
maxscanlength=1000
connurl=""
hostlist="192.168.24.112,192.168.24.113,192.168.24.114"
threads=1,8,16,32,64,128,256
testcase=100%READ,100%UPDATE,100%SCAN,100%INSERT
product=""
jobid=0
sqlurl=""


ARGS=`getopt -o hr:s:u:t:c:p:j: --long help,runtime:,recordcount:,url:,threads:,testcase:,hostlist:,product:,jobid:,sqlurl:,fieldcount:,maxscanlength: -- "$@"`

eval set -- "${ARGS}"

while true
do
  case "$1" in
    -h | --help )                           help
                                            exit 0
                                            ;;
    -r | --runtime)                         maxexecutiontime=$2
                                            shift 2
                                            ;;
    -s | --recordcount)                     recordcount=$2
                                            shift 2
                                            ;;
    -u | --url)                             connurl=$2
                                            shift 2
                                            ;;
    -t | --threads)                         threads=$2
                                            shift 2
                                            ;;
    -c | --testcase)                        testcase=$2
                                            shift 2
                                            ;;
    -p | --product)                         product=$2
                                            shift 2
                                            ;;
    -j | --jobid)
                                            jobid="$2"
                                            shift 2
                                            ;;
    --hostlist)                             hostlist=$2
                                            shift 2
                                            ;;
    --sqlurl)                               sqlurl=$2
                                            shift 2
                                            ;;
    --fieldcount)                           fieldcount=$2
                                            shift 2
                                            ;;
    --maxscanlength)                        maxscanlength=$2
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


if [ "$product" = "sequoiadb" ]; then
  workloadSamplesDirPrfix="$baseDir/sequoiadb"
elif [ "$product" = "sequoiadds" ]; then
  workloadSamplesDirPrfix="$baseDir/sequoiadds"
else
  echo "Error: 'product' parameter must be 'sequoiadb' or 'sequoiadds'"
  exit 1
fi

# 检查 workloadsSample 目录是否存在
if [ ! -d "$workloadSamplesDirPrfix/workloadsSample" ]; then
  echo "Error: '$workloadSamplesDirPrfix/workloadsSample' directory does not exist."
  exit 1
fi

# 检查.load文件是否存在,存在则删除
if [ -f "$loadFile" ];then
  rm -rf $loadFile
fi

# 获取workloads目录的绝对路径
workloadsDir=$(realpath "$workloadSamplesDirPrfix/workloads")

# 如果workloads目录存在，删除它
if [ -d "$workloadsDir" ]; then
  rm -rf "$workloadsDir"
fi

cp -r "$workloadSamplesDirPrfix/workloadsSample" "$workloadsDir"

if [ -z "$connurl" ];then
  if [ $product = "sequoiadds" ];then
     connurl=$(generate_mongourl $hostlist)
  else
     connurl=$(generate_sequoiadburl $hostlist)
  fi
fi

if [ $product = "sequoiadds" ];then
  echo "mongodb.url=$connurl" >>$tmpConfFile
else
  first_part=""
  if [[ "$connurl" == *'@'* ]]; then
    # 如果包含@，用@分割字符串，取第一部分
    first_part=$(echo $connurl | awk -F '@' '{print $1}')
    connurl=$(echo $connurl | awk -F '@' '{print $2}')
  else
    # 如果不包含@，直接使用整个字符串
    connurl=$connurl
  fi

  if [ "$first_part" != "" ];then
    IFS=':' read -ra parts <<< "$first_part"
    echo "sequoiadb.user=${parts[0]}" >>$tmpConfFile
    echo "sequoiadb.passwd=${parts[1]}" >>$tmpConfFile
  fi
  echo "sequoiadb.url=$connurl" >>$tmpConfFile
fi

echo "maxexecutiontime=$maxexecutiontime" >>$tmpConfFile
echo "recordcount=$recordcount" >>$tmpConfFile
echo "fieldcount=$fieldcount" >>$tmpConfFile
echo "maxscanlength=$maxscanlength" >>$tmpConfFile

if [ $jobid -ne 0 ];then
  echo "jobid=${jobid}" >>$tmpConfFile
fi

parse_sqlurl $sqlurl

# 配置项数组
declare -A config_items
# 读取ycsb.tmp.conf中的配置项到关联数组中
while IFS="=" read -r key value; do
  config_items["$key"]="$value"
done < "$tmpConfFile"

# 修改workload目录下的同名配置项
for config_file in "$workloadsDir"/*; do
   filename=$(basename "$config_file")
   for key in "${!config_items[@]}"; do
      if [ "$key" = "maxexecutiontime" ] && grep -q "^insertproportion=1" "$workloadsDir/$filename"; then
         continue
      fi

      if [ -f "$workloadsDir/$filename" ] && grep -q "^$key=" "$workloadsDir/$filename"; then
         sed -i "s/^$key=.*/$key=${config_items[$key]//\//\\/}/" "$workloadsDir/$filename"
      else
         echo "$key=${config_items[$key]}" >>"$workloadsDir/$filename"
      fi
   done
done

# 检查是否存在 recordcount 配置，并且文件名为 workload
if [ -n "${config_items["recordcount"]}" ] && [ -f "$workloadsDir/workload" ]; then
  sed -i "s/^operationcount=.*/operationcount=${config_items["recordcount"]}/" "$workloadsDir/workload"
fi

declare -A mapping
mapping["100%READ"]="workload_read"
mapping["100%UPDATE"]="workload_update"
mapping["100%SCAN"]="workload_scan"
mapping["100%INSERT"]="workload_insert"

IFS=',' read -ra arr_threads <<< "$threads"
echo "threads=(${arr_threads[@]})" >>$tmpConfFile

IFS=',' read -ra arr_testcase <<< "$testcase"
workloads=()
for item in ${arr_testcase[@]};
do
   workloads+=("${mapping[$item]}")
done
echo "workloads=(${workloads[@]})" >>$tmpConfFile
echo "loaddata=1" >>$tmpConfFile
if [ "$product" == "sequoiadds" ]; then
    product="mongodb"
fi
echo "product=${product}" >>$tmpConfFile
echo "Configuration updated successfully."
exit 0
