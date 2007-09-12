/*
 Copyright (C) 2001 - 2007 The Software Conservancy as Trustee. All rights reserved.

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.core.IMessageProcessor;
import org.openadaptor.core.Message;
import org.openadaptor.core.Response;
import org.openadaptor.core.lifecycle.ILifecycleComponent;
import org.openadaptor.core.lifecycle.IRunnable;
import org.openadaptor.core.lifecycle.LifecycleComponent;
import org.openadaptor.core.lifecycle.State;
/*
 * File: $Header: $
 * Rev:  $Revision: $
 * Created Sep 4, 2007 by oa3 Core Team
 */

public abstract class AbstractNodeRunner extends LifecycleComponent implements IMessageProcessor, IRunnable {

  private static final Log log = LogFactory.getLog(AbstractNodeRunner.class);

  protected int exitCode;
  private IMessageProcessor messageProcessor;
  protected IMessageProcessor messageProcessorDelegate;
  protected Throwable exitThrowable;
  private ILifecycleComponent managedComponent;

  public Response process(Message msg) {
    return messageProcessor.process(msg);
  }

  public void setMessageProcessor(IMessageProcessor processor) {
    messageProcessor = processor;
  }

  public void setMessageProcessorDelegate(IMessageProcessor processor) {
   messageProcessorDelegate = processor;
   if (messageProcessorDelegate instanceof ILifecycleComponent  && managedComponent == null) {
     managedComponent = (ILifecycleComponent)messageProcessorDelegate;
   }
  }

  public int getExitCode() {
    return exitCode;
  }

  /**
   * @return an instance of a Throwable if this runnable exits with an unhandled
   *         error. null if the runnable exits correctly.
   */
  public Throwable getExitError() {
    return exitThrowable;
  }

  public abstract void run();

  protected boolean isStillRunning() {
    if (managedComponent != null) {
      return ((isState(State.STARTED)) && managedComponent.isState(State.STARTED));
    }
    else {
      return isState(State.STARTED);
    }
  }

  public void start() {
    if (managedComponent != null) { managedComponent.start();}
    super.start();
  }

  protected void stopping() {
    if (isState(State.STARTED)) {
      log.info(getId() + " is stopping");
      setState(State.STOPPING);
    }
  }

  public void stop() {
    if (!isState(State.STOPPING)) {
      stopping();
    }
    if (managedComponent != null) { managedComponent.stop();}
    super.stop();
  }

  public ILifecycleComponent getManagedComponent() {
    return managedComponent;
  }

  public void setManagedComponent(ILifecycleComponent managedComponent) {
    this.managedComponent = managedComponent;
  }
}