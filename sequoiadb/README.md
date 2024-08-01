SequoiaDB YCSB 性能测试工具

## 测试步骤

1. config.json 文件配置

   ```lang-json
   {
      "hostname": ["192.168.30.81","192.168.30.82","192.168.30.83"],
      "serviceNameBegin": 11800,
      "groupNum": 6,
      "dataDiskpath":
      [
         ["/ssd1/sequoiadb/database/data", "/hdd1/sequoiadb/database/data"],
         ["/ssd2/sequoiadb/database/data", "/hdd2/sequoiadb/database/data"],
         ["/ssd3/sequoiadb/database/data", "/hdd3/sequoiadb/database/data"],
         ["/ssd4/sequoiadb/database/data", "/hdd4/sequoiadb/database/data"],
         ["/ssd5/sequoiadb/database/data", "/hdd5/sequoiadb/database/data"],
         ["/ssd6/sequoiadb/database/data", "/hdd6/sequoiadb/database/data"]
      ]
   }
   ```

   > **Note：**
   > SequoiaDB 安装用户必须拥有以上目录的读写权限。

   参数介绍

   | 参数名           | 描述                   |
   | ---------------- | ---------------------- |
   | hostname         | SequoiaDB 集群部署机器 |
   | serviceNameBegin | 节点起始端口号。例如：起始端口号为 11800，那么编目节点端口号是 11800，协调节点端口号是 11810，数据组1的节点端口号是 11820，数据组2的节点端口号是 11830，以此类推 |
   | groupNum         | 部署的数据组的个数     |
   | dataDiskpath     | 数据节点的部署路径。例如：以上面为例，数据组 1 节点的部署路径为 "/ssd1/sequoiadb/database/data/11820"，lobd 文件路径为 "/hdd1/sequoiadb/database/data/11820"，以此类推 |

2. SequoiaDB 集群部署和配置修改

   下面命令都需要切换到 SequoiaDB 安装用户执行

   2.1 以上面配置文件为例，在 3 台机器上安装好 SequoiaDB，假设安装路径为`/opt/sequoiadb`；

   2.2 将 config.json，sdbPrepare.js 和 sdbPrepare.sh 拷贝到其中一台部署机器上，目标目录为`/opt/sequoiadb/tools/deploy/`，然后在目标机器上执行以下命令，生成 quickDeploy.sh 配置文件，文件名为`sequoiadb.conf`，和修改 SequoiaDB 配置脚本，文件名为`updateConfs.js`

   ```lang-bash
   cd /opt/sequoiadb/tools/deploy/
   ./sdbPrepare.sh --action generate
   ```

   2.3 部署集群
   ```lang-bash
   ./quickDeploy.sh --sdb
   ```

   2.4 更新 SequoiaDB 配置
   ```lang-bash
   ./sdbPrepare.sh --action update
   ```

3. MySQL 集群部署，创建用户名，创建写入测试数据的数据库。如果不想把测试数据写入 MySQL，可以省略这一步骤。SequoiaDB YCSB 测试会在当前目录生成日志文件，日志文件会保存测试数据。创建用户名，授权，创建数据库步骤如下：

   ```lang-bash
   mysql> grant all privileges on *.* to sdbadmin@'%' identified by 'sdbadmin' with grant option;
   mysql> flush privileges;
   mysql> create database test;
   ```

4. 执行测试

   ```lang-bash
   ./run.sh --sdburl '192.168.30.81:11810,192.168.30.82:11810,192.168.30.83:11810' --sqlurl 'jdbc:mysql://localhost:3306/test?user=sdbadmin&password=sdbadmin'
   ```

5. 测试结果整理

   如果把测试结果写入了 MySQL，需要把测试结果导出为 csv 文件，方便整理。以下是导出文件的步骤

   5.1 配置导出文件存放目录。在实例配置文件 auto.cnf 中增加以下配置
   ```lang-bash
   secure_file_priv=<path>
   ```

   5.2 重启 MySQL 实例
   ```lang-bash
   sdb_sql_ctl stopall
   sdb_sql_ctl startall
   ```

   5.3 执行以下 SQL 语句，导出文件，语句中 `<path>`需要修改为实际路径

   导出点查场景数据

   ```lang-bash
   mysql> select a.* into outfile '<path>/read.csv' fields terminated by ',' optionally enclosed by '"' lines terminated by '\n' from (select 'test_id','test_round_id','scenario_name','operation_type','thread_count','throughput','average_latency','min_latency','max_latency','latency_95th_percentile','latency_99th_percentile','test_timestamp' union select b.* from (select * from test.test_results where thread_count = 1 union select * from test.test_results where thread_count = 16 union select * from test.test_results where thread_count = 32 union select * from test.test_results where thread_count = 64 union select * from test.test_results where thread_count = 128) as b where b.scenario_name = "100READ" and b.operation_type != "CLEANUP" ) as a;
   ```

   导出更新场景数据

   ```lang-bash
   mysql> select a.* into outfile '<path>/update.csv' fields terminated by ',' optionally enclosed by '"' lines terminated by '\n' from (select 'test_id','test_round_id','scenario_name','operation_type','thread_count','throughput','average_latency','min_latency','max_latency','latency_95th_percentile','latency_99th_percentile','test_timestamp' union select b.* from (select * from test.test_results where thread_count = 1 union select * from test.test_results where thread_count = 16 union select * from test.test_results where thread_count = 32 union select * from test.test_results where thread_count = 64 union select * from test.test_results where thread_count = 128) as b where b.scenario_name = "100UPDATE" and b.operation_type != "CLEANUP" ) as a;
   ```

   导出范围查场景数据

   ```lang-bash
   mysql> select a.* into outfile '<path>/scan.csv' fields terminated by ',' optionally enclosed by '"' lines terminated by '\n' from (select 'test_id','test_round_id','scenario_name','operation_type','thread_count','throughput','average_latency','min_latency','max_latency','latency_95th_percentile','latency_99th_percentile','test_timestamp' union select b.* from (select * from test.test_results where thread_count = 1 union select * from test.test_results where thread_count = 16 union select * from test.test_results where thread_count = 32 union select * from test.test_results where thread_count = 64 union select * from test.test_results where thread_count = 128) as b where b.scenario_name = "100SCAN" and b.operation_type != "CLEANUP" ) as a;
   ```

   导出单插场景数据

   ```lang-bash
   mysql> select a.* into outfile '<path>/insert.csv' fields terminated by ',' optionally enclosed by '"' lines terminated by '\n' from (select 'test_id','test_round_id','scenario_name','operation_type','thread_count','throughput','average_latency','min_latency','max_latency','latency_95th_percentile','latency_99th_percentile','test_timestamp' union select b.* from (select * from test.test_results where thread_count = 1 union select * from test.test_results where thread_count = 16 union select * from test.test_results where thread_count = 32 union select * from test.test_results where thread_count = 64 union select * from test.test_results where thread_count = 128) as b where b.scenario_name = "100INSERT" and b.operation_type != "CLEANUP" ) as a;
   ```

   导出批插场景数据

   ```lang-bash
   mysql> select a.* into outfile '<path>/batchinsert.csv' fields terminated by ',' optionally enclosed by '"' lines terminated by '\n' from (select 'test_id','test_round_id','scenario_name','operation_type','thread_count','throughput','average_latency','min_latency','max_latency','latency_95th_percentile','latency_99th_percentile','test_timestamp' union select b.* from (select * from test.test_results where thread_count = 1 union select * from test.test_results where thread_count = 16 union select * from test.test_results where thread_count = 32 union select * from test.test_results where thread_count = 64 union select * from test.test_results where thread_count = 128) as b where b.scenario_name = "100BATCHINSERT" and b.operation_type != "CLEANUP" ) as a;
   ```

   清理测试数据
   ```lang-bash
   mysql> use test;
   mysql> drop table test_results;
   mysql> drop table test_config;
   ```
