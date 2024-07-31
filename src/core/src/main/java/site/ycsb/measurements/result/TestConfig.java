package site.ycsb.measurements.result;
import java.sql.Timestamp;

/**
 * .Save the configuration for each round of testing.
 * i.e. .
 */
public class TestConfig {
  private int testRoundId;
  private int recordCount;
  private int maxExecutionTime;
  private Timestamp testTimestamp;

  // 构造函数、getter和setter方法
  public TestConfig() {
  }

  public int getTestRoundId() {
    return testRoundId;
  }

  public void setTestRoundId(int inTestRoundId) {
    this.testRoundId = inTestRoundId;
  }

  public int getRecordCount() {
    return recordCount;
  }

  public void setRecordCount(int inRecordCount) {
    this.recordCount = inRecordCount;
  }

  public int getMaxExecutionTime() {
    return maxExecutionTime;
  }

  public void setMaxExecutionTime(int inMaxExecutionTime) {
    this.maxExecutionTime = inMaxExecutionTime;
  }

  public Timestamp getTestTimestamp() {
    return testTimestamp;
  }

  public void setTestTimestamp(Timestamp inTestTimestamp) {
    this.testTimestamp = inTestTimestamp;
  }
}
