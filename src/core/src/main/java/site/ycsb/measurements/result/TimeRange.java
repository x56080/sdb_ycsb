package  site.ycsb.measurements.result;
/**
 * Time range select condition.
 * i.e. .
 */

public enum TimeRange {
  BY_YEAR("YEAR(test_timestamp) = YEAR(CURRENT_DATE)"),
  BY_MONTH("test_timestamp >= CURRENT_DATE - INTERVAL 1 MONTH and test_timestamp < CURRENT_DATE"),
  BY_WEEK("test_timestamp >= CURRENT_DATE - INTERVAL 1 WEEK and test_timestamp < CURRENT_DATE"),
  BY_CURRENT_WEEK("YEARWEEK(test_timestamp, 1) = YEARWEEK(CURRENT_DATE, 1)"),
  BY_CURRENT_DAY("DATE(test_timestamp) = CURRENT_DATE"),        
  BY_CURRENT_MONTH("EXTRACT(MONTH FROM test_timestamp) = EXTRACT(MONTH FROM CURRENT_DATE)" +
        "AND EXTRACT(YEAR FROM test_timestamp) = EXTRACT(YEAR FROM CURRENT_DATE)"),
  BY_ALL("1=1");

  private final String condition;

  TimeRange(String condition) {
    this.condition = condition;
  }

  public String getCondition() {
    return condition;
  }
}
