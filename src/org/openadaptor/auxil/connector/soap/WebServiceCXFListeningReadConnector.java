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

package org.openadaptor.auxil.connector.soap;

import java.net.UnknownHostException;
import java.util.List;
import java.net.InetAddress;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openadaptor.core.connector.QueuingReadConnector;
import org.openadaptor.core.exception.ConnectionException;
import org.openadaptor.core.jmx.Administrable;


/**
 * ReadConnector that exposes a webservice which allows external clients to send it data.
 * This connector subsumes the XFire based {@link WebServiceListeningReadConnector}.
 * 
 * @author Kris Lachor
 */
public class WebServiceCXFListeningReadConnector extends QueuingReadConnector implements IStringDataProcessor,
           Administrable{

  private static final Log log = LogFactory.getLog(WebServiceCXFListeningReadConnector.class);
  
  private static final String HTTP_PREFIX = "http://";

  private static final String NAMESPACE = "http://www.openadaptor.org";
  
  private static final String DEFAULT_SERVICENAME = "OAService";
  
  private String serviceName = DEFAULT_SERVICENAME;
  
  private int port = 8080;
  
  private Server server;

  /**
   * Default constructor.
   */
  public WebServiceCXFListeningReadConnector() {
    super();
  }

  /**
   * Constructor.
   */
  public WebServiceCXFListeningReadConnector(String id) {
    super(id);
  }

  /**
   * Set name of web service
   * @param name Service name
   */
  public void setServiceName(final String name) {
    serviceName = name;
  }

  /**
   * Programmatic publishing of an Endpoint.
   */
  public void connect() {
    if(server==null){
	  ServerFactoryBean svrFactory = new ServerFactoryBean();
	  svrFactory.setServiceClass(IStringDataProcessor.class);
      QName namespace = new QName(NAMESPACE, serviceName);
      InetAddress localMachine = null;
      try {
        localMachine = java.net.InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
        String errMsg = "Unable to determine hostname";
        log.error(errMsg, e);
        throw new ConnectionException(errMsg);
      }
      String hostname = localMachine.getCanonicalHostName();
	  String endpointUrl = HTTP_PREFIX + hostname + ":" + port + "/" + serviceName;
	  svrFactory.setAddress(endpointUrl);
      svrFactory.setServiceBean(this);
      svrFactory.setServiceName(namespace);
	  server = svrFactory.create();
	  log.info("Created and started WS Endpoint " + getEndpoint());
    }else{
      server.start();
      log.info("Started WS Endpoint " + getEndpoint());
    }
  }

  /**
   * Process requests
   *
   * @param s request string
   */
  public void process(String s) {
    enqueue(s);
  }

  public void validate(List exceptions) {
  }

  public void disconnect() { 
	if (null != server) {
	  server.stop();
	}
  }
  
  /**
   * Create and return wsdl string
   *
   * @return String return wsdl string
   */
  public String getEndpoint() {
	if(server==null){
      return null;
	}
    String endpointAddress = server.getEndpoint().getEndpointInfo().getAddress();
    String wsdl = endpointAddress + "?wsdl";
    log.debug("Endpoint: " + wsdl);
    return wsdl;
  }
  
  public void setPort(final int port) {
    this.port = port;
  }
  
  /**
   * @see Administrable
   */
  public Object getAdmin() {
    return new Admin();
  }

  /**
   * @see Administrable
   */
  public interface AdminMBean {
    int getQueueLimit();

    int getQueueSize();
  }
  
  public class Admin implements AdminMBean {

    public int getQueueLimit() {
      return WebServiceCXFListeningReadConnector.this.getQueueLimit();
    }

    public int getQueueSize() {
      return WebServiceCXFListeningReadConnector.this.getQueueSize();
    }
  }

}
