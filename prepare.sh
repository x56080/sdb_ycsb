#!/bin/bash

function help() {
  cat <<EOF
  echo "prepare is a utility to prepare for SequoiaDDS/SequoiaDB YCSB tests."
  echo ""
  echo "Usage: prepare [OPTIONS]"
  echo ""
  echo "Options:"
  echo "  -h, --help             Display this help message and exit"
  echo "  -r, --runtime          Set the duration of a single test scenario"
  echo "  -s, --recordcount      Set the amount of basic test data"
  echo "  -u, --url              Specify the connection string for the SequoiaDDS|SequoiaDB cluster"
  echo "  -t, --threads          Instruct a set of threads"
  echo "  -c, --testcase         Instruct a set of test cases"
  echo "  -p, --product          Specify the testing product (sequoiadb/sequoiadds)"
  echo "  -j, --jobid            Specify the job ID for CI build association"
  echo "      --hostlist         Specify the list of deployment hosts"
  echo "      --connstr          Specify the MySQL connection URL"
  echo ""
  echo "Examples:"
  echo "  prepare -r 10 -s 1000000 -u 'sdbadmin:sequoiadb@localhost:11810'"
  echo "  prepare -r 5 -s 500000 -u 'mongodb://192.168.24.116:27017/ycsb?replicaSet=rs0' -t 10 -c 'jdbc:mysql://localhost:3306/test?user=sdbadmin&password=sdbadmin' -p sequoiadds"
  echo "  prepare --runtime 20 --recordcount 2000000 --url 'sdbadmin:sequoiadb@localhost:11810' --threads 5,10 --testcase 100%READ,100%UPDATE -p sequoiadb --jobid 12345"
  echo ""
  echo "Note:"
  echo "  - Ensure that the specified connection URL is correct and includes necessary credentials."
  echo "  - The --hostlist option is used to specify the list of deployment hosts for product "
  echo "  - The --connstr option is used to specify the MySQL connection URL when testing with MySQL as a storage backend."
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

      echo $new_url >>ycsb.conf
      echo $new_user >>ycsb.conf
      echo $new_password >>ycsb.conf
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
#mongourl="mongodb://192.168.24.112:27017,192.168.24.113:27017,192.168.24.114:27017/ycsb?replicaSet=rs0"
mongourl=""
hostlist="192.168.24.112,192.168.24.113,192.168.24.114"
threads=1,8,16,32,64,128,256
testcase=100%READ,100%UPDATE,100%SCAN,100%INSERT
product=sequoiadds
declare -A mapping
mapping["100%READ"]="sequoiadds01"
mapping["100%UPDATE"]="sequoiadds02"
mapping["100%SCAN"]="sequoiadds03"
mapping["100%INSERT"]="sequoiadds04"
jobid=0
#sqlurl="jdbc:mysql://localhost:3306/test?user=sdbadmin&password=sdbadmin"
sqlurl=""


ARGS=`getopt -o hr:s:u:t:c:p:j: --long help,runtime:,recordcount:,url:,threads:testcase:,hostlist:,product:,jobid:,connstr: -- "$@"`

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
    -u | --url)                             mongourl=$2
                                            shift 2
                                            ;;
    -t | --thread)                          threads=$2
                                            shift 2
                                            ;;
    -c | --testcase)                        testcase=$2
                                            shift 2
                                            ;;
    -p | --product)                         product=$2 
                                            shift 2
                                            ;;
    -j|--jobid)
                                           jobid="$2"
                                           shift 2
                                            ;;
    --hostlist)                             hostlist=$2
                                            shift 2
                                            ;;
    -c | --connstr)                         sqlurl=$2
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

if [ -z "$mongourl" ];then
  if [ $product = "sequoiadds" ];then
     mongourl=$(generate_mongourl $hostlist)
     //rm -rf sequoiadb-binding/
  else
     mongourl=$(generate_sequoiadburl $hostlist)
     //rm -rf mongodb-binding/
  fi
fi

>ycsb.conf
echo "maxexecutiontime=$maxexecutiontime" >>ycsb.conf
echo "recordcount=$recordcount" >>ycsb.conf
if [ $product = "sequoiadds" ];then
  echo "mongodb.url=$mongourl" >>ycsb.conf
else
  first_part=""
  if [[ "$mongourl" == *'@'* ]]; then
    # 如果包含@，用@分割字符串，取第一部分
    first_part=$(echo $mongourl | awk -F '@' '{print $1}')
    mongourl=$(echo $mongourl | awk -F '@' '{print $2}')
  else
    # 如果不包含@，直接使用整个字符串
    mongourl=$mongourl
  fi

  if [ "$first_part" != "" ];then
    IFS=':' read -ra parts <<< "$first_part"
    echo "sequoiadb.user=${parts[0]}" >>ycsb.conf
    echo "sequoiadb.passwd=${parts[1]}" >>ycsb.conf
  fi
  echo "sequoiadb.url=$mongourl" >>ycsb.conf
fi

if [ $jobid -ne 0 ];then
  echo "jobid=${jobid}" >>ycsb.conf
fi
parse_sqlurl $sqlurl

# 检查.load文件是否存在,存在则删除
if [ -f ".load" ];then
  rm -rf .load
fi

# 检查workloads.bk目录是否存在
if [ ! -d "workloads.bk" ]; then
  echo "Error: 'workloads.bk' directory does not exist."
  exit 1
fi

# 获取workloads目录的绝对路径
workloads_dir=$(realpath "workloads")

# 如果workloads目录存在，删除它
if [ -d "$workloads_dir" ]; then
  rm -rf "$workloads_dir"
fi

# 拷贝workloads.bk为workloads目录
cp -r "workloads.bk" "$workloads_dir"

# 检查ycsb.conf文件是否存在
if [ ! -f "ycsb.conf" ]; then
  echo "Error: 'ycsb.conf' file does not exist."
  exit 1
fi

# 配置项数组
declare -A config_items

grep -vE '^\s*(//|#|$)' ycsb.conf > ycsb_new.conf
# 读取ycsb.conf中的配置项到关联数组中
while IFS="=" read -r key value; do
  config_items["$key"]="$value"
done < "ycsb_new.conf"

rm -rf ycsb_new.conf
# 修改workload目录下的同名配置项
for config_file in "$workloads_dir"/*; do
   filename=$(basename "$config_file")
   for key in "${!config_items[@]}"; do
      if [ "$key" = "maxexecutiontime" ] && grep -q "^insertproportion=1" "$workloads_dir/$filename"; then
         continue
      fi

      if [ -f "$workloads_dir/$filename" ] && grep -q "^$key=" "$workloads_dir/$filename"; then
         sed -i "s/^$key=.*/$key=${config_items[$key]//\//\\/}/" "$workloads_dir/$filename"
      else
         echo "$key=${config_items[$key]}" >>"$workloads_dir/$filename"
      fi
   done
done

# 检查是否存在recordcount配置，并且文件名为sequoiaddsload
if [ -n "${config_items["recordcount"]}" ] && [ -f "$workloads_dir/sequoiaddsload" ]; then
  sed -i "s/^operationcount=.*/operationcount=${config_items["recordcount"]}/" "$workloads_dir/sequoiaddsload"
fi

>ycsb_dds.conf
IFS=',' read -ra arr_threads <<< "$threads"
echo "threads=(${arr_threads[@]})" >>ycsb_dds.conf

IFS=',' read -ra arr_testcase <<< "$testcase"
workloads=()
for item in ${arr_testcase[@]};
do
   workloads+=("${mapping[$item]}")
done
echo "workloads=(${workloads[@]})" >>ycsb_dds.conf
echo "loaddata=1" >>ycsb_dds.conf
if [ "$product" == "sequoiadds" ]; then
    product="mongodb"
fi
echo "product=${product}" >>ycsb_dds.conf
echo "Configuration updated successfully."
exit 0
