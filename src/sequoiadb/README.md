<!--
Copyright (c) 2012 - 2016 YCSB contributors. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You
may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. See accompanying
LICENSE file.
-->

## Quick Start

This section describes how to run YCSB on SequoiaDB. 

### 1. deploy SequoiaDB


### 2. Install Java and Maven

Go to http://www.oracle.com/technetwork/java/javase/downloads/index.html

and get the url to download the rpm into your server. For example:

    wget http://download.oracle.com/otn-pub/java/jdk/7u40-b43/jdk-7u40-linux-x64.rpm?AuthParam=11232426132 -o jdk-7u40-linux-x64.rpm
    rpm -Uvh jdk-7u40-linux-x64.rpm
    
Or install via yum/apt-get

    sudo yum install java-devel

Download MVN from http://maven.apache.org/download.cgi

    wget http://ftp.heanet.ie/mirrors/www.apache.org/dist/maven/maven-3/3.1.1/binaries/apache-maven-3.1.1-bin.tar.gz
    sudo tar xzf apache-maven-*-bin.tar.gz -C /usr/local
    cd /usr/local
    sudo ln -s apache-maven-* maven
    sudo vi /etc/profile.d/maven.sh

Add the following to `maven.sh`

    export M2_HOME=/usr/local/maven
    export PATH=${M2_HOME}/bin:${PATH}

Reload bash and test mvn

    bash
    mvn -version

### 3. Set Up YCSB

Download the YCSB zip file and compile:

    curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz
    tar xfvz ycsb-0.17.0.tar.gz
    cd ycsb-0.17.0

### 4. Run YCSB

Now you are ready to run! First, use the  driver to load the data:

    ./bin/ycsb load sequoiadb -s -P workloads/workloada > outputLoad.txt

Then, run the workload:

    ./bin/ycsb run sequoiadb -s -P workloads/workloada > outputRun.txt
    
See the next section for the list of configuration parameters for SequoiaDB.


## SequoiaDB Configuration Parameters

- `sequoiadb.url`
  
- `sequoiadb.port`
   - Default value is `11810`
- `sequoiadb.host`
- `sequoiadb.keyfield`
   - Default value is `_id`
- `sequoiadb.user`
   - Default value is `sdbadmin`
- `sequoiadb.passwd`
   - Default value is `sdbadmin`
- `sequoiadb.maxConnectionnum`
  - Default value is `100`
- `sequoiadb.maxIdleConnectionnum`
  - Default value is `10`
- `sequoiadb.checkInterval`
   - Default value is `300`.
- `database`
   - Default value is `ycsb`.
- `batchsize`
  - Useful for the insert workload as it will submit the inserts in batches inproving throughput.
  - Default value is `1`.
