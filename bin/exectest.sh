#!/bin/bash

baseDir=$(cd $(dirname $0);cd ..;pwd)
tmpConfFile="$baseDir/tmp/ycsb.tmp.conf"

function help() {
  cat << EOF
exectest is a utility for executing SequoiaDDS/SequoiaDB YCSB tests remotely.

Usage: exectest [OPTIONS]

Options:
  -h, --help             Display this help message and exit
  -c, --credentials      Specify the credentials in the format 'username:password@hostname'
  -d, --remotedir        Specify the remote directory for storing the tool

Examples:
  exectest -c sdbadmin:sdbadmin@remote.example.com -d /path/to/remote/directory

Note:
  - Ensure that you have set up SSH key-based authentication for secure remote execution.
  - The specified user must have the necessary permissions to execute the tests.
EOF
}

credentials=""
remotedir=""

# 使用getopt解析命令行参数
args=$(getopt -o hc:d: -l help,credentials:,remotedir: -- "$@")
eval set -- "$args"

# 处理解析后的参数
while [ $# -gt 0 ]; do
  case "$1" in
    -c|--credentials)
      credentials="$2"
      shift 2
      ;;
    -d|--remotedir)
      remotedir="$2"
      shift 2
      ;;
    -h|--help)
      help
      exit 0
      ;;
    --)
      shift
      break
      ;;
    *)
      echo "Usage: $0 [-c|--credentials <credentials>] [-d|--remotedir <directory>]"
      help
      exit 1
      ;;
  esac
done

# 移除处理过的参数
shift $((OPTIND-1))

# 解析credentials参数
IFS=':' read -ra items <<< "$credentials"
if [ "${#items[@]}" -eq 2 ]; then
   remoteuser="${items[0]}"
   IFS='@' read -ra subitems <<< "${items[1]}"
   if [ "${#subitems[@]}" -ge 2 ];then
      remotehost="${subitems[-1]}"
      remotepwd=$(echo ${items[1]} | sed "s/@${remotehost}//g")
   fi
else
   remotehost="${credentials}"
fi

if [ -z "$remotehost" ]; then
  $baseDir/bin/execycsbtest.sh -c $tmpConfFile
else
  echo >script/inventory.ini
  echo "[compress_server]" >>script/inventory.ini
  echo "${remotehost}" >>script/inventory.ini
  transferdir=$(pwd)
  ansible-playbook  script/remoteexec.yml -i script/inventory.ini -e "local_ycsb_path=${transferdir} remote_ycsb_path=$remotedir remote_user=$remoteuser remote_group=$remoteuser"_group --extra-vars "ansible_ssh_pass=${remotepwd}" --extra-vars "ansible_ssh_user=${remoteuser}"
fi

