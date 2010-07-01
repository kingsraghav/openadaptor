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

package org.openadaptor.core.exception;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.auxil.connector.jdbc.JDBCConnection;
import org.openadaptor.auxil.connector.jdbc.AbstractJDBCConnectionTests;
import org.openadaptor.auxil.connector.jdbc.reader.JDBCReadConnector;
import org.openadaptor.auxil.connector.jdbc.reader.orderedmap.ResultSetToOrderedMapConverter;
import org.openadaptor.core.adaptor.Adaptor;
import org.openadaptor.core.router.Router;
import org.openadaptor.spring.SpringAdaptor;
import org.openadaptor.util.LocalHSQLJdbcConnection;
import org.openadaptor.util.SystemTestUtil;
import org.openadaptor.util.TestComponent;


/**
 * System tests for the hospital writer and reader. 
 * Runs adaptor in different configurations and different nodes throwing exceptions that get written
 * to the hospital. Verifies the hospital holds expected data.
 * Uses in-memory HSQL database for the hospital.
 * 
 * @author Kris Lachor
 */
public class DbBackedExceptionHandlingSystemTestCase extends AbstractJDBCConnectionTests {
  private static String SCHEMA = "CREATE MEMORY TABLE OA_Exception(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TIMESTAMP CHAR(32),EXCEPTION_CLASS_NAME CHAR(128),ORIGINATING_COMPONENT CHAR(128),DATA CHAR(256),FIXED BOOLEAN,REPROCESSED BOOLEAN)";
                                  
  private static String SELECT_ALL_ERRORS_SQL = "SELECT * FROM OA_Exception";
  
  //private static String SELECT_ALL_REPROCESSED_SQL = SELECT_ALL_ERRORS_SQL + " WHERE REPROCESSED=true";
  
  private static String FIX_ALL_RECORDS_SQL = "UPDATE OA_Exception SET FIXED='true'";
  
  private static String HOSPITAL_READER_SELECT_STMT = "SELECT id, timestamp, data, exception_class_name, originating_component FROM OA_Exception WHERE fixed = 'true' AND REPROCESSED = 'false' ";
  
  private static final String RESOURCE_LOCATION = "test/system/src/";
  
  private static final String HOSPITAL_WRITER_1 = "exception_db_writer.xml";
  
  private static final String HOSPITAL_WRITER_2 = "exception_db_writer2.xml";
  
  //private static final String HOSPITAL_WRITER_3 = "exception_db_writer3.xml";
  
  private static final String HOSPITAL_WRITER_4 = "exception_db_writer4.xml";
  
  private static final String HOSPITAL_READER = "exception_db_reader.xml";
 
  TestComponent testComponent = new TestComponent();
  
  /**
   * @return hospital table definition.
   */
  public String getSchemaDefinition() {
    return SCHEMA;
  }

  /**
   * Ensures the connection to the hospital is working, that the hostpital schema is correctly 
   * set up and empty. Unless this condition is met this test case and other test cases in this
   * class will fail.
   */
  public void testHospitalIsEmpty1() throws Exception{
    verifyHospitalIsEmpty();
  }
  
  /**
   * Runs adaptor (with the hospital as exceptionProcessor) with nodes that should not throw any exceptions.
   * Ensures the hospital is empty. 
   */
  public void testHospitalIsEmpty2() throws Exception{
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_1);
    verifyHospitalIsEmpty();
  }

  /**
   * Runs adaptor with a writer node that throws an exception. 
   * For the writer uses {@link ExceptionThrowingWriteConnector}.
   * Verifies hospital has one entry.
   */
  public void testHospitalWriterGetsOneExceptionFromWriteConnector() throws Exception{
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_2);
    verifyHospitalHasOneEntry();
  }
  
// Readers are unable to do any exception handling at the moment. 
// Commented out until exception handling in a read node is fixed.   
//  
//  /**
//   * Runs adaptor with a reader node that throws an exception. 
//   * Verifies hospital has one entry.
//   */
//  public void testHospitalWriterGetsOneExceptionFromReadConnector() throws Exception{
//    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_3);
//    verifyHospitalHasOneEntry();
//  }
  
  /**
   * Runs adaptor with a data processor node that throws an exception. 
   * Uses {@link ExceptionThrowingDataProcessor} for the data processor.
   * Verifies hospital has one entry.
   */
  public void testHospitalWriterGetsOneExceptionFromDataProcessor() throws Exception{
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_4);
    verifyHospitalHasOneEntry();
  }
  
  /**
   * Runs adaptor (two times) with a node that throws an exception. 
   * Verifies the hostpital has two exceptions, verifies corect values of its some data.
   */
  public void testHospitalWriterGetsTwoExceptions() throws Exception{
    SpringAdaptor adaptor = SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_2);
    SystemTestUtil.adaptorRun(adaptor);
    PreparedStatement preparedStmt = jdbcConnection.getConnection().prepareStatement(SELECT_ALL_ERRORS_SQL);
    ResultSet rs = preparedStmt.executeQuery();
    assertTrue("Hospital is empty, expected two elements.", rs.next());
    assertTrue("Hospital has one elements, expected two. ", rs.next());
    String exceptionClassName = rs.getString("EXCEPTION_CLASS_NAME");
    assertTrue(exceptionClassName!=null);
    assertEquals(exceptionClassName, new RuntimeException().getClass().getName());
    String data = rs.getString("DATA");
    assertTrue(data!=null);
    assertEquals(data, "Dummy read connector test data");
    assertFalse("Hospital has more than two elements", rs.next());
    preparedStmt.close();
  }
  
  
  /**
   * Runs adaptor that creates one entry in the hospital, then adaptor that reads from the hospital. 
   */
  public void testHospitalReader1() throws Exception{
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_2);
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_READER);
  }
  
  
  /** 
   * Runs adaptor that creates one entry in the hospital, then the hospital reader. 
   * Verifies that reader reads no elements (as they need to be 'fixed' first).
   */
  public void testHospitalReader2() throws Exception{
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_2);
    JDBCReadConnector hospitalReader = assembleHostpitalReader();
    TestComponent.TestWriteConnector writer = new TestComponent.TestWriteConnector();
    Map processMap = new HashMap();  
    processMap.put(hospitalReader, writer);
    assertTrue(writer.counter == 0);
    Adaptor adaptor = assembleAdaptor(processMap);
    SystemTestUtil.adaptorRun(adaptor);
    /* No record should be read from the hospital as the FIXED flat is still set to false. */
    assertTrue(writer.counter == 0);
  }
  
  /** 
   * Runs adaptor that creates one entry in the hospital. 'Fixes' the records in the hospital. 
   * Runs the hospital reader, verifies that reader read one element.
   */
  public void testHospitalReader3() throws Exception{
    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_2);
    fixAllRecordsInHospital();
    JDBCReadConnector hospitalReader = assembleHostpitalReader();
    TestComponent.TestWriteConnector writer = new TestComponent.TestWriteConnector();
    Map processMap = new HashMap();  
    processMap.put(hospitalReader, writer);
    assertTrue(writer.counter == 0);
    Adaptor adaptor = assembleAdaptor(processMap);
    SystemTestUtil.adaptorRun(adaptor);    
    assertTrue(writer.counter == 1);
  }
  
//  No way to update the REPROCESSED flag with the current JDBC readers (after reading
//  all data from the hospital). 
//  Commented out until functionality implemented.
//  
//  /** 
//   * Runs adaptor that creates one entry in the hospital. 'Fixes' the records in the hospital. 
//   * Runs the hospital reader, verifies that reader read one element (until now identical to testHospitalReader3()
//   * although executed in a slightly different way).
//   * Then verifies that the 'reprocessed' flag has been switched to true.
//   */
//  public void testHospitalReader4() throws Exception{
//    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_WRITER_2);
//    fixAllRecordsInHospital();
//    SystemTestUtil.runAdaptor(this, RESOURCE_LOCATION, HOSPITAL_READER);
//    PreparedStatement preparedStmt = jdbcConnection.getConnection().prepareStatement(SELECT_ALL_REPROCESSED_SQL);
//    ResultSet rs = preparedStmt.executeQuery();
//    assertTrue("An entry read by the hospital reader hasn't been makred as reprocessed.", rs.next());
//    assertFalse("Expected only one element, found two.", rs.next());
//    preparedStmt.close();
//  }
  
  private JDBCReadConnector assembleHostpitalReader(){
    JDBCConnection jdbcConnection = new LocalHSQLJdbcConnection();
    JDBCReadConnector hospitalReader = new JDBCReadConnector();
    ResultSetToOrderedMapConverter resultSetConverter = new ResultSetToOrderedMapConverter();
    hospitalReader.setResultSetConverter(resultSetConverter);
    hospitalReader.setJdbcConnection(jdbcConnection);
    hospitalReader.setSql(HOSPITAL_READER_SELECT_STMT);
    return hospitalReader;
  }
  
  private Adaptor assembleAdaptor(Map processMap){
    Router router = new Router(); 
    Adaptor adaptor = new Adaptor();
    adaptor.setMessageProcessor(router);
    router.setProcessMap(processMap);
    return adaptor;
  }
  
  private void verifyHospitalIsEmpty() throws Exception{
    PreparedStatement preparedStmt = jdbcConnection.getConnection().prepareStatement(SELECT_ALL_ERRORS_SQL);
    ResultSet rs = preparedStmt.executeQuery();
    assertFalse("Hospital not empty", rs.next());
    ResultSetMetaData rsmd = rs.getMetaData();
    int numberOfColumns = rsmd.getColumnCount();
    assertTrue("Hospital returned wrong number of columns", numberOfColumns > 5);
    preparedStmt.close();
  }
  
  private void verifyHospitalHasOneEntry() throws Exception{
    PreparedStatement preparedStmt = jdbcConnection.getConnection().prepareStatement(SELECT_ALL_ERRORS_SQL);
    ResultSet rs = preparedStmt.executeQuery();
    assertTrue("Hospital is empty", rs.next());
    assertFalse("Hospital has more than one element", rs.next());
    preparedStmt.close();
  }
  
  private void fixAllRecordsInHospital() throws Exception{
    PreparedStatement preparedStmt = jdbcConnection.getConnection().prepareStatement(FIX_ALL_RECORDS_SQL);
    preparedStmt.executeUpdate();
    preparedStmt.close();
  }
  
}
