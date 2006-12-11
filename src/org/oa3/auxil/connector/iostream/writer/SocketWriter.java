/*
 * [[
 * Copyright (C) 2001 - 2006 The Software Conservancy as Trustee. All rights
 * reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to
 * trademarks, copyrights, patents, trade secrets or any other intellectual
 * property of the licensor or any contributor except as expressly stated
 * herein. No patent license is granted separate from the Software, for
 * code that you delete from the Software, or for combinations of the
 * Software with other software or hardware.
 * ]]
 */
package org.oa3.auxil.connector.iostream.writer;

/*
 * File: $Header: /cvs/oa3/src/org/oa3/connector/stream/writer/SocketWriter.java,v 1.1 2006/02/24 09:34:52 higginse Exp $
 * Rev: $Revision: 1.1 $ Created Feb 23, 2006 by Eddy Higgins
 */
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oa3.core.exception.OAException;

/**
 * @author OA3 Core Team
 */
public class SocketWriter extends AbstractStreamWriter {
  
  private static final Log log = LogFactory.getLog(SocketWriter.class);

  private String hostname;

  private int port;

  private Socket socket;

  // BEGIN Bean getters/setters

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getHostname() {
    return hostname;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  // END Bean getters/setters

  /**
   * Establish a Socket Connection
   * 
   * @throws org.oa3.control.OAException
   */

  public void connect() throws OAException {
    log.debug("Opening socket connection to host:port " + hostname + ":" + port);
    try {
      socket = new Socket(hostname, port);
      outputStream = socket.getOutputStream();
      super.connect();
      ;
    } catch (UnknownHostException uhe) {
      log.error("Unknown host - " + hostname + "." + uhe.toString());
      throw new OAException("Unknown host - " + hostname, uhe);
    }

    catch (IOException ioe) {
      log.error("IOException - " + ioe.toString());
      throw new OAException("IOException: " + hostname, ioe);
    }
  }

  /**
   * Disconnect from the external message transport. If already disconnected then do nothing.
   * 
   * @throws org.oa3.control.OAException
   */
  public void disconnect() {
    log.debug("Disconnecting from host:port " + hostname + ":" + port);
    super.disconnect();
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException ioe) {
        log.warn("Failed to close socket - " + ioe.toString());
      }
    }
  }
}