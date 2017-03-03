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

package org.openadaptor.auxil.connector.jndi;

import java.util.ArrayList;
import java.util.List;

import javax.naming.*;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openadaptor.auxil.orderedmap.IOrderedMap;
import org.openadaptor.core.exception.*;

/**
 * <p>
 * This class is a connector which will generate IOrderedMaps from the results of a JNDI search.
 * </p>
 * <p>
 * It should be configured with an appropriate JNDIConnection and JNDISearch objects.
 * </p>
 * 
 * @author Eddy Higgins, Andrew Shire, Kris Lachor
 * @see AbstractJNDIReadConnector
 * @see JNDIConnection
 * @see JNDISearch
 */

public class JNDIReadConnector extends AbstractJNDIReadConnector {

  private static final Log log = LogFactory.getLog(JNDIReadConnector.class);

  // internal state:
  /**
   * Direcory Context for this reader.
   */
  protected DirContext _ctxt;

  /**
   * Naming enumeration which holds the results of an executed search.
   */
  protected NamingEnumeration _namingEnumeration;


  // bean properties:
  /**
   * JNDIConnection which this reader will use
   */
  protected JNDIConnection jndiConnection;
  
  
  /**
   * Constructor.
   */
  public JNDIReadConnector() {
  }

  /**
   * Constructor.
   * 
   * @param id
   */
  public JNDIReadConnector(String id) {
    super(id);
  }
   

  // BEGIN Bean getters/setters

  /**
   * Assign a JNDI connection for use by the reader. Behaviour is undefined if this is set when the reader has already
   * called connect().
   *
   * @param connection The JNDIConnection to use
   */
  public void setJndiConnection(JNDIConnection connection) {
    jndiConnection = connection;
  }

  /**
   * Return the JNDIConnection for this reader.
   *
   * @return JNDIConnection instance.
   */
  public JNDIConnection getJndiConnection() {
    return jndiConnection;
  }
  
  /**
   * Return the dirContext for this reader.
   * <p/>
   * The dirContext is set when the underlying <code>JNDIConnection</code> object has it's connect() method invoked.
   *
   * @return DirContext DirContext, or <tt>null</tt> if it hasn't been set yet.
   */
  public DirContext getContext() {
    return _ctxt;
  }
  
  // END Bean getters/setters

  
  /**
   * Checks that the mandatory properties have been set
   *
   * @param exceptions list of exceptions that any validation errors will be appended to
   */
  public void validate(List exceptions) {
    super.validate(exceptions);
    if (jndiConnection == null) {
      exceptions.add(new ValidationException("jndiConnection property is not set", this));
    }
  }
  
  
  /**
   * Ask the enrichment connection for the enrichment data that matches
   * the incoming record (i.e. perform the enrichment lookup).
   * 
   * @return enrichment data for the current incoming record
   * @throws Exception for example if there was a connectivity problem
   */
  protected IOrderedMap[] getMatches() throws Exception {
    IOrderedMap[] results = null;
    boolean treatMultiValuedAttributesAsArray = search.getTreatMultiValuedAttributesAsArray();
    String joinArraysWithSeparator = search.getJoinArraysWithSeparator();
    NamingEnumeration current = search.execute(this.getContext());
    ArrayList resultList = new ArrayList();
    while (current.hasMore()) {
      SearchResult searchResult = (SearchResult) current.next();
      resultList.add(JNDIUtils.getOrderedMap(searchResult, treatMultiValuedAttributesAsArray,
          joinArraysWithSeparator));
    }
    if (resultList.size() > 0) {
      results = (IOrderedMap[]) resultList.toArray(new IOrderedMap[resultList.size()]);
    }
    return results;
  }
  

  /**
   * Establish an external JNDI connection.
   * <p/>
   * If already connected, do nothing.
   *
   * @throws ConnectionException if an AuthenticationException or NamingException occurs
   */
  public void connect() {
    try {
      _ctxt = jndiConnection.connect();
    } catch (AuthenticationException ae) {
      log.warn("Failed JNDI authentication for principal: " + jndiConnection.getSecurityPrincipal());
      throw new ConnectionException("Failed to Authenticate JNDI connection - " + ae.toString(), ae, this);
    } catch (NamingException ne) {
      log.warn(ne.getMessage());
      throw new ConnectionException("Failed to establish JNDI connection - " + ne.toString(), ne, this);
    }
    log.info(getId() + " connected");
  }

  /**
   * Disconnect external JNDI connection.
   * <p/>
   * If already disconnected, do nothing.
   *
   * @throws ConnectionException if a NamingException occurs.
   */
  public void disconnect() {
    log.debug("Connector: [" + getId() + "] disconnecting ....");
    if (_ctxt != null) {
      try {
        _ctxt.close();
      } catch (NamingException ne) {
        log.warn(ne.getMessage());
      }
    }
    log.info(getId() + " disconnected");
  }

  /**
   * Return the next record from this reader.
   * <p/>
   * It first tests if the underlying search has already executed. If not, it executes it. It then takes the next
   * available result from the executed search, and returns it.<br>
   * If the result set is empty, then it returns <tt>null</tt> indicating that the reader is exhausted.
   *
   * @return Object[] containing an IOrderedMap of results, or <tt>null</tt>
   * @throws OAException
   */
  public Object[] next(long timeoutMs) throws OAException {
    
    Object[] result = null;
    
    /* different processing path when used as IEnrichmentReadConnector */
    if(inputParameters!=null){
      result = processOrderedMap(inputParameters);
      inputParameters = null;
      return result;
    }
    
    /* non-enhancement processor */
    try {
      if (!_searchHasExecuted) {
        log.info("Executing JNDI search - " + search.toString());
        _namingEnumeration = search.execute(_ctxt);
        _searchHasExecuted = true;
      }
      if (_namingEnumeration.hasMore()) {
        IOrderedMap map = JNDIUtils.getOrderedMap((SearchResult) _namingEnumeration.next(), search
            .getTreatMultiValuedAttributesAsArray(), search.getJoinArraysWithSeparator());

        result = new Object[] { map };
      }
    } catch (CommunicationException e) {
      throw new ConnectionException(e.getMessage(), e, this);
    } catch (ServiceUnavailableException e) {
      throw new ConnectionException(e.getMessage(), e, this);
    } catch (NamingException e) {
      throw new ProcessingException(e.getMessage(), e, this);
    }
    return result;
  }

  /**
   * @return false if the search has not yet been performed or there are still results
   * to be processed then we are not dry
   */
  public boolean isDry() {
    try {
      if (_namingEnumeration == null || _namingEnumeration.hasMore()) {
        return false;
      }
    } catch (NamingException e) {
    }

    return true;
  }
}
