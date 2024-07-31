#!/bin/bash

function build_help()
{
   echo ""
   echo "Usage:"
   echo "  --action arg         generate or update"
   echo "                       - generate: It will generate a SequoiaDB deploy config file and"
   echo "                         a update SequoiaDB config script file"
   echo "                       - update: It will execute the update lob config script file"
   echo "Example: "
   echo "  ./ycsbPrepare.sh --action generate"
   echo "  ./ycsbPrepare.sh --action update"
   echo ""
}

action="generate"
myPath=`dirname $0`

if [[ ${myPath:0:1} != "/" ]]; then
   myPath=$(pwd)/$myPath  #relative path
else
   myPath=$myPath         #absolute path
fi

#Parse command line parameters
ARGS=`getopt -o h --long help,action: -- "$@"`
ret=$?
test $ret -ne 0 && exit $ret

eval set -- "${ARGS}"

while true
do
   case "$1" in
      --action )       action=$2
                       shift 2
                       ;;
      -h | --help )    build_help
                       exit 0
                       ;;
      --)              shift
                       break
                       ;;
      *)               echo "Internal error!"
                       exit 64
                       ;;
   esac
done

if [ "$action" = "generate" ]; then
  jsFile="$myPath/ycsbPrepare.js"
elif [ "$action" = "update" ]; then
  jsFile="$myPath/updateLobConf.js"
else
  echo "Error: 'action' parameter must be 'generate' or 'update'"
  exit 1
fi

#get sdb shell path
sdbShellPath=$myPath/../../bin/sdb

#execute command
command=$sdbShellPath" -f "$jsFile
echo "Execute command: "$command
eval $command
