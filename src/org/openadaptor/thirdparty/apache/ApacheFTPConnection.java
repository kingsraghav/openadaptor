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

package org.openadaptor.thirdparty.apache;

/*
 * File: $Header$ Rev: $Revision$
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.openadaptor.auxil.connector.ftp.AbstractFTPLibrary;
import org.openadaptor.auxil.connector.ftp.IFTPConnection;
import org.openadaptor.core.exception.ConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This component will provide basic File Transfer Protocol (FTP) connunication to allow the adaptor to GET a file from
 * or PUT a file onto a remote machine. <p/>
 * 
 * The methods return FTP input/output streams to the caller which can then be used to either read a file from or write
 * a file to the remote server. <p/>
 * 
 * Uses the org.apache.commons.net.ftp classes to perform the actual file transfer. <p/>
 * 
 * Based on the original ideas by Pablo Bawdekar
 * 
 * @author Russ Fennell
 * @see IFTPConnection
 * @see AbstractFTPLibrary
 */
public class ApacheFTPConnection extends AbstractFTPLibrary {
  
  private static final Log log = LogFactory.getLog(ApacheFTPConnection.class);

  private FTPClient _ftpClient;

  private boolean _loggedIn = false;

  /**
   * Method is deprecated after switch to commons-net 3.x
   */
  @Deprecated
  public void setUseReaderThread(boolean b) {
  }

  /**
   * Method is deprecated after switch to commons-net 3.x
   * @return always returns false
   */
  @Deprecated
  public boolean isUseReaderThread() {
    return false;
  }

  /**
   * Takes the supplied hostname and port of the target machine and attempts to connect to it. If successful the
   * isConnected() flag is set. Sets the file transfer mode.
   * 
   * @throws ConnectionException
   *           if we fail to connect to the remote server or we fail to set BINARY mode (if required)
   */
  public void connect() throws ConnectionException {
    log.debug("Connecting to " + hostName + " on port " + port);

    try {
      _ftpClient = new FTPClient();

      _ftpClient.connect(hostName, port);

      // check to see that the connection went through ok
      if (!FTPReply.isPositiveCompletion(_ftpClient.getReplyCode())) {
        _ftpClient.disconnect();
        throw new Exception("Failed to connect");
      }

      // set the file transfer mode
      try {
        if (binaryTransfer) {
          _ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
          log.debug("Binary transfer mode set");
        } else {
          _ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
          log.debug("ASCII transfer mode set");
        }
      } catch (IOException e) {
        throw new ConnectionException("Failed to set " + (binaryTransfer ? "binary" : "ascii") + " transfer mode: "
            + e.getMessage(), this);
      }

      log.debug("ApacheFTPLibrary object initialised and connected to " + hostName);
    } catch (Exception e) {
      close();
      throw new ConnectionException("Failed to initialise the ApacheFTPLibrary object: " + e.getMessage(), this);
    }
  }

  /**
   * @return boolean to indicate if the FTPClient object has successfully connected to the remote server
   */
  public boolean isConnected() {
    return _ftpClient.isConnected();
  }

  /**
   * @return boolean to indicate if the FTPClient object has successfully logged into the remote server
   */
  public boolean isLoggedIn() {
    return isConnected() && _loggedIn;
  }

  /**
   * Attempt to log into the remote server using the supplied credentials. This method checks to make saure that the
   * client has been successfully connected to the remote server before attempting to log in.
   * 
   * @throws ConnectionException
   *           if we failt o log into the remote server
   */
  public void logon() throws ConnectionException {
    log.debug("Logging in " + userName);

    try {
      if (!_ftpClient.login(userName, password)) {
        _ftpClient.disconnect();
        log.fatal(userName + " Failed to login");
      }

      _loggedIn = true;
      log.debug(userName + " logged in");

    } catch (Exception e) {
      close();
      throw new ConnectionException("Failed to login to FTP Server:" + e, this);
    }
  }

  /**
   * This method creates a FTP input stream in the shape of an InputStreamReader that the caller can use to perform the
   * GET function. Assumes that the FTP client has already been successfully connected to and authenticated. Although,
   * you really shouldn't be able to get this far without this being the case anyway! <p/>
   * 
   * Due to the way that the Apache libraries work we have to download the file, use the completePendingCommand() to
   * finish the transfer process. We then read the file back in and pass it as an InputSreamReader to the caller.
   * 
   * @param fileName -
   *          The name of the file to retrieve
   * 
   * @return An InputStreamReader object containing the file contents
   * 
   * @throws ConnectionException
   *           if we cannot open the remote stream
   */
  public InputStream get(String fileName) throws ConnectionException {

    InputStream in;

    log.debug("get() called: directory=" + getCurrentWorkingDirectory() + "; file=" + fileName);

    checkLoggedIn();

    try {
      in = _ftpClient.retrieveFileStream(fileName);

      if (in == null) {
        log.warn("Error retrieving file: " + _ftpClient.getReplyString());
        return null;
      }
    } catch (IOException e) {
      close();
      throw new ConnectionException("Cannot open FTP stream:" + e.toString(), this);
    }

    return in;
  }

  /**
   * This method creates a FTP output stream in the shape of an OutputStreamWriter that the caller can use to perform
   * the PUT function. Assumes that the FTP client has already been successfully connected to and authenticated.
   * Although, you really shouldn't be able to get this far without this being the case anyway! <p/>
   * 
   * If the createDirectory flag is set then the upload directory will be created if it is not present on the remote
   * server
   * 
   * @param fileName -
   *          The name of the file to transfer
   * 
   * @return OutputStreamWriter that the caller can use to write the file
   * 
   * @throws ConnectionException -
   *           if the client is not conected and logged into the remote server or the FTP output stream cannot be
   *           created (eg. does not have permission)
   */
  public OutputStream put(String fileName) throws ConnectionException {
    log.debug("put() called: directory=" + getCurrentWorkingDirectory() + "; file=" + fileName);
    OutputStream out;

    checkLoggedIn();

    try {
      log.debug("Creating transfer stream");
      out = _ftpClient.storeFileStream(fileName);

      if (out == null) {
        log.warn("Error creating file: " + _ftpClient.getReplyString());
        return null;
      }
      log.debug("File transfer stream created using FTP PUT for " + fileName);
    } catch (IOException e) {
      close();
      throw new ConnectionException("Cannot open FTP stream:" + e.getMessage(), this);
    }

    return out;
  }

  /**
   * Transfers a file to the remote server but appends the output stream the remote file rather than overwriting it.
   * <p/>
   * 
   * If the createDirectory flag is set then the upload directory will be created if it is not present on the remote
   * server
   * 
   * @param fileName -
   *          The name of the file to transfer
   * 
   * @return OutputStreamWriter that the caller can use to write the file
   * 
   * @throws ConnectionException -
   *           if the client is not conected and logged into the remote server or the FTP output stream cannot be
   *           created (eg. does not have permission)
   */
  public OutputStream append(String fileName) throws ConnectionException {
    log.debug("append() called: directory=" + getCurrentWorkingDirectory() + "; file=" + fileName);
    OutputStream out;

    checkLoggedIn();

    try {
      log.debug("Creating transfer stream");
      out = _ftpClient.appendFileStream(fileName);

      if (out == null) {
        log.warn("Error appending to file: " + _ftpClient.getReplyString());
        return null;
      }
      log.debug("File transfer stream created using FTP APPEND for " + fileName);
    } catch (IOException e) {
      close();
      throw new ConnectionException("Cannot open FTP stream:" + e.getMessage(), this);
    }

    return out;
  }

  /**
   * Close the connection to the remote server
   * 
   * @throws ConnectionException
   *           if the connection fails to close
   */
  public void close() throws ConnectionException {
    try {
      if (_ftpClient.isConnected()) {
        // You must logout as a deadlock can occur in some VM's (if the
        // system line seperator is not set to "\r\n")
        _ftpClient.logout();
        _ftpClient.disconnect();
        log.debug("FTP Connection closed");
      }
    } catch (Exception e) {
      throw new ConnectionException("Failed to close FTP Server" + e.toString(), this);
    }
  }

  /**
   * Check for directory on remote server. Assumes that the client is already connected and logged into the remote
   * server
   * 
   * @param dirName -
   *          the directory to check for
   * 
   * @return boolean to indicate the presence of the directory
   * 
   * @throws ConnectionException -
   *           if the client is not connected and logged into the remote server
   */
  public boolean directoryExists(String dirName) throws ConnectionException {
    boolean _isPresent = false;

    checkLoggedIn();

    // attempt to change to the supplied directory. Throws an exception or
    // returns false if the directory does not exist
    try {
      String cwd = _ftpClient.printWorkingDirectory();

      _isPresent = _ftpClient.changeWorkingDirectory(dirName);

      if (cwd != null)
        _ftpClient.changeWorkingDirectory(cwd);
    } catch (Exception e) {
      // do nothing as this means that the directory does not exist and
      // the _isPresent flag is left initialised to false
    }

    return _isPresent;
  }

  /**
   * Deletes supplied file from the remote server
   * 
   * @param fileName -
   *          the file to delete
   * 
   * @throws ConnectionException -
   *           if the client is not logged into the remote server or there was a problem with the deletion
   */
  public void delete(String fileName) throws ConnectionException {
    log.debug("Deleting " + fileName);

    checkLoggedIn();

    // attempt to delete the supplied file from the remote server
    try {
      _ftpClient.deleteFile(fileName);
    } catch (Exception e) {
      close();
      throw new ConnectionException("Failed to delete " + fileName + ": " + e.getMessage(), this);
    }
  }

  /**
   * Retrieves a list of file names on the remote server that match the supplied pattern.
   * 
   * @param filePattern -
   *          the pattern the match file names against
   * 
   * @return - array of the file names or null if none found
   * 
   * @throws ConnectionException -
   *           if there was an communications error
   */
  public String[] fileList(String filePattern) throws ConnectionException {
    log.debug("Getting directory listing: dir=" + getCurrentWorkingDirectory() + ", file pattern=" + filePattern);

    checkLoggedIn();

    String[] s;

    try {
      s = _ftpClient.listNames(filePattern);

      if (s != null)
        log.debug(s.length + " file(s) found");
      else
        log.debug("Failed to get file list [" + filePattern + "]: No Files Found");
    } catch (Exception e) {
      close();
      throw new ConnectionException("Error retrieving file list: " + e.getMessage(), this);
    }

    return s;
  }

  /**
   * Changes the current working directory
   * 
   * @param directoryName -
   *          the new directory
   */
  public void cd(String directoryName) {
    try {
      _ftpClient.changeWorkingDirectory(directoryName);
      log.info("Changed directory to [" + directoryName + "]");
    } catch (IOException e) {
      close();
      throw new ConnectionException("Failed to change direcotries to [" + directoryName + "]: " + e.getMessage(), this);
    }
  }

  /**
   * Returns the current working directory or null if there was an error
   */
  public String getCurrentWorkingDirectory() {
    try {
      return _ftpClient.printWorkingDirectory();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * There are a few FTPClient methods that do not complete the entire sequence of FTP commands to complete a
   * transaction. These commands require some action by the programmer after the reception of a positive intermediate
   * command. After the programmer's code completes its actions, it must call this method to receive the completion
   * reply from the server and verify the success of the entire transaction.
   * 
   * @return true if the transfer has been successful
   * 
   * @throws ConnectionException
   *           if we fail to verify the transfer
   */
  public boolean verifyFileTransfer() throws ConnectionException {
    boolean success = false;
    try {
      if (_ftpClient.completePendingCommand())
        success = true;
    } catch (IOException e) {
      close();
      throw new ConnectionException("Failed to verify file transfer: " + e.getMessage(), this);
    }

    return success;
  }
}
