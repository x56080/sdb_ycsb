该项目是基于 YCSB 基础上开发的，方便 SequoiaDB 和 SequoiaDDS 结构化场景性能测试。

##SequoiaDB性能测试##

测试步骤详细见`sequoiadb/README.md`

##SequoiaDDS性能测试##

执行步骤如下
```lang-bash
./bin/prepare.sh -r 600 -s 200000000 -u 'mongodb://server1:27017,server2:27017/ycsb?replicaSet=rs0' --sqlurl 'jdbc:mysql://localhost:3306/test?user=sdbadmin&password=sdbadmin' -p sequoiadds

./bin/exectest.sh
```

> **Note：**
> SequoiaDB 和 SequoiaDDS YCSB 支持把测试结果写入 MySQL，测试命令需要指定 sqlurl。在测试前，需要先部署 MySQL，创建用户名和密码，手动创建写入的数据库。


##项目文件介绍##

```lang-bash
├── bin
│   ├── bindings.properties       // YCSB 依赖库配置
│   ├── exectest.sh               // YCSB 测试脚本，对 execycsbtest.sh 和 execycsbtest.sac.sh 脚本的封装，
                                  // 增加了远程执行的能力
│   ├── execycsbtest.sh           // YCSB 测试脚本，对 YCSB 原生测试脚本的封装，只能在本地执行测试
│   ├── execycsbtest.sac.sh       // YCSB 测试脚本，对 YCSB 原生测试脚本的封装，适配 sac，只能在本地执行测试
│   ├── prepare.sh                // YCSB 配置修改脚本
│   ├── ycsb                      // YCSB 原生测试脚本
│   ├── ycsb.bat                  // YCSB 原生测试脚本
│   └── ycsb.sh                   // YCSB 原生测试脚本
├── java                          // JDK 库，包括 x86_64 和 ARM
├── lib                           // YCSB 测试依赖库文件
├── LICENSE.txt                   // LICENSE 文件
├── mongodb-binding               // SequoiaDDS YCSB 驱动包
├── NOTICE.txt                    // NOTICE 文件
├── README.md                     // README 文件
├── script                        // exectest.sh 远程执行 YCSB 测试，机器和路径配置文件
│   ├── inventory.ini
│   └── remoteexec.yml
├── sequoiadb
│   ├── config.json               // ycsbPrepare.js 配置文件，通过该脚本可以自动生成
                                  // SequoiaDB quickDeploy.sh 配置文件，
                                  // 和修改 SequoiaDB 配置脚本
│   ├── README.md                 // README 文件
│   ├── run.sh                    // 对外层 exectest.sh 和 prepare.sh 脚本的封装，
                                  // 对 SequoiaDB YCSB 的测试场景和测试配置做了标准化，可以实现一键测试
│   ├── workloadsSample           // SequoiaDB YCSB 测试默认配置文件
│   ├── ycsbPrepare.js            // 通过该脚本可以自动生成 SequoiaDB quickDeploy.sh 配置文件，
                                  // 和修改 SequoiaDB 配置脚本，
│   └── ycsbPrepare.sh            // 封装了 ycsbPrepare.js 脚本，增加了通过 sdb shell 执行脚本的能力
├── sequoiadb-binding             // SequoiaDB YCSB 驱动包
├── sequoiadds
│   ├── README.md                 // README 文件
│   ├── run.sh                    // SequoiaDDS 一键测试脚本（未实现）
│   └── workloadsSample           // SequoiaDDS YCSB 测试默认配置文件
├── src                           // YCSB 源码
└── tmp
    └── ycsb.tmp.conf             // 临时文件，存放 prepare.sh 和 exectest.sh 脚本执行过程中产生的临时数据
```

