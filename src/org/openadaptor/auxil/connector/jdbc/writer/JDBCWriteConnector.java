/*
 Copyright (C) 2001 - 2010 The Software Conservancy as Trustee. All rights reserved.

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

package org.openadaptor.auxil.connector.jdbc.writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.auxil.connector.jdbc.AbstractJDBCConnector;
import org.openadaptor.auxil.connector.jdbc.JDBCConnection;
import org.openadaptor.core.IWriteConnector;
import org.openadaptor.core.exception.ConnectionException;
import org.openadaptor.core.exception.OAException;
import org.openadaptor.core.exception.ValidationException;
import org.openadaptor.core.transaction.ITransactional;

import java.sql.SQLException;
import java.util.List;

/**
 * This connector writes output records to JDBC databases.
 * <br>
 * It delegates the actual writing to ISQLWriter instances.
 *
 *
 * @author higginse
 *
 * @see JDBCConnection
 */
public class JDBCWriteConnector extends AbstractJDBCConnector implements IWriteConnector, ITransactional {
  
  private static final Log log = LogFactory.getLog(JDBCWriteConnector.class);

  //Provide a default writer delegate.
  private ISQLWriter sqlWriter;// = new RawSQLWriter();

  private boolean connected;

  public JDBCWriteConnector() {
    super();
  }

  public JDBCWriteConnector(String id) {
    super(id);
  }

  /**
   * Delegate which will actually perform the writes.
   * <br>
   * Optional - defaults to an instance of SQLWriter()
   * <br>
   * This delegate will be used to write each batch of records received
   * by this connector.
   *
   * @param sqlWriter {@link ISQLWriter} implementation which converts the incoming data into a PreparedStatement.
   */
  public void setWriter(final ISQLWriter sqlWriter) {
    this.sqlWriter = sqlWriter;
  }

  /**
   * Checks that all mandatory properties have been set. Calls validate() on the statement
   * converter to ensure that it's properties are correctly set as well. 
   *
   * @param exceptions list of exceptions that any validation errors will be appended to
   */
  public void validate(List exceptions) {
    if ( jdbcConnection == null ) {
      exceptions.add(new ValidationException("jdbcConnection property not set", this));
    } else {
      jdbcConnection.validate(exceptions);
    }
    //If an sqlWriter has not been configured, use RawSQLWriter as a default.
    if (sqlWriter==null) {
      sqlWriter=new RawSQLWriter();
      log.info("sqlWriter not configured (defaulting to "+sqlWriter.getClass().getName()+")");
    }
    sqlWriter.validate(exceptions, this);
  }

  /**
   * Write incoming array of records to a database.
   * It will use the configured ISQLWriter delegate to perform the
   * writes.
   *
   * Access to a JDBC Connection needs to be synchronized, as some JDBC drivers, such 
   * as Sybase simply interrupt execution of a thread when another thread starts
   * running a statement on the same connection. Oracle should not need the synchronisation
   * here as apparently it is already implemented in the JDBC driver.
   * 
   * @param data Object[] of records to be written.
   *
   * @return null
   *
   * @throws OAException just a wrapper around any SQLExceptions that may be thrown
   * or if the jdbcConnection details have not been set
   */
  public synchronized Object deliver(Object[] data) throws OAException {
    try {   
      sqlWriter.writeBatch(data);
    } catch (SQLException e) {
      jdbcConnection.handleException(e, null);
    }
    return null;
  }


  /**
   * Creates a connection to the database. 
   * Reuses existing connection if one already exists. 
   *
   * @throws ConnectionException just a wrapper around a SQLException or thrown if the
   * jdbcConnection property has not been set
   */
  public void connect() throws ConnectionException {
    log.debug("Connector: [" + getId() + "] connecting ....");

    if (!jdbcConnection.isConnected() ) {
      try {
        jdbcConnection.connect();
      } catch (SQLException e) {
        jdbcConnection.handleException(e, "Failed to establish JDBC connection");
      }
      //Execute preamble sql if it exists...
      if (afterConnectSql!=null) {
        log.info("Executing preamble SQL: "+afterConnectSql);
        executePrePostambleSQL(afterConnectSql, jdbcConnection.getConnection());
      }
    }

    /* 
     * checking if sqlWriter is null before initialising as subclasses 
     * may choose not to use an sqlWriter. 
     */
    if(sqlWriter!=null){
      sqlWriter.initialise(jdbcConnection.getConnection());
    }
    else{
      log.warn("sqlWriter not set and could not be initialised");
    }
    
    connected = true;
    log.info("Connector: [" + getId() + "] successfully connected.");
  }

  
  /**
   * Closes the connection to the database.
   *
   * @throws ConnectionException just a wrapper around any SQLexception that may be thrown
   */
  public void disconnect() throws ConnectionException {
    log.debug("Connector: [" + getId() + "] disconnecting ....");

    if ( jdbcConnection == null ) {
      log.info("Connection already closed");
      return;
    }

    //Execute postamble sql if it exists...
    if (beforeDisconnectSql!=null) {
      log.info("Executing postamble SQL: "+beforeDisconnectSql);
      executePrePostambleSQL(beforeDisconnectSql, jdbcConnection.getConnection());
    }

    try {
      jdbcConnection.disconnect();
    } catch (SQLException e) {
      jdbcConnection.handleException(e, "Failed to disconnect JDBC connection");
    }
    clearInternalState();
    connected = false;
    log.info("Connector: [" + getId() + "] disconnected");
  }


  /**
   * @return the transaction resource if the connection is transacted or null otherwise
   */
  public Object getResource() {
    if ( jdbcConnection != null && jdbcConnection.isTransacted()) {
      return jdbcConnection.getTransactionalResource();
    }

    return null;
  }
  
  public boolean isConnected() {
    return connected;
  }

  /**
   * Clear internal state variables where necessary.
   * This is necessary in case connector is restarted
   * This may happen when using RunConfigs, or posssibly other embedded mechanisms.
   * This relates to [SC105]
   */
  protected void clearInternalState() {
  	log.debug("Clearing internal state for connector");
  	sqlWriter=null;
  }

}
