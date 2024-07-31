 /**
 * Copyright (c) 2010-2016 Yahoo! Inc., 2017 YCSB contributors All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package site.ycsb;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.ui.RectangleEdge;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.axis.ValueAxis;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import site.ycsb.measurements.DatabaseOperations;
import site.ycsb.measurements.result.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The HTMLReportGenerator class is responsible for generating HTML reports
 * based on the provided data. It offers methods to create various HTML
 * elements such as tables, charts, and text content. The generated HTML
 * report can be saved to a file or served directly to clients.
 * i.e. 
 */

public class HtmlReporter {
  private String testReportDir;
  private DatabaseOperations dbOps;
  private Map<String, List<Double>> scenario2Result = new LinkedHashMap<String, List<Double>>();
  private Map<String, Set<Integer>> scenario2Id = new LinkedHashMap<String, Set<Integer>>();
  private Map<String, Set<Integer>> scenario2Thread = new LinkedHashMap<String, Set<Integer>>();

  public HtmlReporter(String reportDir, DatabaseOperations oper){
    this.testReportDir = reportDir;
    Path path = Paths.get(this.testReportDir);
    if (!(Files.exists(path) && Files.isDirectory(path))){
      try{
        Files.createDirectories(path);
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
    this.dbOps = oper;
  }
  
  public void generateReport(){
    if (dbOps == null){
      return;
    }
    try {
      //Map<String, List<TestResult>> previousResults = dbOps.getTestResultByRoundId(dbOps.getPreviousTestRoundId());
      Map<String, List<TestResult>> previousResults = null;
      Map<String, List<TestResult>> currentResults = dbOps.getTestResultByRoundId(dbOps.getCurrentTestRoundId());
    
      //String[] seriesArray = {"previous", "current"};
      String[] seriesArray = {"current"};
      int tpsArraySize = 1;
      //if (previousResults.isEmpty()){
      //  tpsArraySize = 1;
      //  seriesArray = new String[tpsArraySize];
      //  seriesArray[0] = "current";
      //}
    
      for(String key: currentResults.keySet()){
        List<TestResult> currentResult = currentResults.get(key);
        //List<TestResult> previousResult = previousResults.get(key);

        String[] threads = new String[currentResult.size()];
        double[] tps = new double[currentResult.size()];
        /*if (previousResult != null && previousResult.size() == currentResult.size()){
          tps = new double[currentResult.size()*tpsArraySize];
        }else{
          tps = new double[currentResult.size()];
        }*/
            
        int j=0;
        /*if (previousResult != null && previousResult.size() == currentResult.size()){
          for(int i = 0; i < previousResult.size(); ++i){
            tps[j++] = previousResult.get(i).getThroughput();
          }
        }*/
            
        for(int i = 0; i < currentResult.size(); ++i){
          threads[i] = "" +currentResult.get(i).getThreadCount();
          tps[j++] = currentResult.get(i).getThroughput();
        }
        createComparisonChart(key, seriesArray, threads, tps);
      }
      generateHtml(previousResults, currentResults);
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public void generateHtml(Map<String, List<TestResult>> previousResults,
             Map<String, List<TestResult>> currentResults) {
    // Generate HTML content with tables and charts
    StringBuilder htmlContent = new StringBuilder();
    htmlContent.append("<html><head><title>Test Report</title></head><body>");
    
    for (Map.Entry<String, List<TestResult>> entry : currentResults.entrySet()) {
      String key = entry.getKey();
      List<TestResult> currentResult = entry.getValue();
      
      List<TestResult> previousResult = null;
      if (previousResults != null){
        previousResults.get(key);
      }
      
      if (previousResult != null){
        htmlContent.append("<h1>" + key + "</h1>");
        htmlContent.append("<table border='1'><tr><th>Thread Count</th><th>Throughput</th>" +
                     "<th>Average Latency</th><th>Min Latency</th><th>Max Latency</th>" +
                 "<th>Latency 95th Percentile</th><th>Latency 99th Percentile</th><th>Test Timestamp</th></tr>");
        for (TestResult result : previousResult) {
          htmlContent.append("<tr>");
          htmlContent.append("<td>" + result.getThreadCount() + "</td>");
          htmlContent.append("<td>" + result.getThroughput() + "</td>");
          htmlContent.append("<td>" + result.getAverageLatency() + "</td>");
          htmlContent.append("<td>" + result.getMinLatency() + "</td>");
          htmlContent.append("<td>" + result.getMaxLatency() + "</td>");
          htmlContent.append("<td>" + result.getLatency95thPercentile() + "</td>");
          htmlContent.append("<td>" + result.getLatency99thPercentile() + "</td>");
          htmlContent.append("<td>" + result.getTestTimestamp() + "</td>");
          htmlContent.append("</tr>");
        }
        htmlContent.append("</table>");
      }
     
      //Generate table for test data
      htmlContent.append("<h1>" + key + "</h1>");
      htmlContent.append("<table border='1'><tr><th>Thread Count</th><th>Throughput</th>" + 
               "<th>Average Latency</th><th>Min Latency</th><th>Max Latency</th>" +
             "<th>Latency 95th Percentile</th><th>Latency 99th Percentile</th><th>Test Timestamp</th></tr>");

      for (TestResult result : currentResult) {
        htmlContent.append("<tr>");
        htmlContent.append("<td>" + result.getThreadCount() + "</td>");
        htmlContent.append("<td>" + result.getThroughput() + "</td>");
        htmlContent.append("<td>" + result.getAverageLatency() + "</td>");
        htmlContent.append("<td>" + result.getMinLatency() + "</td>");
        htmlContent.append("<td>" + result.getMaxLatency() + "</td>");
        htmlContent.append("<td>" + result.getLatency95thPercentile() + "</td>");
        htmlContent.append("<td>" + result.getLatency99thPercentile() + "</td>");
        htmlContent.append("<td>" + result.getTestTimestamp() + "</td>");
        htmlContent.append("</tr>");
      }
      htmlContent.append("</table>");
      // Generate JavaScript code for a chart (e.g., using Chart.js)
      htmlContent.append("<h1>Chart for " + key + "</h1>");
      htmlContent.append("<img src='" + key + ".jpeg' width='640' height='480'>");        
    }
    htmlContent.append("</body></html>");

    // Save HTML content to a file
    // Write the 'htmlContent' to an HTML file, similar to the previous example
    saveHtmlReportToFile(htmlContent.toString(), "ycsb");
  }
    
  public void createComparisonChart(String title, String[] seriesArray, String[]categoryArray, double[]values){
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    int data = 0;
    for (int i=0; i<seriesArray.length; i++) {
      for (int j = 0; j < categoryArray.length; j++) {
        if (data < values.length){
          dataset.addValue(values[data], seriesArray[i], categoryArray[j]);
          data++;
        }
      }
    }
      
    // 设置字体，去除中文乱码
    StandardChartTheme sct = new StandardChartTheme("CN");
    sct.setExtraLargeFont(new Font("宋体", Font.LAYOUT_LEFT_TO_RIGHT, 15));
    sct.setRegularFont(new Font("宋体", Font.LAYOUT_LEFT_TO_RIGHT, 15));
    sct.setLargeFont(new Font("宋体", Font.LAYOUT_LEFT_TO_RIGHT, 15));
    ChartFactory.setChartTheme(sct);

    JFreeChart chart = ChartFactory.createBarChart3D(
                  title,
                  "", // 类别
                  "", // 值
                  dataset,
                  PlotOrientation.VERTICAL,
                  true, true, false);
    
    //得到绘图区
    CategoryPlot plot = (CategoryPlot) chart.getPlot();

    int width = 640;
    int height = 480;

    // 标注位于上侧
    chart.getLegend().setPosition(RectangleEdge.TOP);
    // 设置标注无边框
    chart.getLegend().setFrame(new BlockBorder(Color.WHITE));

    File p = new File(this.testReportDir);
    if (!p.exists()) {
      p.mkdirs();
    }
    String imageName = title + ".jpeg";
    File file = new File(p.getPath() + "/" + imageName);
    try {
      if(file.exists()) {
        file.delete();
      }
      ChartUtilities.saveChartAsJPEG(file, chart, width, height);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
    
  public void saveHtmlReportToFile(String htmlContent, String fileName) {
    // 构建完整的文件路径
    String filePath = this.testReportDir + "/" + fileName + ".html";
    try {
      // 创建一个文件写入器
      FileWriter fileWriter = new FileWriter(filePath);

      // 写入HTML内容
      fileWriter.write(htmlContent);
      
      // 关闭文件写入器
      fileWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
      // 处理文件写入时的异常
    }
  }

  public void generateCompareHtml(String range) {
    // Generate HTML content with tables and charts
    Map<String, List<TestResult>>results = null;
    try{
      TimeRange timeRange = TimeRange.valueOf(range);
      results = dbOps.getHistoryTestResultByTimeStamp(timeRange);
    }catch(IllegalArgumentException e){
      results = dbOps.getHistoryTestResultById(range);
    }
    
    StringBuilder htmlContent = new StringBuilder();
    htmlContent.append("<html><head><title>Test Report</title></head><body>");
      
    for (Map.Entry<String, List<TestResult>> entry : results.entrySet()) {
      String key = entry.getKey();
      List<TestResult> currentResult = entry.getValue();
      //Generate table for test data
      htmlContent.append("<h1>" + key + "</h1>");
      htmlContent.append("<table border='1'><tr><th>jobid</th><th>Thread Count</th><th>Throughput</th>" + 
                 "<th>Average Latency</th><th>Min Latency</th><th>Max Latency</th>" +
               "<th>Latency 95th Percentile</th><th>Latency 99th Percentile</th><th>Test Timestamp</th></tr>");

      for (TestResult result : currentResult) {
        htmlContent.append("<tr>");
        htmlContent.append("<td>" + result.getTestRoundId() + "</td>");
        htmlContent.append("<td>" + result.getThreadCount() + "</td>");
        htmlContent.append("<td>" + result.getThroughput() + "</td>");
        htmlContent.append("<td>" + result.getAverageLatency() + "</td>");
        htmlContent.append("<td>" + result.getMinLatency() + "</td>");
        htmlContent.append("<td>" + result.getMaxLatency() + "</td>");
        htmlContent.append("<td>" + result.getLatency95thPercentile() + "</td>");
        htmlContent.append("<td>" + result.getLatency99thPercentile() + "</td>");
        htmlContent.append("<td>" + result.getTestTimestamp() + "</td>");
        htmlContent.append("</tr>");
      }
      htmlContent.append("</table>");
      // Generate JavaScript code for a chart (e.g., using Chart.js)
          
    }  
    genCompareChart(results, htmlContent);
    htmlContent.append("</body></html>");

    // Save HTML content to a file
    // Write the 'htmlContent' to an HTML file, similar to the previous example
    saveHtmlReportToFile(htmlContent.toString(), "ycsb_history");
  }

  public void convResult(Map<String, List<TestResult>>results){  
    Map<String, Map<Integer, Integer>> scenario2ValueCnt  = new HashMap<String, Map<Integer, Integer>>();
    List<Double> tpsset = null;
    Set<Integer> threads = null;
    Set<Integer> ids = null;
    
    for (Entry<String, List<TestResult>> entry: results.entrySet()){
      String key = entry.getKey();
      List<TestResult> result = entry.getValue();
      
      String[] parts = key.split("_");
      assert parts.length != 3;
      String scenario = parts[0];
      if (!scenario2Thread.containsKey(scenario)){
        scenario2Thread.put(scenario, new LinkedHashSet<Integer>());
      }
      threads = scenario2Thread.get(scenario);
      
      if (!scenario2Id.containsKey(scenario)){
        scenario2Id.put(scenario, new LinkedHashSet<Integer>());
      }
      ids = scenario2Id.get(scenario);
      
      if (!scenario2Result.containsKey(scenario)){
        scenario2Result.put(scenario, new ArrayList<Double>());
        
      }
      tpsset = scenario2Result.get(scenario);
      if (!scenario2ValueCnt.containsKey(scenario)){
        scenario2ValueCnt.put(scenario, new HashMap<Integer, Integer>());
      }
      
      for(TestResult res : result){
        Map<Integer, Integer> thread2id = scenario2ValueCnt.get(scenario);
        if (thread2id.isEmpty()){
          thread2id.put(res.getThreadCount(), res.getTestRoundId());
        }else if (thread2id.values().contains(res.getTestRoundId())){
          thread2id.put(res.getThreadCount(), res.getTestRoundId()); 
        }
        
        if (thread2id.containsKey(res.getThreadCount())){
          ids.add(res.getTestRoundId());
          tpsset.add(res.getThroughput());
          threads.add(res.getThreadCount());
        }
      }
    }
    return;
  }
  public void genCompareChart(Map<String, List<TestResult>> results, StringBuilder htmlContent){
    convResult(results);
    for (Entry<String, List<Double>> entry: scenario2Result.entrySet()){
      String[] title = new String[scenario2Thread.get(entry.getKey()).size()];
      int i = 0;
      for (Integer thread: scenario2Thread.get(entry.getKey())){
        title[i++] = String.format("thread-%d", thread);
      }
      
      Set<Integer> ids = scenario2Id.get(entry.getKey());
      String[] xValue = new  String[ids.size()];
      i = 0;
      for (Integer id : ids){
        xValue[i++] = Integer.toString(id);
      }
      
      if (xValue.length > 1){
        createManyLineChartByTestResult(entry.getKey(), title, xValue, entry.getValue(), htmlContent);
      }else{
        createManyLineChartByTestResult(entry.getKey(), xValue, title, entry.getValue(), htmlContent); 
      }
    }
  }
  
  private  void createManyLineChartByTestResult(String chartTitle, String[] title, 
                                String[] xValue, List<Double> tpss, StringBuilder htmlContent){
    // 绘图数据集
    DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
    
    int pos = 0;
    for (int i =0; i<title.length; i++) {
      for (int j=0; j<xValue.length; j++) {   
        if (pos < tpss.size()){
          dataSet.setValue(tpss.get(pos), title[i], xValue[j]);
          pos++;
        }
      }
    }
    JFreeChart chart = createManyLineChart(chartTitle, dataSet);
    File p = new File(this.testReportDir);
    if (!p.exists()) {
      p.mkdirs();
    }
    String imageName = chartTitle + ".jpeg";
    File file = new File(p.getPath() + "/" + imageName);
    try {
      if(file.exists()) {
        file.delete();
      }
      ChartUtilities.saveChartAsJPEG(file, chart, 800, 600);
    } catch (IOException e) {
      e.printStackTrace();
    }
  
    htmlContent.append("<h1>Chart for " + chartTitle + "</h1>");
    htmlContent.append("<img src='" + chartTitle + ".jpeg' width='640' height='480'>");     
  }

  public  JFreeChart createManyLineChart(String chartTitle, DefaultCategoryDataset dataSet){
    String newChartTitle = chartTitle + " tps";
    //如果把createLineChart改为createLineChart3D就变为了3D效果的折线图
    JFreeChart  chart = ChartFactory.createLineChart(newChartTitle, "job", "tps", dataSet,
              PlotOrientation.VERTICAL, // 绘制方向
              true, // 显示图例
              true, // 采用标准生成器
              false // 是否生成超链接
    );
    //如 果不使用Font,中文将显示不出来
    Font font = new Font("新宋体", Font.BOLD, 15);

    chart.getTitle().setFont(font);  // 设置标题字体
    chart.getLegend().setItemFont(font); // 设置图例类别字体
    // chart.setBackgroundPaint(); // 设置背景色
    //获取绘图区对象
    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.LIGHT_GRAY); // 设置绘图区背景色
    plot.setRangeGridlinePaint(Color.gray); // 设置水平方向背景线颜色
    // 设置背景透明度
    plot.setBackgroundAlpha(0.1f);
    // 设置网格横线颜色
    plot.setRangeGridlinePaint(Color.gray);
    // 设置网格横线大小
    plot.setDomainGridlineStroke(new BasicStroke(0.2F));
    plot.setRangeGridlineStroke(new BasicStroke(0.2F));
    plot.setRangeGridlinesVisible(true); // 设置是否显示水平方向背景线,默认值为true
    plot.setDomainGridlinePaint(Color.WHITE); // 设置垂直方向背景线颜色
    plot.setDomainGridlinesVisible(true);  // 设置是否显示垂直方向背景线,默认值为false

    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setLabelFont(font); // 设置横轴字体
    domainAxis.setTickLabelFont(font); // 设置坐标轴标尺值字体
    domainAxis.setLowerMargin(0.01); // 左边距 边框距离
    domainAxis.setUpperMargin(0.06); // 右边距 边框距离,防止最后边的一个数据靠近了坐标轴。
    domainAxis.setMaximumCategoryLabelLines(2);

    ValueAxis rangeAxis = plot.getRangeAxis();
    rangeAxis.setLabelFont(font);
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); //Y轴显示整数
    rangeAxis.setAutoRangeMinimumSize(1);   //最小跨度
    rangeAxis.setUpperMargin(0.18); //上边距,防止最大的一个数据靠近了坐标轴。
    rangeAxis.setLowerBound(0);   //最小值显示0
    rangeAxis.setAutoRange(false);   //不自动分配Y轴数据
    rangeAxis.setTickMarkStroke(new BasicStroke(1.6f));     // 设置坐标标记大小
    rangeAxis.setTickMarkPaint(Color.BLACK);     // 设置坐标标记颜色

    // 获取折线对象
    LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    BasicStroke realLine = new BasicStroke(1.8f); // 设置实线
    // 设置虚线
    float[] dashes = {5.0f};
    BasicStroke brokenLine = new BasicStroke(2.2f, // 线条粗细
              BasicStroke.CAP_ROUND, // 端点风格
              BasicStroke.JOIN_ROUND, // 折点风格
              8f, dashes, 0.6f);
    for (int i = 0; i < dataSet.getRowCount(); i++) {
      if (i % 2 == 0) {
        renderer.setSeriesStroke(i, realLine); // 利用实线绘制
      } else {
        renderer.setSeriesStroke(i, brokenLine); // 利用虚线绘制
      }
      // 生成折线图上的数字
      //绘图区域(红色矩形框的部分)
      renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
      //设置图表上的数字可见
      renderer.setBaseItemLabelsVisible(true);
      //设置图表上的数字字体
      renderer.setBaseItemLabelFont(new Font("宋体", Font.BOLD, 15));

      //设置折线图拐角上的正方形
      //创建一个正方形
      Rectangle  shape=new Rectangle(4, 4); 
      renderer.setSeriesShape(0, shape); 
      //设置拐角上图形可见
      renderer.setSeriesShapesVisible(0, true);
    }
    plot.setNoDataMessage("无对应的数据，请重新查询。");
    plot.setNoDataMessageFont(font); //字体的大小
    plot.setNoDataMessagePaint(Color.RED); //字体颜色
    return chart;
  }
}
