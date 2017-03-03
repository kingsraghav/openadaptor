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

package org.openadaptor.core.lifecycle;

import org.openadaptor.core.IMessageProcessor;
import org.openadaptor.core.adaptor.Adaptor;

/**
 * Represents classes that "create" new data and process it by delegating to an {@link IMessageProcessor}.
 * Implementations are typically run in separate threads, hence this interface extends {@link Runnable}, 
 * and since they also implement {@link ILifecycleComponent} the framework components monitor their state
 * changes and check the exitCode().
 * 
 * @author perryj
 * @see Adaptor
 */
public interface IRunnable extends ILifecycleComponent, Runnable {
  
  void setMessageProcessor(IMessageProcessor processor);
  
  int getExitCode();
  
  /**
   * @return an instance of a Throwable if this runnable exits with an unhandled 
   *         error. null if the runnable exits correctly.
   */
  Throwable getExitError();
}
