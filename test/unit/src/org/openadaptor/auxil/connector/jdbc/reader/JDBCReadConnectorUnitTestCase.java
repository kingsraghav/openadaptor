/*
 Copyright (C) 2001 - 2007 The Software Conservancy as Trustee. All rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in the
 Software without restriction, including without limitation the rights to use, copy,
 modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 and to permit persons to whom the Software is furnished to do so, subject to the
 following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Nothing in this notice shall be deemed to grant any rights to trademarks, copyrights,
 patents, trade secrets or any other intellectual property of the licensor or any
 contributor except as expressly stated herein. No patent license is granted separate
 from the Software, for code that you delete from the Software, or for combinations
 of the Software with other software or hardware.
 */
package org.openadaptor.auxil.connector.jdbc.reader;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.jmock.Mock;
import org.openadaptor.auxil.orderedmap.IOrderedMap;
import org.openadaptor.auxil.orderedmap.OrderedHashMap;
import org.openadaptor.core.IReadConnector;
import org.openadaptor.core.connector.DBEventDrivenPollingReadConnector;
import org.openadaptor.core.exception.ConnectionException;

/**
 * Unit tests for {@link JDBCReadConnector}. 
 * The tests verify that {@link JDBCReadConnector}, perhaps in combination with a {@link IPollingReadConnector}
 * implementation, is compatible with the pre 3.3 JDBC read connectors which it has replaced:
 * {@OldJDBCReadConnector)
 * {@JDBCPollConnector}
 * {@JDBCEventReadConnector}
 * 
 * Additionally, some tests verify new functionality such as batching (that didn't exist in
 * any of the legacy connectors).
 * 
 * @author Kris Lachor
 */
public class JDBCReadConnectorUnitTestCase extends AbstractJDBCConnectorTest{
  
  private JDBCReadConnector jdbcReadConnector = new JDBCReadConnector();
  
  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    jdbcReadConnector.setId("Test Read Connector");
    jdbcReadConnector.setSql(SQL);
    jdbcReadConnector.setJdbcConnection(mockConnection);    
  }

  
  /**
   * Tests {@link JDBCReadConnector#connect}.
   */
  public void testConnect(){
    mockSqlConnection.expects(once()).method("createStatement").will(returnValue(mockStatement.proxy()));
    assertNull(jdbcReadConnector.statement);
    jdbcReadConnector.connect();
    assertEquals(mockStatement.proxy(), jdbcReadConnector.statement);
  }
  
  /**
   * Tests {@link JDBCReadConnector#connect}.
   * Creating the statement throws exception.
   */
  public void testConnect2(){
    mockSqlConnection.expects(once()).method("createStatement").will(throwException(new SQLException("test", "test")));
    assertNull(jdbcReadConnector.statement);
    try{ 
       jdbcReadConnector.connect();
    }catch(ConnectionException ce){
       assertNull(jdbcReadConnector.statement);
       return;
    }
    assertTrue(false);
  }
  
  /**
   * Tests {@link JDBCReadConnector#connect}.
   * Ported from the legacy {@link JDBCEventReadConnectorUnitTestCase#testConnect()}.
   */
  public void testConnect3(){
    DBEventDrivenPollingReadConnector pollingReadConnector = new DBEventDrivenPollingReadConnector();
    pollingReadConnector.setEventServiceID("10");
    pollingReadConnector.setEventTypeID("20");
    pollingReadConnector.setJdbcConnection(mockConnection);
    pollingReadConnector.setDelegate(jdbcReadConnector);
    Mock mockCallableStatement =  new Mock(CallableStatement.class);   
    connectDBEventDrivenConnector(mockCallableStatement, pollingReadConnector);
  }
  
  /**
   * Test the scenario when the afterConnectSql property is set.
   * 
   * Tests {@link JDBCReadConnector#connect}.
   */
  public void testConnectWithAfterConnectSql(){
    Mock mockAfterConnectPreparedStatement = new Mock(PreparedStatement.class);
    String afterConnectSql = "SELECT * FROM ...";
    mockSqlConnection.expects(once()).method("createStatement").will(returnValue(mockStatement.proxy()));
    mockSqlConnection.expects(once()).method("prepareStatement").with(eq(afterConnectSql))
      .will(returnValue(mockAfterConnectPreparedStatement.proxy()));
    mockAfterConnectPreparedStatement.expects(once()).method("execute").will(returnValue(false));
    mockAfterConnectPreparedStatement.expects(once()).method("close");
    assertNull(jdbcReadConnector.statement);
    jdbcReadConnector.setAfterConnectSql(afterConnectSql);
    jdbcReadConnector.connect();
    assertEquals(mockStatement.proxy(), jdbcReadConnector.statement);
  }
  
  /**   
   * Tests {@link JDBCReadConnector#disconnect}.
   */
  public void testDisonnect(){
    testConnect();
    mockStatement.expects(once()).method("close");
    mockSqlConnection.expects(once()).method("close");
    jdbcReadConnector.disconnect();
  }
  
  /**   
   * Tests {@link JDBCReadConnector#disconnect}.
   */
  public void testDisonnectWithBeforeDisconnectSql(){
    Mock mockBeforeDisconnectPreparedStatement = new Mock(PreparedStatement.class);
    String beforeDisconnectSql = "SELECT * FROM ...";
    jdbcReadConnector.setBeforeDisconnectSql(beforeDisconnectSql);
    testConnect();
    mockStatement.expects(once()).method("close");
    
    /* Part specific to beforeDisconnectSql */
    mockSqlConnection.expects(once()).method("prepareStatement").with(eq(beforeDisconnectSql))
      .will(returnValue(mockBeforeDisconnectPreparedStatement.proxy()));
    mockBeforeDisconnectPreparedStatement.expects(once()).method("execute").will(returnValue(false));
    mockBeforeDisconnectPreparedStatement.expects(once()).method("close");
    
    mockSqlConnection.expects(once()).method("close");
    jdbcReadConnector.disconnect();
  }
  
  /**
   * Test method for {@link org.openadaptor.auxil.connector.jdbc.reader.JDBCReadConnector#next(long)}.
   * Initialises mock interfaces to return a result set with one column and one row.
   * One call to the {@link JDBCReadConnector#next(long)} method.
   * Ported from the legacy OldJDBCReadConnectorUnitTestCase.
   */
  public void testNextOneRow1() {
    setMocksToReturnNumberOfRows(1);
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 1);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
  }
  
  /**
   * Test method for {@link org.openadaptor.auxil.connector.jdbc.reader.JDBCReadConnector#next(long)}.
   * This is identical to #testNextOneRow1 except that <code>postSubstitutionSql</code> is set and therefore
   * should be used in place of <code>sql</code>. 
   */
  public void testNextOneRow1WithPostSubtitutionSqlSet() {
    String someSQL = "Some other SQL";
    jdbcReadConnector.postSubstitutionSql = someSQL;
    
    /* slightly modified version of  setMocksToReturnNumberOfRows(1)*/
    mockStatement.expects(once()).method("executeQuery").with(eq(someSQL)).will(returnValue(mockResultSet.proxy()));
    mockSqlConnection.expects(once()).method("createStatement").will(returnValue(mockStatement.proxy()));
    mockResultSet.expects(atLeastOnce()).method("getMetaData").will(returnValue(mockResultSetMetaData.proxy()));
    for(int i=0; i<1; i++){
      mockResultSetMetaData.expects(once()).method("getColumnCount").will(returnValue(1));
      mockResultSetMetaData.expects(once()).method("getColumnName").will(returnValue(COL1)); 
      mockResultSet.expects(once()).method("getObject").with(eq(1)).will(returnValue(TEST_STRING)); 
    }
    
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 1);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
  }
  
  /**
   * Test method for {@link org.openadaptor.auxil.connector.jdbc.reader.JDBCReadConnector#next(long)}.
   * Initialises mock interfaces to return a result set with one column and one row.
   * Two calls to the {@link JDBCReadConnector#next(long)} method.
   * Checks value {@link JDBCReadConnector#isDry()}.
   * Ported from the legacy OldJDBCReadConnectorUnitTestCase.
   */
  public void testNextOneRow2() {
    setMocksToReturnNumberOfRows(1);
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
    mockResultSet.expects(once()).method("close"); 
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 1);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    jdbcReadConnector.next(10);
    assertTrue("Read connector not dry. Should be.", jdbcReadConnector.isDry());
  }
  
  
  /**
   * Based on {@link #testNextOneRow2()} but also sets 'preReadSql' property.
   */
  public void testNextOneRow2WithPreReadSql() {
    Mock mockPreReadSqlPreparedStatement = new Mock(PreparedStatement.class);
    String preReadSql = "UPDATE ...";
    jdbcReadConnector.setPreReadSql(preReadSql);
    
    setMocksToReturnNumberOfRows(1);
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
   
    /* Part specific to preReadSql */
    mockSqlConnection.expects(once()).method("prepareStatement").with(eq(preReadSql))
      .will(returnValue(mockPreReadSqlPreparedStatement.proxy()));
    mockPreReadSqlPreparedStatement.expects(once()).method("execute").will(returnValue(false));
    mockPreReadSqlPreparedStatement.expects(once()).method("close");
    
    mockResultSet.expects(once()).method("close"); 
   
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 1);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    jdbcReadConnector.next(10);
    assertTrue("Read connector not dry. Should be.", jdbcReadConnector.isDry());
  }
  
  
  /**
   * Based on {@link #testNextOneRow2()} but also sets 'postReadSql' property.
   */
  public void testNextOneRow2WithPostReadSql() {
    Mock mockPostReadSqlPreparedStatement = new Mock(PreparedStatement.class);
    String postReadSql = "UPDATE ...";
    jdbcReadConnector.setPostReadSql(postReadSql);
    
    setMocksToReturnNumberOfRows(1);
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
    mockResultSet.expects(once()).method("close"); 
    
    /* Part specific to postReadSql */
    mockSqlConnection.expects(once()).method("prepareStatement").with(eq(postReadSql))
      .will(returnValue(mockPostReadSqlPreparedStatement.proxy()));
    mockPostReadSqlPreparedStatement.expects(once()).method("execute").will(returnValue(false));
    mockPostReadSqlPreparedStatement.expects(once()).method("close");
    
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 1);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    jdbcReadConnector.next(10);
    assertTrue("Read connector not dry. Should be.", jdbcReadConnector.isDry());
  }
  
  
  /**
   * Test method for {@link org.openadaptor.auxil.connector.jdbc.reader.JDBCReadConnector#next(long)}.
   * Initialises mock interfaces to return a result set with one column and three rows.
   * Batch size is set to 0 (= return all records in one batch).
   * Checks value {@link JDBCReadConnector#isDry()}.
   */
  public void testNextBatchAll() {
    setMocksToReturnNumberOfRows(3);
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(true), returnValue(false)));
    mockResultSet.expects(once()).method("close");  
    jdbcReadConnector.setBatchSize(0);
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 3);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
    assertTrue("Read connector not dry. Should be.", jdbcReadConnector.isDry());
  }
  
  /**
   * Test method for {@link org.openadaptor.auxil.connector.jdbc.reader.JDBCReadConnector#next(long)}.
   * Initialises mock interfaces to return a result set with one column and three rows.
   * Batch size is set to 2 (= next() should return one batch with two elements and the second batch with one 
   * element). Checks value {@link JDBCReadConnector#isDry()}.
   */
  public void testNextBatchTwo() {
    setMocksToReturnNumberOfRows(3);
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(true), returnValue(false)));
    mockResultSet.expects(once()).method("close");  
    jdbcReadConnector.setBatchSize(2);
    jdbcReadConnector.connect();
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    Object [] arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 2);
    Map map = (Map) arr[0];
    String s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
    assertFalse("Read connector dry to soon.", jdbcReadConnector.isDry());
    arr = (Object []) jdbcReadConnector.next(10);
    assertTrue("Unexpected result type", arr[0] instanceof Map);
    assertTrue("Unexpected result count", arr.length == 1);
    map = (Map) arr[0];
    s = (String) map.get(COL1);
    assertTrue("Unexpected result", s.equals(TEST_STRING));
    assertTrue("Read connector not dry. Should be.", jdbcReadConnector.isDry());
  }
  
  private void setMocksToReturnNumberOfRows(int numRows){
    mockStatement.expects(once()).method("executeQuery").with(eq(SQL)).will(returnValue(mockResultSet.proxy()));
    mockSqlConnection.expects(once()).method("createStatement").will(returnValue(mockStatement.proxy()));
    mockResultSet.expects(atLeastOnce()).method("getMetaData").will(returnValue(mockResultSetMetaData.proxy()));
    for(int i=0; i<numRows; i++){
      mockResultSetMetaData.expects(once()).method("getColumnCount").will(returnValue(1));
      mockResultSetMetaData.expects(once()).method("getColumnName").will(returnValue(COL1)); 
      mockResultSet.expects(once()).method("getObject").with(eq(1)).will(returnValue(TEST_STRING)); 
    }
  }
  
  /**
   * Test method for {@link JDBCReadConnector#next(long)}.
   * Ported from the legacy JDBCEventReadConnector unit tests.
   * Stored procedure that polls returns nothing (no new events).
   */
  public void testNextEventDriven1() {
    DBEventDrivenPollingReadConnector pollingReadConnector = new DBEventDrivenPollingReadConnector();
    pollingReadConnector.setEventServiceID("10");
    pollingReadConnector.setEventTypeID("20");
    pollingReadConnector.setJdbcConnection(mockConnection);
    pollingReadConnector.setDelegate(jdbcReadConnector);
    
    Mock mockPollStatement =  new Mock(CallableStatement.class);
    connectDBEventDrivenConnector(mockPollStatement, pollingReadConnector);
 
    /* 
     * actual call to next. executeQuery returns a result set that is immediately closed
     * (because of an SQLException thrown when no results are found..).
     */
    mockPollStatement.expects(once()).method("executeQuery").will(returnValue(mockResultSet.proxy()));
    mockPollStatement.expects(once()).method("close");  
    mockResultSet.expects(once()).method("getMetaData").will(returnValue(mockResultSetMetaData.proxy()));
    mockResultSet.expects(once()).method("next").will(returnValue(false));
    mockResultSet.expects(once()).method("close");
    
    assertFalse("Read connector dry to soon.", pollingReadConnector.isDry());
    assertNull(pollingReadConnector.next(10));  
  }
  
  /**
   * Test method for {@link JDBCReadConnector#next(long)}.
   * Ported from the legacy JDBCEventReadConnector unit tests.
   * Stored procesure that polls returns one new event. Query constructed based on this
   * event returns an empty result set.
   */
  public void testNextEventDriven2(){
    DBEventDrivenPollingReadConnector pollingReadConnector = new DBEventDrivenPollingReadConnector();
    pollingReadConnector.setEventServiceID("10");
    pollingReadConnector.setEventTypeID("20");
    pollingReadConnector.setJdbcConnection(mockConnection);
    pollingReadConnector.setDelegate(jdbcReadConnector);
    
    Mock mockPollStatement =  new Mock(CallableStatement.class);
    connectDBEventDrivenConnector(mockPollStatement, pollingReadConnector);
    
    /* actual call to next */
    mockPollStatement.expects(once()).method("executeQuery").will(returnValue(mockResultSet.proxy()));    
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false), returnValue(false)));
    mockResultSet.expects(atLeastOnce()).method("getMetaData").will(returnValue(mockResultSetMetaData.proxy()));
    mockResultSetMetaData.expects(atLeastOnce()).method("getColumnCount").will(returnValue(15));
    for(int i=1; i<=15; i++){
      mockResultSetMetaData.expects(once()).method("getColumnName").with(eq(i)).will(returnValue("COL" + new Integer(i)));
    }
    mockResultSet.expects(atLeastOnce()).method("getObject").will(returnValue("TEST"));
    mockResultSet.expects(once()).method("close");
    
    Mock mockActualStatement =  new Mock(CallableStatement.class);
    mockSqlConnection.expects(once()).method("prepareCall").with(eq("{ call TEST (?,?,?,?,?,?,?,?,?,?)}")).will(returnValue(mockActualStatement.proxy()));
    mockActualStatement.expects(atLeastOnce()).method("setString");
    mockActualStatement.expects(once()).method("executeQuery").will(returnValue(mockResultSet.proxy()));
    mockResultSet.expects(once()).method("close");
    mockActualStatement.expects(once()).method("close");   
    
    assertFalse("Read connector dry to soon.", pollingReadConnector.isDry());
    Object [] arr = (Object []) pollingReadConnector.next(10);
    assertTrue("Unexpected result count", arr.length == 0);
  }
  
  /**
   * Test method for {@link JDBCReadConnector#next(long)}.
   * Ported from the legacy JDBCEventReadConnector unit tests.
   * Stored procesure that polls returns one new event. Query constructed based on this
   * event returns a result set with one row.
   */
  public void testNextEventDriven3() {
    DBEventDrivenPollingReadConnector pollingReadConnector = new DBEventDrivenPollingReadConnector();
    pollingReadConnector.setEventServiceID("10");
    pollingReadConnector.setEventTypeID("20");
    pollingReadConnector.setJdbcConnection(mockConnection);
    pollingReadConnector.setDelegate(jdbcReadConnector);
    
    Mock mockPollStatement =  new Mock(CallableStatement.class);
    Mock mockResultSet2 = new Mock(ResultSet.class);
    Mock mockResultSetMetaData2 = new Mock(ResultSetMetaData.class);
    connectDBEventDrivenConnector(mockPollStatement, pollingReadConnector);
 
    /* actual call to next */
    mockPollStatement.expects(once()).method("executeQuery").will(returnValue(mockResultSet.proxy()));
    
    mockResultSet.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
    mockResultSet.expects(once()).method("getMetaData").will(returnValue(mockResultSetMetaData.proxy()));
    mockResultSetMetaData.expects(atLeastOnce()).method("getColumnCount").will(returnValue(15));
    for(int i=1; i<=15; i++){
      mockResultSetMetaData.expects(once()).method("getColumnName").with(eq(i)).will(returnValue("COL" + new Integer(i)));
    }
    mockResultSet.expects(atLeastOnce()).method("getObject").will(returnValue("TEST"));

    Mock mockActualStatement =  new Mock(CallableStatement.class);
    mockSqlConnection.expects(once()).method("prepareCall").with(eq("{ call TEST (?,?,?,?,?,?,?,?,?,?)}")).will(returnValue(mockActualStatement.proxy()));
    mockActualStatement.expects(atLeastOnce()).method("setString");
  
    mockActualStatement.expects(once()).method("executeQuery").will(returnValue(mockResultSet2.proxy()));
    mockResultSet2.expects(atLeastOnce()).method("next").will(onConsecutiveCalls(returnValue(true), returnValue(false)));
    mockResultSet2.expects(once()).method("getMetaData").will(returnValue(mockResultSetMetaData2.proxy()));
    mockResultSet2.expects(atLeastOnce()).method("getObject").will(returnValue("TEST2"));

    mockResultSetMetaData2.expects(once()).method("getColumnCount").will(returnValue(1));
    mockResultSetMetaData2.expects(once()).method("getColumnName").with(eq(1)).will(returnValue("COL1"));

    mockResultSet.expects(once()).method("close");
    mockActualStatement.expects(once()).method("close"); 
    
    assertFalse("Read connector dry to soon.", pollingReadConnector.isDry());
    Object [] arr = (Object []) pollingReadConnector.next(10);
    assertTrue("Unexpected result count", arr.length == 1);
  }
  
  /**
   * Test for {@link JDBCReadConnector#next(org.openadaptor.auxil.orderedmap.IOrderedMap, long)}
   */
  public void testSetParametersForQuery1(){
    IOrderedMap inputParams = new OrderedHashMap();
    inputParams.put("param1", "1");
    String sql = "SELECT a FROM TABLE1 WHERE a=?";
    jdbcReadConnector.setSql(sql);
    jdbcReadConnector.setParametersForQuery(inputParams);
    assertEquals(jdbcReadConnector.sql, sql);
    assertEquals(jdbcReadConnector.postSubstitutionSql, "SELECT a FROM TABLE1 WHERE a=1");
  }
  
  /**
   * Test for {@link JDBCReadConnector#next(org.openadaptor.auxil.orderedmap.IOrderedMap, long)}
   * No placeholders in the query.
   */
  public void testSetParametersForQuery2(){
    IOrderedMap inputParams = new OrderedHashMap();
    inputParams.put("param1", "1");
    String sql = "SELECT a FROM TABLE1 WHERE a=10";
    jdbcReadConnector.setSql(sql);
    jdbcReadConnector.setParametersForQuery(inputParams);
    assertEquals(jdbcReadConnector.sql, sql);
    assertNull(jdbcReadConnector.postSubstitutionSql);
  }
  
  /**
   * Test for {@link JDBCReadConnector#next(org.openadaptor.auxil.orderedmap.IOrderedMap, long)}
   * More placeholders than params.
   */
  public void testSetParametersForQuery3(){
    IOrderedMap inputParams = new OrderedHashMap();
    inputParams.put("param1", "1");
    String sql = "SELECT a FROM TABLE1 WHERE a=? AND b=?";
    jdbcReadConnector.setSql(sql);
    jdbcReadConnector.setParametersForQuery(inputParams);
    assertEquals(jdbcReadConnector.sql, sql);
    assertEquals(jdbcReadConnector.postSubstitutionSql, "SELECT a FROM TABLE1 WHERE a=1 AND b=?");
  }
  
  /**
   * Test for {@link JDBCReadConnector#next(org.openadaptor.auxil.orderedmap.IOrderedMap, long)}
   * More placeholders than params.
   */
  public void testSetParametersForQuery4(){
    IOrderedMap inputParams = new OrderedHashMap();
    inputParams.put("param1", "1");
    inputParams.put("param2", "2");
    String sql = "SELECT a FROM TABLE1 WHERE a=? AND b=?";
    jdbcReadConnector.setSql(sql);
    jdbcReadConnector.setParametersForQuery(inputParams);
    assertEquals(jdbcReadConnector.sql, sql);
    assertEquals(jdbcReadConnector.postSubstitutionSql, "SELECT a FROM TABLE1 WHERE a=1 AND b=2");
  }
  
  /**
   * Test for {@link JDBCReadConnector#next(org.openadaptor.auxil.orderedmap.IOrderedMap, long)}
   * Null input params;
   */
  public void testsetParametersForQuery5(){
    String sql = "SELECT a FROM TABLE1 WHERE a=?";
    jdbcReadConnector.setSql(sql);
    jdbcReadConnector.setParametersForQuery(null);
    assertEquals(jdbcReadConnector.sql, sql);
    assertEquals(jdbcReadConnector.postSubstitutionSql, null);
  }
  
  private void connectDBEventDrivenConnector(Mock mockStatement, IReadConnector readConnector){
    /* Callable statement for the pollingReadConnector */
    mockSqlConnection.expects(once()).method("prepareCall").will(returnValue(mockStatement.proxy()));
    
    /* 'Plain' statement for the underlying connector */
    //mockSqlConnection.expects(once()).method("createStatement");
    
    mockStatement.expects(once()).method("registerOutParameter").with(eq(1), eq(java.sql.Types.INTEGER));
    mockStatement.expects(once()).method("setInt").with(eq(2), eq(10));
    mockStatement.expects(once()).method("setInt").with(eq(3), eq(20));    
    readConnector.connect();
  }
}
