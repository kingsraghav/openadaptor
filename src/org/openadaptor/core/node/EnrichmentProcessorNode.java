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

package org.openadaptor.core.node;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.auxil.orderedmap.IOrderedMap;
import org.openadaptor.core.IComponent;
import org.openadaptor.core.IEnrichmentProcessor;
import org.openadaptor.core.IEnrichmentReadConnector;
import org.openadaptor.core.IMessageProcessor;
import org.openadaptor.core.IMetadataAware;
import org.openadaptor.core.Message;
import org.openadaptor.core.Response;
import org.openadaptor.core.lifecycle.ILifecycleComponent;

/**
 * Class that brings together {@link IEnrichmentProcessor} and {@link IMessageProcessor}.
 * Manages the lifecycle of {@link IEnrichmentProcessor} and the lifecycle of 
 * {@link IEnrichmentReadConnector} embedded in it.
 * 
 * Essentially it is similar to {@link Node}, which it extends. The differences are
 * firstly this node connects and disconnects the underlying reader, secondly 
 * processing of a single record of data is different than in Node -
 * this difference is implemented in {@link EnrichmentProcessorNode#processSingleRecord(Object)}.
 * 
 * If the <code>enrichmentProcessor</code> and/or <code>readConnector</code> are 
 * {@link IMetadataAware} this class injects the metadata.
 * 
 * @author Kris Lachor
 * @since Post 3.3
 * @see Node
 * @see IMessageProcessor
 */
public final class EnrichmentProcessorNode extends Node implements IMessageProcessor{

  private static final Log log = LogFactory.getLog(EnrichmentProcessorNode.class);
  
  private IEnrichmentProcessor enrichmentProcessor;
  
  protected IEnrichmentReadConnector readConnector;
  
  private long readerTimeoutMs = ReadNode.DEFAULT_TIMEOUT_MS;
  
  /**
   * Constructor.
   *
   * @see Node#Node()
   */
  public EnrichmentProcessorNode() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param id
   * @see Node#Node(String)
   */
  public EnrichmentProcessorNode(String id) {
    super(id);
  }
  
  /**
   * Constructor.
   * 
   * @param id
   * @param processor
   * @see Node#Node(String)
   */
  public EnrichmentProcessorNode(String id, IEnrichmentProcessor processor) {
    super(id);
    this.enrichmentProcessor = processor;
    this.readConnector = processor.getReadConnector();
  }

  /**
   * Sets the enrichment processor and the read connector.
   */
  public void setEnrichmentProcessor(IEnrichmentProcessor enrichmentProcessor) {
    this.enrichmentProcessor = enrichmentProcessor;
    this.readConnector = enrichmentProcessor.getReadConnector();
  }

  /**
   * Connects the reader.
   * 
   * @see ILifecycleComponent#start
   * @see Node#start()
   */
  public void start() {
    readConnector.connect();
    super.start();
  }

  /**
   * Disconnects the reader.
   * 
   * @see ILifecycleComponent#stop
   * @see Node#stop()
   */
  public void stop() {
    readConnector.disconnect();
    super.stop();
  }
  
  /**
   * Processes individual record of input data. First asks the enrichment processor to prepare
   * query parameters for the reader, then sets the parameters on the reader and asks reader to
   * call resource for more data. Last step is the actual enrichment of input data with the
   * additional data from the reader.
   * 
   * @param record input record
   * @return result/additional data from the enrichment processor
   * @see Node#processSingleRecord(Object)
   */
  public Object [] processSingleRecord(Object record){
    IOrderedMap parameters = null;
    if(! (record instanceof IOrderedMap)){
      log.warn("enrichment processor parameters not an IOrderedMap");
    }
    else{
      parameters = enrichmentProcessor.prepareParameters((IOrderedMap)record);
    }
    if (log.isDebugEnabled() && parameters!=null){
      log.debug("Parameters to set on the reader: " + parameters);
      log.debug("Number of parameters: " +  parameters.size()); 
    }
    if(parameters == null){
      log.warn("No parameters for reader");
    }
    if (log.isDebugEnabled()) {
      log.debug("Calling for enrichment data...");
    }
    Object [] enrichmentData = readConnector.next(parameters, readerTimeoutMs);
    if (log.isDebugEnabled()) {
      log.debug("Reader returned: " + enrichmentData + ". Calling enricher...");
    }
    Object [] outputs = enrichmentProcessor.enrich(record, enrichmentData);
    return outputs;
  }
  
  /**
   * Sets the metadata on enrichmentProcessor and the embedded read connector, then
   * delegates to {@link Node#process(Message)}.
   * 
   * @see Node#process(Message)
   */
  public Response process(Message msg) {
    if(enrichmentProcessor instanceof IMetadataAware){
      ((IMetadataAware) enrichmentProcessor).setMetadata(msg.getMetadata());
    }
    if(readConnector instanceof IMetadataAware){
      ((IMetadataAware) readConnector).setMetadata(msg.getMetadata());
    }
    return super.process(msg);
  }
  
  /**
   * In addition to calling superclass {@link Node#validate(List)}, runs the 
   * validation of the enrichment processor.
   * 
   * @see ILifecycleComponent#validate(List)
   * @see Node#validate(List)
   * @see IEnrichmentProcessor#validate(List)
   */
  public void validate(List exceptions) {
    super.validate(exceptions);
    enrichmentProcessor.validate(exceptions);
  }

  /**
   * @see org.openadaptor.core.lifecycle.LifecycleComponent#getId()
   */
  public String getId() {
    String id = super.getId();
    if (id == null && enrichmentProcessor instanceof IComponent) {
      return ((IComponent)enrichmentProcessor).getId();
    }
    return id;
  }

}
