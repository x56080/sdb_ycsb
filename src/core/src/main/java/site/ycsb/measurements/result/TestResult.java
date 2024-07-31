package  site.ycsb.measurements.result;
import  java.sql.Timestamp;

/**
 * .Save the results of each test.
 * i.e. .
 */
public class TestResult {
  private int testId;
  private int testRoundId;
  private String scenarioName;
  private String operationType;
  private int threadCount;
  private double throughput;
  private double averageLatency;
  private double minLatency;
  private double maxLatency;
  private long latency95thPercentile;
  private long latency99thPercentile;
  private Timestamp testTimestamp;

  // 构造函数、getter和setter方法
  public TestResult() {
  }

  public  int  getTestId()  {
    return testId;
  }

  public void setTestId(int inTestId) {
    this.testId = inTestId;
  }

  public int getTestRoundId() {
    return testRoundId;
  }

  public void setTestRoundId(int inTestRoundId) {
    this.testRoundId = inTestRoundId;
  }

  public String getScenarioName() {
    return scenarioName;
  }

  public void setScenarioName(String inScenarioName) {
    this.scenarioName = inScenarioName;
  }

  public String getOperationType() {
    return operationType;
  }

  public void setOperationType(String inOperationType) {
    this.operationType = inOperationType;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public void setThreadCount(int inThreadCount) {
    this.threadCount = inThreadCount;
  }

  public double getThroughput() {
    return throughput;
  }

  public void setThroughput(double inThroughput) {
    this.throughput = inThroughput;
  }

  public double getAverageLatency() {
    return averageLatency;
  }

  public void setAverageLatency(double inAverageLatency) {
    this.averageLatency = inAverageLatency;
  }

  public double getMinLatency() {
    return minLatency;
  }

  public void setMinLatency(double inMinLatency) {
    this.minLatency = inMinLatency;
  }

  public double getMaxLatency() {
    return maxLatency;
  }

  public void setMaxLatency(double inMaxLatency) {
    this.maxLatency = inMaxLatency;
  }

  public long getLatency95thPercentile() {
    return latency95thPercentile;
  }

  public void setLatency95thPercentile(long inLatency95thPercentile) {
    this.latency95thPercentile = inLatency95thPercentile;
  }

  public long getLatency99thPercentile() {

    return latency99thPercentile;
  }

  public void setLatency99thPercentile(long inLatency99thPercentile) {
    this.latency99thPercentile = inLatency99thPercentile;
  }

  public Timestamp getTestTimestamp() {
    return testTimestamp;
  }

  public void setTestTimestamp(Timestamp inTestTimestamp) {
    this.testTimestamp = inTestTimestamp;
  }
}
