var confJsonFile = new File('config.json');
var config = JSON.parse(confJsonFile.read())
confJsonFile.close();

const hostnames = config.hostname;
const serviceNameBegin = config.serviceNameBegin;
const groupNum = config.groupNum;
const dataDiskPaths = config.dataDiskpath;

function appendConf(role, groupName, serviceName, dbInsatllPath) {
   for (let i = 0; i < hostnames.length; i++) {
      conf += role + ',' + groupName + ',' + hostnames[i] + ',' + serviceName + ',' + dbInsatllPath + '\n' ;
   }
   conf += '\n';
}

function appendUpdateLobConfCode(serviceName, lobmPath,lobdPath){
   for (let i = 0; i < hostnames.length; i++) {
      code += 'var oma = new Oma(' + '\'' + hostnames[i] + '\'' + ',' + 11790 + ');' + '\n';
      code += 'oma.updateNodeConfigs(' + serviceName + ',' + '{lobmpath: \"' + lobmPath + '\",' + 'lobdpath: \"' + lobdPath + '\"});' + '\n';
      code += 'oma.stopNode(' + serviceName + ');' + '\n';
      code += 'oma.startNode(' + serviceName + ');' + '\n';
      code += 'println(\"Update ' + hostnames[i] + ':' + serviceName + ' lobmpath and lobdpath done\")' + '\n';
      code += '\n';
   }
}

let conf = 'role,groupName,hostName,serviceName,dbPath\n' + '\n';
let code = '' ;

appendConf('catalog', 'SYSCatalogGroup', serviceNameBegin, '[installPath]/database/catalog' + '/' + serviceNameBegin);

appendConf('coord', 'SYSCoord', serviceNameBegin + 10, '[installPath]/database/coord' + '/' + ( serviceNameBegin + 10 ) );

for (let groupIndex = 0; groupIndex < groupNum; groupIndex++) {
   const groupName = "group" + (groupIndex + 1);
   const serviceName = serviceNameBegin + 20 + (groupIndex * 10);
   const dbInsatllPath = dataDiskPaths[groupIndex % dataDiskPaths.length][0] + '/' + serviceName;
   const lobmPath = dataDiskPaths[groupIndex % dataDiskPaths.length][0] + '/' + serviceName;
   const lobdPath = dataDiskPaths[groupIndex % dataDiskPaths.length][1] + '/' + serviceName;
   appendConf('data', groupName, serviceName, dbInsatllPath);
   appendUpdateLobConfCode(serviceName, lobmPath,lobdPath);
}

code += 'var db = new Sdb();' + '\n';
code += 'db.getRecycleBin().disable();' + '\n';
code += 'db.updateConf({diaglevel:0,auditmask:""});' + '\n';

var sdbDeployConfFile = new File("sequoiadb.conf",0644,SDB_FILE_READWRITE|SDB_FILE_CREATE|SDB_FILE_REPLACE);
sdbDeployConfFile.write(conf);
sdbDeployConfFile.close();

var updateLobConfJsFile = new File("updateConfs.js",0644,SDB_FILE_READWRITE|SDB_FILE_CREATE|SDB_FILE_REPLACE);
updateLobConfJsFile.write(code);
updateLobConfJsFile.close();