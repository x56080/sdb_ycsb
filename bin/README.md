SequoiaDB or SequoiaDDS Performance Test

It is a tool that interfaces with SequoiaDB or SequoiaDDS for performance testing. It supports batch execution of tests and generates simple HTML test reports.

## Configuration

1. Execute `prepare.sh` to modify the test configuration:

    ```bash
    ./prepare.sh -h
    ```

   Options:
   - `-h, --help`: Display this help message and exit
   - `-r, --runtime`: Set the duration of a single test scenario
   - `-s, --recordcount`: Set the amount of basic test data
   - `-u, --url`: Specify the connection string for the SequoiaDDS|SequoiaDB cluster
   - `-t, --threads`: Instruct a set of threads
   - `-c, --testcase`: Instruct a set of test cases
   - `-p, --product`: Specify the testing product (sequoiadb/sequoiadds)
   - `--hostlist`: Specify the list of deployment hosts
   - `--connstr`: Specify the MySQL connection URL
   - `--fieldcount`: Specify the number of fields in a record (default: 10)"
   - `--maxscanlength`: Specify the maximum number of records to be scanned in the scan operation (default: 1000)"



2. Execute `exectest.sh`:

    - Locally (no arguments needed):
      ```bash
      ./exectest.sh
      ```

    - Remotely (specify remote machine and directory):
      ```bash
      bash exectest.sh -c sdbadmin:Admin@1024@192.168.30.78 -d /data/ssd1/sequoiadb
      ```

   Use `-d` to specify the remote directory.

3. If generating test reports, check the `report` directory for the `ycsb.html` report.

**Note:** Adjust commands and options as needed for your specific use case.

