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
import org.openadaptor.core.Response;

/**
 * Used in {@link Response}, RuntimeExceptions thrown from calls to {@link IDataProcessor#process(Object)}
 * are wrapped in this class with the argument to the call.
 * 
 * @author perryj, Kris Lachor
 *
 */
public class MessageException extends Throwable {

	protected static final String UNSET_THREAD_NAME = "Originating Thread Name Not Set";
	
    private static final long serialVersionUID = 1L;
	private Object data;
	private Exception exception;
    private String originatingModule;
    private String originatingThreadName = UNSET_THREAD_NAME;    
    private String adaptorName;
    private Map metadata;

    /**
     * Constructor.
     */
    public MessageException(final Object data, final Map metadata, final Exception exception, 
          final String originatingModule) {
		this.data = data;
		this.exception = exception;
        this.originatingModule = originatingModule;
        this.metadata = metadata;
	}

    public MessageException(final Object data, final Map metadata, final Exception exception, 
        final String originatingModule, final String originatingThreadName) {
      this.data = data;
      this.exception = exception;
      this.originatingModule = originatingModule;
      this.originatingThreadName = originatingThreadName;
      this.metadata = metadata;
    }
    
	public Object getData() {
		return data;
	}

	public Exception getException() {
		return exception;
	}
	
	public String toString() {
		return exception.getClass().getName() + ":" + exception.getMessage() + ":" + data.toString();
    }
    
    public String getOriginatingModule() {
      return originatingModule;
    }
    
    public String getOriginatingThreadName() {
      return originatingThreadName;
    }

    public void setOriginatingThreadName(String originatingThreadName) {
      this.originatingThreadName = originatingThreadName;
    }
    
    public String getAdaptorName() {
      return adaptorName;
    }

    public void setAdaptorName(String adaptorName) {
      this.adaptorName = adaptorName;
    }

    public Map getMetadata() {
      return metadata;
    }

    public void setMetadata(Map metadata) {
      this.metadata = metadata;
    }
   
}
