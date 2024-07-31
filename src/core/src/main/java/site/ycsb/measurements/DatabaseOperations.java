package site.ycsb.measurements;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import site.ycsb.measurements.result.TestConfig;
import site.ycsb.measurements.result.TestResult;
import site.ycsb.measurements.result.TimeRange;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Database Operation Class.
 * i.e. .
 */
public class DatabaseOperations {
  private Connection connection;
  public static final String CREATE_TEST_CONFIG_TABLE =
      "CREATE TABLE IF NOT EXISTS test_config (" +
      "test_round_id INT AUTO_INCREMENT PRIMARY KEY," +
      "record_count INT NOT NULL," +
      "max_execution_time INT NOT NULL," +
      "test_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
      ")ENGINE=InnoDB;";

  public static final  String CREATE_TEST_RESULTS_TABLE =
      "CREATE TABLE IF NOT EXISTS test_results (" +
      "test_id INT AUTO_INCREMENT PRIMARY KEY," +
      "test_round_id INT NOT NULL," +
      "scenario_name VARCHAR(255) NOT NULL," +
      "operation_type VARCHAR(20) NOT NULL," +
      "thread_count INT NOT NULL," +
      "throughput DOUBLE," +
      "average_latency DOUBLE," +
      "min_latency DOUBLE," +
      "max_latency DOUBLE," +
      "latency_95th_percentile BIGINT," +
      "latency_99th_percentile BIGINT," +
      "test_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
      "FOREIGN KEY (test_round_id) REFERENCES test_config(test_round_id)" +
      ")ENGINE=InnoDB;";

  private static final String LARGEST_TEST_ROUND_ID_QUERY = 
              "SELECT  max(test_round_id) from test_config;";
  private static final String SECOND_LARGEST_TEST_ROUND_ID_QUERY = 
                "SELECT DISTINCT test_round_id " +
                "FROM test_config " +
                "ORDER BY test_round_id DESC " +
                "LIMIT 1 OFFSET 1;";
  private static final String QUERY_HISTORY_RESULT = 
             "SELECT test_round_id,scenario_name,operation_type,thread_count,throughput," +
               "average_latency,min_latency,max_latency,latency_95th_percentile," +
               "latency_99th_percentile,test_timestamp" + 
             " FROM test_results " +
             " WHERE %s and operation_type <> 'CLEANUP'" +
             " GROUP BY test_round_id,scenario_name,operation_type,thread_count ;";
  
  private static final String QUERY_HISTORY_RESULT_BYID = 
            "SELECT test_round_id,scenario_name,operation_type,thread_count,throughput," +
              "average_latency,min_latency,max_latency,latency_95th_percentile," +
              "latency_99th_percentile,test_timestamp" +
            " FROM test_results " +
            " WHERE test_round_id in(%s) and operation_type <> 'CLEANUP'" +
            " GROUP BY  test_round_id,scenario_name,operation_type,thread_count ;";

  public DatabaseOperations(String jdbcUrl, String user, String password) throws SQLException {
    connection = DriverManager.getConnection(jdbcUrl, user, password);
  }

  private void createTable(String createTableSql) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(createTableSql)) {
      preparedStatement.execute();
    }
  }

  public void createConfigTest() throws SQLException{
    createTable(CREATE_TEST_CONFIG_TABLE);
  }
  
  public void createTestResults() throws SQLException{
    createTable(CREATE_TEST_RESULTS_TABLE);
  }

  public void createIndex(String createIndexSql) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(createIndexSql)) {
      preparedStatement.execute();
    }
  }

  public void insertTestConfigData(TestConfig testConfig) throws SQLException {
    String sql = "INSERT INTO test_config (test_round_id, record_count, max_execution_time, test_timestamp)" + 
                 " VALUES (?, ?, ?, ?)";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      preparedStatement.setInt(1, testConfig.getTestRoundId());
      preparedStatement.setInt(2, testConfig.getRecordCount());
      preparedStatement.setInt(3, testConfig.getMaxExecutionTime());
      preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

      int rowsAffected = preparedStatement.executeUpdate();
      if (rowsAffected == 1) {
        System.out.println("Data inserted into test_config.");
      } else {
        System.err.println("Insert into test_config failed.");
      }
    }
  }

  public void insertTestResultData(TestResult testResult) throws SQLException {
    String sql = "INSERT INTO test_results (test_round_id, scenario_name, operation_type, thread_count, throughput," + 
        "average_latency, min_latency, max_latency, latency_95th_percentile, latency_99th_percentile, test_timestamp)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      preparedStatement.setInt(1, testResult.getTestRoundId());
      preparedStatement.setString(2, testResult.getScenarioName());
      preparedStatement.setString(3, testResult.getOperationType());
      preparedStatement.setInt(4, testResult.getThreadCount());
      preparedStatement.setDouble(5, testResult.getThroughput());
      preparedStatement.setDouble(6, testResult.getAverageLatency());
      preparedStatement.setDouble(7, testResult.getMinLatency());
      preparedStatement.setDouble(8, testResult.getMaxLatency());
      preparedStatement.setLong(9, testResult.getLatency95thPercentile());
      preparedStatement.setLong(10, testResult.getLatency99thPercentile());
      preparedStatement.setTimestamp(11, new Timestamp(System.currentTimeMillis()));

      int rowsAffected = preparedStatement.executeUpdate();
      if (rowsAffected == 1) {
        System.out.println("Data inserted into test_results.");
      } else {
        System.err.println("Insert into test_results failed.");
      }
    }
  }

  private void getTestResult(Map<String, List<TestResult>> results, 
                   PreparedStatement preparedStatement, boolean manyRow){
    try {
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        TestResult result = new TestResult();        
        String scenarioName  = resultSet.getString("scenario_name");
        result.setScenarioName(scenarioName);
        result.setTestRoundId(resultSet.getInt("test_round_id"));
        String operType = resultSet.getString("operation_type");
        result.setOperationType(operType);
        result.setThreadCount(resultSet.getInt("thread_count"));
        result.setThroughput(resultSet.getDouble("throughput"));
        result.setAverageLatency(resultSet.getDouble("average_latency"));
        result.setMinLatency(resultSet.getDouble("min_latency"));
        result.setMaxLatency(resultSet.getDouble("max_latency"));
        result.setLatency95thPercentile(resultSet.getInt("latency_95th_percentile"));
        result.setLatency99thPercentile(resultSet.getInt("latency_99th_percentile"));
        result.setTestTimestamp(resultSet.getTimestamp("test_timestamp"));
        
        String key = scenarioName+operType;
        if (!results.containsKey(key)){
          results.put(key, new ArrayList<TestResult>());
        }
        results.get(key).add(result);
      }
      resultSet.close();
      preparedStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    } 
  }

  public Map<String, List<TestResult> > getTestResultByRoundId(int testRoundId){
    Map<String, List<TestResult>> results = new LinkedHashMap<String, List<TestResult>>();
    try {
      String query = "SELECT test_round_id,scenario_name, operation_type, thread_count,throughput," +
                  "average_latency,min_latency," +
                  "max_latency,latency_95th_percentile," +
                  "latency_99th_percentile,test_timestamp FROM test_results " +
                  "WHERE test_round_id = ? AND operation_type <> 'CLEANUP'" +
                  " GROUP BY scenario_name, operation_type,thread_count " +
                  "ORDER BY scenario_name, operation_type,thread_count;";

      PreparedStatement preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, testRoundId);
      getTestResult(results, preparedStatement, false);
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return results;    
  }

  public Map<String, List<TestResult> > getHistoryTestResultByTimeStamp(TimeRange timeRange){
    Map<String, List<TestResult>> results = new LinkedHashMap<String, List<TestResult>>();
    try {
      String newQuery = String.format(QUERY_HISTORY_RESULT, timeRange.getCondition());
      PreparedStatement preparedStatement = connection.prepareStatement(newQuery);
      getTestResult(results, preparedStatement, true);
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return results;
  }
  
  public Map<String, List<TestResult> > getHistoryTestResultById(String ids){
    Map<String, List<TestResult>> results = new LinkedHashMap<String, List<TestResult>>();
    try {
      String newQuery = String.format(QUERY_HISTORY_RESULT_BYID, ids);
      PreparedStatement preparedStatement = connection.prepareStatement(newQuery);
         
      getTestResult(results, preparedStatement, true);
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return results;
  }

  public int delete(String deleteSql) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSql)) {
      return preparedStatement.executeUpdate();
    }
  }

  public int update(String updateSql) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
      return preparedStatement.executeUpdate();
    }
  }

  public int getTestRoundId(String sql) throws SQLException {
    int maxTestRoundId = 0;
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          maxTestRoundId = resultSet.getInt(1);
        }
      }
    }
    return maxTestRoundId;
  }

  public int getCurrentTestRoundId() throws SQLException{
    return getTestRoundId(LARGEST_TEST_ROUND_ID_QUERY);
  }
    
  public int getPreviousTestRoundId() throws SQLException{
    return getTestRoundId(SECOND_LARGEST_TEST_ROUND_ID_QUERY);
  }
    
  public void close() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  public static void main(String[] args) {
    try {
      String jdbcUrl = "jdbc:mysql://192.168.50.51:3306/test";
      String user = "sdbadmin";
      String password = "sdbadmin";

      DatabaseOperations dbOps = new DatabaseOperations(jdbcUrl, user, password);

      // 创建表

      dbOps.createTable(CREATE_TEST_CONFIG_TABLE);
      dbOps.createTable(CREATE_TEST_RESULTS_TABLE);


      TestConfig testConfig = new TestConfig();
      testConfig.setRecordCount(1000);
      testConfig.setMaxExecutionTime(60);
      dbOps.insertTestConfigData(testConfig);

    } catch (SQLException e) {
      e.printStackTrace();
    }

  }
}

