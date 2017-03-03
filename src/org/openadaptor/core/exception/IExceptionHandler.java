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

package org.openadaptor.core.exception;

import java.util.Map;

import org.openadaptor.core.IDataProcessor;
import org.openadaptor.core.IMessageProcessor;
import org.openadaptor.core.node.Node;

/**
 * Interface for OA exception handler that allows for setting a custom
 * exception map. 
 * 
 * @author Kris Lachor
 */
public interface IExceptionHandler extends IDataProcessor{
  
  /**
   * Sets the exceptionMap which defines how to route MessageExceptions from one
   * adaptor component to anothers. 
   * The keys must be IMessageProcessors and the values Maps of Maps. Where the keys
   * are exception classnames and the values List of IMessageProcessors
   * However this setter will do a fair amount of autoboxing to make the caller's life 
   * slightly easier. Non list values will automatically be boxed into a list. 
   * Values which are not actually IMessageProcessors but are Connectors or Processors will
   * be automatically boxed in a Node. 
   * If the parameters is not a map of maps then value is interpreted as the exceptin map
   * for all components.
   * There is a default Autoboxer but this can be overriden.
   * 
   * @param map
   * @see MessageException
   * @see IMessageProcessor
   * @see Node
   */
  void setExceptionMap(Map map);

  /** 
   * Returns the exceptionMap.
   * 
   * @see #setExceptionMap(Map)
   */
  Map getExceptionMap();
  
}
