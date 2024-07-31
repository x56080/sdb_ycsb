/**
 * Copyright (c) 2012 - 2015 YCSB contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

/*
 * SequoiaDB client binding for YCSB.
 *
 * Submitted by wangwenjing on 9/12/2023.
 *
 */
package site.ycsb.db;

import site.ycsb.ByteArrayByteIterator;
import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.Status;

import org.bson.types.Binary;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.base.options.InsertOption;
import com.sequoiadb.base.result.DeleteResult;
import com.sequoiadb.base.result.InsertResult;
import com.sequoiadb.base.result.UpdateResult;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigInteger;

/**
 * SequoiaDB binding for YCSB framework using the SequoiaDB Inc. <a
 * href="https://doc.sequoiadb.com/cn/index/Public/Home/document/702/api/java/html/index.html">driver</a>
 * <p>
 * See the <code>README.md</code> for configuration information.
 * </p>
 *
 * @author ypai
 * @see <a href="https://doc.sequoiadb.com/cn/index/Public/Home/document/702/api/java/html/index.html">SequoiaDB Inc.
 *      driver</a>
 */

public class SequoiaDbClient extends DB {
  /** Used to include a field in a response. */
  private static final Integer INCLUDE = Integer.valueOf(1);

  /**
   * The database name to access.
   */
  private static String databaseName;
  private static String keyfield;

  /**
   * Count the number of times initialized to teardown on the last
   * {@link #cleanup()}.
   */
  private static final AtomicInteger INIT_COUNT = new AtomicInteger(0);
  private static SequoiadbDatasource sdbClient = null;
  private Sequoiadb sequoiadb = null;

  private CollectionSpace cs = null;
  private DBCollection cl = null;

  /** The batch size to use for inserts. */
  private static int batchSize;

  /** The bulk inserts pending for the thread. */
  private final List<BSONObject> bulkInserts = new ArrayList<BSONObject>();
  private static final String KEY_FILED = "_id";
  private static int subClNum = 10;

  /**
   * Cleanup any state for this DB. Called once per DB instance; there is one
   * DB instance per client thread.
   */
  @Override
  public void cleanup() throws DBException {
    try {
      if (0 != bulkInserts.size()) {
        InsertOption option = new InsertOption();
        option.appendFlag(InsertOption.FLG_INSERT_CONTONDUP);
        cl.bulkInsert(bulkInserts, option);
      }
      if (INIT_COUNT.decrementAndGet() == 0) {
        sdbClient.close();
      }
    } catch (Exception e1) {
      System.err.println("Could not close SequoiaDB connection pool: " + e1.toString());
      e1.printStackTrace();
      return;
    } finally {
      cs = null;
      sdbClient = null;
    }
  }

  private DBCollection createSubCollection(String table) throws DBException {
    BSONObject options = new BasicBSONObject();
    BSONObject subobj = new BasicBSONObject();
    subobj.put(KEY_FILED, 1);
    options.put("ShardingKey", subobj);
    options.put("EnsureShardingIndex", false);
    options.put("AutoSplit", true);
    options.put("Compressed", false);

    return cs.createCollection(table, options);
  }

  private DBCollection createCollection(String table) throws DBException {
    BSONObject options = new BasicBSONObject();
    BSONObject subobj = new BasicBSONObject();
    subobj.put(KEY_FILED, 1);
    options.put("ShardingKey", subobj);
    options.put("IsMainCL", true);

    DBCollection collection = cs.createCollection(table, options);
    for (int i = 0; i < subClNum; ++i) {
      BSONObject attachOptions = new BasicBSONObject();
      DBCollection subCollection = createSubCollection(String.format("%s_%d", table, i));
      BSONObject lowBound = new BasicBSONObject().append(KEY_FILED, getBound(subClNum, i));
      BSONObject upBound = new BasicBSONObject().append(KEY_FILED, getBound(subClNum, i + 1));
      attachOptions.put("LowBound", lowBound);
      attachOptions.put("UpBound", upBound);
      collection.attachCollection(subCollection.getFullName(), attachOptions);
    }
    if (0 != keyfield.compareTo(KEY_FILED)) {
      collection.createIndex("index", "{" + keyfield + ":1}", true, true);
    }
    return collection;
  }

  private String getBound(int subCollectionNum, int pos) {
    BigInteger maxValue = new BigInteger("10000000000000000000");
    BigInteger interval = maxValue.divide(BigInteger.valueOf(subCollectionNum));
    if (BigInteger.valueOf(pos).multiply(interval).compareTo(maxValue) >= 0) {
      return "user9999999999999999999";
    }

    String formattedValue = String.format("%019d", BigInteger.valueOf(pos).multiply(interval));
    return "user" + formattedValue;
  }

  public DBCollection getCollection(String table) throws DBException {
    try{
      if (sequoiadb == null) {
        cs = null;
        cl = null;
        sequoiadb = sdbClient.getConnection();
      }
      if (cs == null) {
        cl = null;
        cs = sequoiadb.getCollectionSpace(databaseName);
      }

      synchronized (INCLUDE) {
        if (cl != null) {
          return cl;
        }
        if (cs.isCollectionExist(table)){
          cl = cs.getCollection(table);
        }else{
          cl = createCollection(table);
        }
      }
    } catch (DBException | InterruptedException e) {
      e.printStackTrace();
      throw new DBException(e);
    }
    return cl;
  }

  /**
   * Delete a record from the database.
   *
   * @param table
   *            The name of the table
   * @param key
   *            The record key of the record to delete.
   * @return Zero on success, a non-zero error code on error. See the
   *         {@link DB} class's description for a discussion of error codes.
   */
  @Override
  public Status delete(String table, String key) {
    try {
      DBCollection collection = getCollection(table);
      BSONObject query = new BasicBSONObject().append(KEY_FILED, key);
      DeleteResult result = collection.deleteRecords(query);
      if (result.getDeletedNum() == 0) {
        System.err.println("Nothing deleted for key " + key);
        return Status.NOT_FOUND;
      }
      return Status.OK;
    } catch (Exception e) {
      System.err.println(e.toString());
      return Status.ERROR;
    }
  }

  /**
   * Initialize any state for this DB. Called once per DB instance; there is
   * one DB instance per client thread.
   */
  @Override
  public void init() throws DBException {
    INIT_COUNT.incrementAndGet();
    synchronized (INCLUDE) {
      if (sdbClient != null) {
        try {
          System.out.println(Thread.currentThread().getName());
          sequoiadb = sdbClient.getConnection();
          cs = sequoiadb.getCollectionSpace(databaseName);
        } catch (BaseException | InterruptedException e) {
          e.printStackTrace();
          throw new DBException(e);
        }
        return;
      }

      List<String> addressList = new ArrayList<>();
      Properties props = getProperties();
      // Set insert batchsize, default 1 - to be YCSB-original equivalent
      batchSize = Integer.parseInt(props.getProperty("batchsize", "1"));
      String surl = props.getProperty("sequoiadb.url", "");
      if (!surl.isEmpty()) {
        for (String url : surl.split(",")) {
          addressList.add(url);
        }
      }else{
        String port = props.getProperty("sequoiadb.port", "11810");
        for (String host : props.getProperty("sequoiadb.host", "localhost").split(",")) {
          addressList.add(String.format("%s:%s", host, port));
        }
      }

      keyfield = props.getProperty("sequoiadb.keyfield", "_id");
      int maxConnectionnum = Integer.parseInt(props.getProperty("sequoiadb.maxConnectionnum", "100"));
      int maxidleconnnum = Integer.parseInt(props.getProperty("sequoiadb.maxConnectionnum", "10"));
      int checkInterval = Integer.parseInt(props.getProperty("sequoiadb.checkInterval", "300"));
      databaseName = props.getProperty("database", "ycsb");
      subClNum = Integer.parseInt(props.getProperty("sequoiadb.subCollectionNum", "10"));
      String user = props.getProperty("sequoiadb.user", "sdbadmin");
      String passwd = props.getProperty("sequoiadb.passwd", "sequoiadb");

      ConfigOptions configOptions = new ConfigOptions();
      DatasourceOptions datasourceOptions = new DatasourceOptions();
      datasourceOptions.setMaxCount(maxConnectionnum);
      datasourceOptions.setMaxIdleCount(maxidleconnnum);
      datasourceOptions.setCheckInterval(checkInterval);

      sdbClient = new SequoiadbDatasource(addressList, user, passwd, configOptions, datasourceOptions);
      try {
        sequoiadb = sdbClient.getConnection();
        if (sequoiadb.isCollectionSpaceExist(databaseName)) {
          cs = sequoiadb.getCollectionSpace(databaseName);
        } else {
          cs = sequoiadb.createCollectionSpace(databaseName);
        }
      } catch (BaseException | InterruptedException e) {
        e.printStackTrace();
        throw new DBException(e);
      }
    }
  }

  /**
   * Insert a record in the database. Any field/value pairs in the specified
   * values HashMap will be written into the record with the specified record
   * key.
   *
   * @param table
   *            The name of the table
   * @param key
   *            The record key of the record to insert.
   * @param values
   *            A HashMap of field/value pairs to insert in the record
   * @return Zero on success, a non-zero error code on error. See the
   *         {@link DB} class's description for a discussion of error codes.
   */
  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    try {
      DBCollection collection = getCollection(table);
      BSONObject toInsert = new BasicBSONObject().append(KEY_FILED, key);
      for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
        toInsert.put(entry.getKey(), entry.getValue().toArray());
      }
      InsertResult result = null;
      if (batchSize == 1) {
        result = collection.insertRecord(toInsert);
      } else {
        bulkInserts.add(toInsert);
        if (bulkInserts.size() == batchSize) {
          InsertOption option = new InsertOption();
          option.appendFlag(InsertOption.FLG_INSERT_CONTONDUP);
          result = collection.bulkInsert(bulkInserts, option);
          bulkInserts.clear();
        } else {
          return Status.BATCHED_OK;
        }
      }
      if (result.getInsertNum() == batchSize) {
        return Status.OK;
      } else {
        return Status.ERROR;
      }
    } catch (Exception e) {
      System.err.println("Exception while trying bulk insert with " + bulkInserts.size());
      e.printStackTrace();
      return Status.ERROR;
    }
  }

  /**
   * Read a record from the database. Each field/value pair from the result
   * will be stored in a HashMap.
   *
   * @param table
   *            The name of the table
   * @param key
   *            The record key of the record to read.
   * @param fields
   *            The list of fields to read, or null for all of them
   * @param result
   *            A HashMap of field/value pairs for the result
   * @return Zero on success, a non-zero error code on error or "not found".
   */
  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    DBCursor cursor = null;
    try {
      DBCollection collection = getCollection(table);
      BSONObject query = new BasicBSONObject().append(KEY_FILED, key);
      BSONObject projection = null;
      if (fields != null) {
        projection = new BasicBSONObject();
        for (String field : fields) {
          projection.put(field, INCLUDE);
        }
      }
      cursor = collection.query(query, projection, null, null, 0, -1, DBQuery.FLG_QUERY_WITH_RETURNDATA);
      if (cursor != null && cursor.hasNext()) {
        fillMap(result, cursor.getNext());
        return Status.OK;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (Exception e) {
      System.err.println(e.toString());
      return Status.ERROR;
    }
  }

  /**
   * Perform a range scan for a set of records in the database. Each
   * field/value pair from the result will be stored in a HashMap.
   *
   * @param table
   *            The name of the table
   * @param startkey
   *            The record key of the first record to read.
   * @param recordcount
   *            The number of records to read
   * @param fields
   *            The list of fields to read, or null for all of them
   * @param result
   *            A Vector of HashMaps, where each HashMap is a set field/value
   *            pairs for one record
   * @return Zero on success, a non-zero error code on error. See the
   *         {@link DB} class's description for a discussion of error codes.
   */
  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
      Vector<HashMap<String, ByteIterator>> result) {
    DBCursor cursor = null;
    try {
      DBCollection collection = getCollection(table);
      BSONObject scanRange = new BasicBSONObject().append("$gte", startkey);
      BSONObject query = new BasicBSONObject().append(KEY_FILED, scanRange);
      BSONObject sort = new BasicBSONObject().append(KEY_FILED, INCLUDE);
      BSONObject hint = new BasicBSONObject().append("", "");
      BSONObject projection = null;

      if (fields != null) {
        projection = new BasicBSONObject();
        for (String fieldName : fields) {
          projection.put(fieldName, INCLUDE);
        }
      }
      cursor = collection.query(query, projection, sort, hint, 0, recordcount);
      if (!cursor.hasNext()) {
        System.err.println("Nothing found in scan for key " + startkey);
        return Status.ERROR;
      }
      result.ensureCapacity(recordcount);
      while (cursor.hasNext()) {
        HashMap<String, ByteIterator> resultMap = new HashMap<String, ByteIterator>();
        BSONObject obj = cursor.getNext();
        fillMap(resultMap, obj);
        result.add(resultMap);
      }
      return Status.OK;
    } catch (Exception e) {
      System.err.println(e.toString());
      return Status.ERROR;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Update a record in the database. Any field/value pairs in the specified
   * values HashMap will be written into the record with the specified record
   * key, overwriting any existing values with the same field name.
   *
   * @param table
   *            The name of the table
   * @param key
   *            The record key of the record to write.
   * @param values
   *            A HashMap of field/value pairs to update in the record
   * @return Zero on success, a non-zero error code on error. See this class's
   *         description for a discussion of error codes.
   */
  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    try {
      DBCollection collection = getCollection(table);
      BSONObject query = new BasicBSONObject().append(KEY_FILED, key);
      BSONObject fieldsToSet = new BasicBSONObject();

      for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
        fieldsToSet.put(entry.getKey(), entry.getValue().toArray());
      }

      BSONObject update = new BasicBSONObject().append("$set", fieldsToSet);
      UpdateResult result = collection.updateRecords(query, update, null);
      if (result.getModifiedNum() == 1) {
        return Status.OK;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (Exception e) {
      System.err.println(e.toString());
      return Status.ERROR;
    }
  }

  /**
   * Fills the map with the values from the DBObject.
   *
   * @param resultMap
   *            The map to fill/
   * @param obj
   *            The object to copy values from.
   */
  protected void fillMap(Map<String, ByteIterator> resultMap, BSONObject obj) {
    for (String key : obj.keySet()) {
      Object value = obj.get(key);
      if (value instanceof Binary) {
        resultMap.put(key, new ByteArrayByteIterator(((Binary) value).getData()));
      }
    }
  }

  public static void main(String[] args) {
    for (int i = 0; i < 10; ++i){
      Thread thread = new Thread(){
        public void run() {
          SequoiaDbClient dbClient = new SequoiaDbClient();
          try {
            Properties p = new Properties();
            p.setProperty("sequoiadb.host", "192.168.30.67,192.168.30.68,192.168.30.69");
            p.setProperty("database", "ycsb");
            p.setProperty("sequoiadb.user", "sdbadmin");
            p.setProperty("sequoiadb.passwd", "sdbadmin");
            dbClient.setProperties(p);
            dbClient.init();
            dbClient.getCollection("usertable");
          } catch (DBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      };
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
