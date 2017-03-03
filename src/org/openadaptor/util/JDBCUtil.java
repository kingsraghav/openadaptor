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

package org.openadaptor.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JDBCUtil {
  private static final Log log = LogFactory.getLog(JDBCUtil.class);

  public static void closeNoThrow(Statement s) {
    if (s != null) {
      try {
        s.close();
      } catch (SQLException e) {
      	if (log.isDebugEnabled()){
      		log.debug("Ignoring exception "+e.getClass().getName()+" on close() -" +e.getMessage());
      	}
      }
    }
  }

  public static void closeNoThrow(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
      	if (log.isDebugEnabled()){
      		log.debug("Ignoring exception "+e.getClass().getName()+" on close() -" +e.getMessage());
      	}
      }
    }
  }

  public static void logCurrentResultSetRow(Log log, String msg, ResultSet rs) throws SQLException {
    if (log.isDebugEnabled()) {
      ResultSetMetaData rsmd = rs.getMetaData();
      if (msg != null) {
        log.debug(msg);
      }
      for (int i = 1; i <= rsmd.getColumnCount(); i++) {
        log.debug("  " + rsmd.getColumnName(i) + " (" + rsmd.getColumnClassName(i) + ") = " + rs.getString(i));
      }
    }
  }


}
