#!/bin/bash
set -x
#set -e

# 增加失败次数判断，当失败次数大于10时终止测试
# 需要增加如果是load时，清理之前的数据
baseDir=$(cd $(dirname $0);cd ..;pwd)
curDir=$(cd $(dirname $0);pwd)

arch=$(uname -p)

if [ "$arch" = "x86_64" ];then
  export java_home="${baseDir}/java/x86_64/openjdk-8u292-b10/"
else
  export java_home="${baseDir}/java/aarch64/openjdk-8u292-b10/"
fi

export JAVA_HOME="$java_home"
export PATH="$JAVA_HOME/bin:$PATH"
threads=(1 8 16 32 64 128 256)
workloads=(workload_read workload_update workload_scan workload_insert)

function check_failure()
{
   logfile=$1
   if grep -q OVERALL "$logfile" && [ "$(grep -c Exception "$logfile")" -le 10 ]; then
     return 0  # 成功
   else
     return 1  # 失败
   fi
}


# 检查文件是否存在函数
function check_file_exist() {
  local file=$1
  if [ ! -f "$file" ]; then
    echo "$file does not exist!"
    exit 1
  fi
}
# 初始化变量
configfile=""

# 使用getopt解析命令行参数
args=$(getopt -o c: -l configfile: -- "$@")
eval set -- "$args"

# 处理解析后的参数
while [ $# -gt 0 ]; do
  case "$1" in
    -c|--configfile)
      configfile="$2"
      shift 2
      ;;
    --)
      shift
      break
      ;;
    *)
      echo "Usage: $0 [-c|--configfile <file>]"
      exit 1
      ;;
  esac
done

# 输出参数
echo "Config File: $configfile"
if [ ! -f "$configfile" ];then
  echo "Config File $configfile does not exist!"
  exit 1
fi

. $configfile

workloadSamplesDirPrfix=""
if [ "$product" = "sequoiadb" ]; then
  workloadSamplesDirPrfix="$baseDir/sequoiadb"
elif [ "$product" = "sequoiadds" ]; then
  workloadSamplesDirPrfix="$baseDir/sequoiadds"
else
  echo "Error: 'product' parameter must be 'sequoiadb' or 'sequoiadds'"
  exit 1
fi

if [ -d $workloadSamplesDirPrfix/log ];then
   bakname=$(date +%Y%m%d%H%M)
   mv $workloadSamplesDirPrfix/log "$workloadSamplesDirPrfix/log_$bakname"
fi
mkdir -p $workloadSamplesDirPrfix/log

workloadfile="$workloadSamplesDirPrfix/workloads/workload"
check_file_exist "$workloadfile"

recordcount=$(grep recordcount $workloadfile |awk -F '=' '{print $2}')
if [ ! -f $baseDir/.load ] || [ $loaddata -eq 1 ];then
   logfile="$workloadSamplesDirPrfix/log/workload_100"
   bin/ycsb load ${product} -P $workloadfile  -s -p threadcount=100 1>>${logfile} 2>&1
   ret=$?
   check_failure $logfile
   if [[ $? -ne 0 || $ret -ne 0  ]];then
       cat $logfile
       exit 1
   else
      touch $baseDir/.load
   fi
fi

length=${#workloads[@]}
for ((i=0; i<$length; i++));
do
   workload="${workloads[i]}"
   workloadfile="$workloadSamplesDirPrfix/workloads/${workload}"
   check_file_exist $workloadfile
   modpara=0
   if [ "$workload" = "workload_insert" ];then
      origrecordcount=$(grep recordcount ${workloadfile} |awk -F '=' '{print $2}')
      originsertstart=$(grep insertstart ${workloadfile} |awk -F '=' '{print $2}')
      operationcount=$(grep operationcount ${workloadfile} |awk -F '=' '{print $2}')
      modpara=1
   fi

   generateReport=false
   threadsSize=${#threads[@]}
   for ((n=0; n<$threadsSize; n++));
   do
      thread="${threads[n]}"
      if [ $i -eq $((length-1)) ] && [ $n -eq $((threadsSize-1))  ]; then
        generateReport=true
      fi
      if [ $modpara -eq 1 ];then
         sed -i "s/recordcount=${origrecordcount}/recordcount=${recordcount}/g" ${workloadfile}
         sed -i "s/insertstart=${originsertstart}/insertstart=${recordcount}/g" ${workloadfile}
      fi
      origintimelen=$(grep maxexecutiontime $workloadfile | awk -F '=' '{print $2}')
      timelen=$origintimelen
      while((1))
      do
         logfile="$workloadSamplesDirPrfix/log/${workload}_${thread}"
         start=$(date +"%s")
         bin/ycsb run ${product} -P $workloadfile  -s -p threadcount=$thread -p generatereport=$generateReport 1>>${logfile} 2>&1
         ret=$?
         if [ $ret -eq 0 ];then
            check_failure $logfile
            ret2=$?
         fi
         if [ $ret -ne 0 ];then
             end=$(date +"%s")
             diff=$((end-start))
             lasttimelen=$timelen
             timelen=$((timelen-diff))
             if [ $timelen -le 0 ];then
                echo "sed -i s/maxexecutiontime=${lasttimelen}/maxexecutiontime=${origintimelen} $workloadfile"
                break
             else
                echo "sed -i s/maxexecutiontime=${lasttimelen}/maxexecutiontime=${timelen} $workloadfile"
             fi
         else
            echo "sed -i s/maxexecutiontime=${timelen}/maxexecutiontime=${origintimelen} $workloadfile"
            break
         fi
      done
      if [ $modpara -eq 1 ];then
         sed -i "s/recordcount=${recordcount}/recordcount=${origrecordcount}/g" ${workloadfile}
         sed -i "s/insertstart=${recordcount}/insertstart=${originsertstart}/g" ${workloadfile}
         recordcount=$(($recordcount+$operationcount))
      fi
      sleep 100s
   done
done

exit 0
