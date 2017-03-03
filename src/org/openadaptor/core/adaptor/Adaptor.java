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

package org.openadaptor.core.adaptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.core.IMessageProcessor;
import org.openadaptor.core.Message;
import org.openadaptor.core.Response;
import org.openadaptor.core.jmx.Administrable;
import org.openadaptor.core.lifecycle.*;
import org.openadaptor.core.node.ReadNode;
import org.openadaptor.core.recordable.IMetricsPrinter;
import org.openadaptor.core.recordable.ISimpleComponentMetrics;
import org.openadaptor.core.recordable.IComponentMetrics;
import org.openadaptor.core.recordable.IRecordableComponent;
import org.openadaptor.core.router.Router;
import org.openadaptor.core.router.TopologyAnalyser;
import org.openadaptor.core.transaction.ITransactionInitiator;
import org.openadaptor.core.transaction.ITransactionManager;
import org.openadaptor.core.transaction.TransactionManager;
import org.openadaptor.util.Application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An Adaptor is a core framework class that serves as the "top level" Runnable
 * bean. This bean is responsible for managing the lifecycle of
 * {@link ILifecycleComponent}s and one or more {@link IRunnable}s. If any of
 * the {@link IRunnable} components implement the {@link ITransactionInitiator}
 * interface then the Adaptor will call their TransactionManager setter. Once
 * running it receives data from the {@link IRunnable}s that it manages and
 * delegates these to another {@link IMessageProcessor}, typically this
 * delegate is a {@link Router}.
 * 
 * <br/>An Adaptor implements {@link Runnable} and will return when all the
 * {@link IRunnable} components it manages have exited. If an {@link IRunnable}
 * exits with an non-zero return code, it will stop any other {@link IRunnable}
 * components.
 * 
 * <br/> When an Adaptor is configured with an {@link IMessageProcessor} that is
 * also an {@link ILifecycleComponentContainer} it will register itself as the
 * {@link ILifecycleComponentManager}. Routers and Pipelines implement the
 * {@link ILifecycleComponentContainer} interface. This avoids having to
 * duplicate the configuration for all of the components that are referred to in
 * the Router / Pipeline in the Adaptor.
 * 
 * The majority of all the openadaptor examples are within the context of an
 * Adaptor. Either as code which create and runs an Adaptor or as a spring
 * configuration which via the SpringAdaptor class is run as a stand alone
 * process.
 * 
 * @author OA3 Core Team
 * 
 */
public class Adaptor extends Application implements IMessageProcessor, ILifecycleComponentManager, Runnable,
     Administrable, ILifecycleListener, IRecordableComponent {
 
  private static final Log log = LogFactory.getLog(Adaptor.class);

  private static final long DEFAULT_TIMEOUT_MS = 1000;

  /**
   * IMessageProcessor that this adaptor delegates message processing to
   * typically a {@link Router}
   */
  private IMessageProcessor processor;

  /**
   * ordered list of adaptor runnables
   */
  private List runnables;

  /**
   * ordered list of all the lifecycle components that it manages this includes
   * adaptor runnables too
   */
  private List components;

  /**
   * controls whether adaptor creates a new thread to run the runnables if false
   * can only work for an adaptor with a single runnable
   */
  private boolean runRunnablesInCallingThread = false;

  /**
   * threads used to run the runnables
   */
  private Thread[] runnableThreads = new Thread[0];

  /**
   * current state of the adaptor
   */
  private State state = State.STOPPED;

  /**
   * transaction manager, this is passed to the adaptor inpopints
   */
  private ITransactionManager transactionManager;

  /**
   * exit code, this is an aggregation of the adaptor runnable exit codes 0
   * denotes that adaptor exited naturally
   */
  private int exitCode = 0;
  
  /**
   * an agregation of exit exceptions from non-runnables.
   */
  private List exitErrors = new ArrayList();

  /**
   * controls adaptor retry and start, stop, restart functionality
   */
  private AdaptorRunConfiguration runConfiguration;
  
  private boolean hasShutdownHooks = false;
  
  /**
   * Metrics for this adaptor.
   */
  private IComponentMetrics metrics = null;
  
  /**
   * Metrics printer. See the setter for more info.
   */
  private IMetricsPrinter metricsPrinter;
  
  /**
   * shutdown hook
   */
  private Thread shutdownHook = new ShutdownHook();
  
  /** Indicates invocation of the shutdown hook */
  private boolean shutdownInProgress = false;

  private long timeoutMs = DEFAULT_TIMEOUT_MS;

  public Adaptor() {
    super();
    transactionManager = new TransactionManager();
    runnables = new ArrayList();
    components = new ArrayList();
  }

  public void setMessageProcessor(final IMessageProcessor processor) {
    if (this.processor != null) {
      throw new RuntimeException("message processor has already been set");
    }
    this.processor = processor;
    
    /* if the processor has metrics treat them as 'global' Adaptor ones */
    if(processor instanceof IRecordableComponent){
      this.metrics = ((IRecordableComponent) processor).getMetrics();
      String enabled =  metrics.isMetricsEnabled() ? "on" : "off";
      log.info("Metrics are " + enabled);
    }
  }
  
  public IMessageProcessor getMessageProcessor() {
    return this.processor;
  }
  
  public long getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(long timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  private void registerComponents() {
    if (processor != null && processor instanceof ILifecycleComponentContainer) {
      log.debug("MessageProcessor is also a component container. Registering with processor.");
      runnables.clear();
      components.clear();
      ((ILifecycleComponentContainer) processor).setComponentManager(this);
    }
  }

  /**
   * 
   * @param runRunnablesInCallingThread
   *          if true and there is only one {@link IRunnable} then the adaptor
   *          will not create and run another thread when {@link #run} is called
   */
  public void setRunInCallingThread(final boolean runRunnablesInCallingThread) {
    this.runRunnablesInCallingThread = runRunnablesInCallingThread;
  }

  /**
   * 
   * @param transactionManager
   *          ITransactionManager to use, defaults to {@link TransactionManager}.
   *          Any components that implement {@link ITransactionInitiator} will
   *          have their setter called with this when the adaptor is started.
   */
  public void setTransactionManager(final ITransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /**
   * Allows a run configuration to define restart strategies and scheduling.
   */
  public void setRunConfiguration(AdaptorRunConfiguration config) {
    runConfiguration = config;
  }

  /**
   * Registers a an {@link ILifecycleComponent} to be managed once the adaptor
   * is started. Throws a runtime exception if the adaptor state is not
   * {@link State#STOPPED}. Typically this is called by a
   * {@link ILifecycleComponentContainer} as a result of the adaptor calling
   * {@link ILifecycleComponentContainer#setComponentManager(ILifecycleComponentManager)}
   */
  public void register(ILifecycleComponent component) {
    if (state != State.STOPPED) {
      throw new RuntimeException("Cannot register component with running adaptor");
    }
    components.add(component);
    if (component instanceof IRunnable) {
      log.debug("runnable " + component.getId() + " registered with adaptor");
      runnables.add(component);
      ((IRunnable) component).setMessageProcessor(this);
    } else {
      log.debug("component " + component.getId() + " registered with adaptor");
    }
    if (component instanceof ITransactionInitiator) {
      ((ITransactionInitiator) component).setTransactionManager(transactionManager);
    }
    if (component instanceof ReadNode) {
      ((ReadNode) component).setTimeoutMs(timeoutMs );
    }
    
    /* enable recording metrics in all components if necessary */
    if(component instanceof IRecordableComponent && this.isMetricsEnabled()){  
      ((IRecordableComponent) component).setMetricsEnabled(true);
      log.debug("component " + component.getId() + " - enabled metrics");
    }
  }

  /**
   * Throws a runtime exception if the adaptor state is not
   * {@link State#STOPPED}.
   */
  public ILifecycleComponent unregister(ILifecycleComponent component) {
    ILifecycleComponent match = null;
    if (state != State.STOPPED) {
      throw new RuntimeException("Cannot unregister component from running adaptor");
    }
    if (components.remove(component)) {
      match = component;
      runnables.remove(component);
    }
    return match;
  }

  /**
   * delegates processing to the configured delegate, typically a {@link Router}
   */
  public Response process(Message msg) {
    return processor.process(msg);
  }
  

  /**
   * "starts" all of the components that it is managing. Blocks until these
   * components "exit".
   * 
   */
  public void start() {    

    if (state != State.STOPPED) {
      throw new RuntimeException("adaptor is currently " + state.toString());
    }
    
    exitCode = 0;
    exitErrors = new ArrayList();
    registerComponents();
    
    if(metrics!=null){
      metrics.recordComponentStart();
    }

    try {
      
      /* 
       * Register shutdown hook (remove old one first in case the same 
       * adaptor is run multiple times).
       */
      Runtime.getRuntime().removeShutdownHook(shutdownHook);  
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      hasShutdownHooks = true;
      shutdownInProgress = false;
      
      state = State.STARTED;
      validate();
      if (transactionManager != null) {
        transactionManager.setTransactionTimeout(timeoutMs * 10);
      }
      startNonRunnables();
      startRunnables();
      register();

      //Not ready for production use yet.
//      if (processor instanceof Router) {
//        TopologyAnalyser analyser=new TopologyAnalyser(((Router)processor).getRoutingMap());
//        System.out.println(analyser.toStringVerbose());
//      }
      
      if (runRunnablesInCallingThread) {
        runRunnable();
      } else {
        runRunnableThreads();
      }

      log.info("waiting for runnables to stop");
      waitForRunnablesToStop();
      log.info("all runnables are stopped");
 
      state = State.STOPPING;
      stopNonRunnables();
      
      waitForRegistrationToComplete();
      
    } catch (Throwable ex) {
      log.error("failed to start adaptor", ex);
      exitCode = 1;
      exitErrors.add(ex);
    } finally {
      /* handle metrics */
      if(metrics!=null){
        metrics.recordComponentStop();
        if(metricsPrinter!=null){
          metricsPrinter.print(metrics, "Adaptor");
        }
      }
      
      /* 
       * if the adaptor is already stopping (and stop not triggered via shutdown hook) 
       * the shutdown hook needs to be removed. 
       */
      if (state == State.STOPPING && !shutdownInProgress) {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        hasShutdownHooks = false;
      }
      state = State.STOPPED;
      log.info("Adaptor stopped normally.");
    }

    if (getExitCode() != 0) {
      log.fatal("adaptor exited with error code " + getExitCode());
    }
  }

  public void stop() {
    stopRunnables();
    waitForRunnablesToStop();
    stopNonRunnables();
  }

  public void stopNoWait() {
    stopRunnables();
  }

  public void validate(List exceptions) {

    if (runnables.isEmpty()) {
      exceptions.add(new Exception("no runnables"));
    }

    if (runRunnablesInCallingThread && runnables.size() > 1) {
      exceptions.add(new Exception("runRunnablesInCallingThread == true but multiple runnables"));
    }

    for (Iterator iter = components.iterator(); iter.hasNext();) {
      IMessageProcessor processor = (IMessageProcessor) iter.next();
      if (processor instanceof ILifecycleComponent) {
        try {
          ((ILifecycleComponent) processor).validate(exceptions);
        } catch (Exception e) {
          log.error("validation exception for " + processor.toString() + " : ", e);
          exceptions.add(e);
        }
      }
    }
  }

  /**
   * either delegates to a {@link AdaptorRunConfiguration} if one has been configured or
   * calls {@link #start}
   */
  public void run() {
    if (runConfiguration != null) {
      runConfiguration.run(this);
    } else {
      start();
    }
  }

  /**
   * @see #getExitErrors()
   */
  public int getExitCode() {
    int exitCode = this.exitCode;
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      exitCode += runnable.getExitCode();
    }
    return exitCode;
  }
  
  /**
   * Returns a list of exit errors (instances of Throwable) from all Runnables in this adaptor. 
   * The list will be empty if none of the nodes (connectors, processors) in the adaptor 
   * produced an error, or if the adaptor has an exception handler set up (via the 
   * <code>exceptionProcessor</code> property on the <code>Router</code>).
   * 
   * @return a list of exit errors from all Runnables in the adaptor. Empty list if no unhandled 
   *         errors occured in any of the runnables.
   */
  public List getExitErrors(){
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      if(runnable.getExitError() != null) {
        exitErrors.add(runnable.getExitError());
      }
    }
    return exitErrors;
  }

  private void validate() {
    List exceptions = new ArrayList();
    validate(exceptions);
    if (exceptions.size() > 0) {
      for (Iterator iter = exceptions.iterator(); iter.hasNext();) {
        Exception exception = (Exception) iter.next();
        log.error("validation exception", exception);
      }
      throw new RuntimeException("adaptor validation failed");
    }
  }

  private void runRunnable() {
    if (runnables.size() != 1) {
      throw new RuntimeException("cannot run runnable directly as there are " + runnables.size() + " runnables");
    }
    IRunnable runnable = (IRunnable) runnables.get(0);
    runnable.run();
  }

  private void waitForRunnablesToStop() {
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      runnable.waitForState(State.STOPPED);
    }
  }

  private void runRunnableThreads() {
    runnableThreads = new Thread[runnables.size()];
    int i = 0;
    for (Iterator iter = runnables.iterator(); iter.hasNext(); i++) {
      IRunnable runnable = (IRunnable) iter.next();
      runnableThreads[i] = new Thread(runnable);
      if (runnable.getId() != null) {
        runnableThreads[i].setName(runnable.toString());
      }
    }
    for (int j = 0; j < runnableThreads.length; j++) {
      runnableThreads[j].start();
    }
  }

  private void startRunnables() {
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      runnable.addListener(this);
      startLifecycleComponent(runnable);
    }
  }

  private void stopRunnables() {
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      runnable.removeListener(this);
      stopLifecycleComponent(runnable);
    }
  }

  private void interruptRunnables() {
    for (int i = 0; i < runnableThreads.length; i++) {
      runnableThreads[i].interrupt();
    }
  }

  private void stopLifecycleComponent(ILifecycleComponent c) {
    synchronized (c) {
      if (!c.isState(State.STOPPED)) {
        c.stop();
      }
    }
  }

  private void startLifecycleComponent(ILifecycleComponent c) {
    synchronized (c) {
      if (c.isState(State.STOPPED)) {
        c.start();
      }
    }
  }

  private void startNonRunnables() {
    for (Iterator iter = components.iterator(); iter.hasNext();) {
      ILifecycleComponent component = (ILifecycleComponent) iter.next();
      if (!runnables.contains(component)) {
        startLifecycleComponent(component);
      }
    }
  }

  private void stopNonRunnables() {
    for (Iterator iter = components.iterator(); iter.hasNext();) {
      ILifecycleComponent component = (ILifecycleComponent) iter.next();
      if (!runnables.contains(component)) {
        stopLifecycleComponent(component);
      }
    }
  }
  
  private void waitForRegistrationToComplete(){
    int maxSecsWait = getRegistrationTimeoutSecs();
    if(registrationThread != null && registrationThread.isAlive()){
      log.info("Waiting for registration to complete (max " + maxSecsWait + " seconds).");
      try {
        registrationThread.join(maxSecsWait * 1000);
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting for the registration thread to complete.");
      }
    }
  }

  public void exit(boolean wait) {
    state = State.STOPPING;
    if (runConfiguration != null) {
      runConfiguration.setExitFlag();
    }
    if (wait) {
      stop();
    } else {
      stopNoWait();
    }
  }

  public class ShutdownHook extends Thread {
    public void run() {
      log.info("shutdownhook invoked, calling exit()");
      try {
        Adaptor.this.shutdownInProgress=true;
        Adaptor.this.exit(false);
      } catch (Throwable t) {
        log.error("uncaught error or exception", t);
      }
    }
  }

  public void stateChanged(ILifecycleComponent component, State newState) {
    if (state == State.STARTED && runnables.contains(component) && newState == State.STOPPED) {
      if (((IRunnable) component).getExitCode() != 0) {
        log.warn(component.getId() + " has exited with non zero exit code, stopping adaptor");
        stopNoWait();
      }
    }
  }
  
  protected IRecordableComponent getRecordableComponent(){
    return this;
  }

  /**
   * Returns the state of this adaptor.
   * Method primarily for JMX use.
   * 
   * @return the state of the adaptor as a String
   */
  public String getState() {
    return state.toString();
  }
  
  /**
   * Static method that facilitates programmatic execution of an adaptor.
   * 
   * @param processMap - a map with adaptor processors.
   * @return instance of an executed adaptor.
   */
  public static Adaptor run(Map processMap){
    return Adaptor.run(processMap, null);
  }
  
  /**
   * Static method that facilitates programmatic execution of an adaptor.
   * 
   * @param processMap - a map with adaptor processors.
   * @param exceptionProcessor - an exception handler.
   * @return instance of an executed adaptor.
   */
  public static Adaptor run(Map processMap, Object exceptionProcessor){
    Router router = new Router();
    Adaptor adaptor = new Adaptor();
    adaptor.setMessageProcessor(router);
    router.setProcessMap(processMap);
    if(exceptionProcessor != null){
      router.setExceptionProcessor(exceptionProcessor);
    }
    adaptor.run();
    return adaptor;
  }
  
  /**
   * Static method that facilitates programmatic execution of an adaptor.
   * 
   * @param processors a list with adaptor processors.
   * @return instance of an executed adaptor.
   */
  public static Adaptor run(List processors){
    Router router = new Router();
    Adaptor adaptor = new Adaptor();
    adaptor.setMessageProcessor(router);
    router.setProcessors(processors);
    adaptor.run();
    return adaptor;
  }

  /**
   * Checks if Adaptor has shutdown hooks installed. For system testing mostly.
   * 
   * @return true is shutdown hooks are installed, false otherwise.
   */
  protected boolean hasShutdownHooks() {
    return hasShutdownHooks;
  }

  /**
   * Returns metrics for this adaptor.
   * 
   * @see IRecordableComponent#getMetrics()
   */
  public IComponentMetrics getMetrics() {
    return metrics;
  }

  /**
   * True if metrics recording is enabled for this adaptor. 
   */
  public boolean isMetricsEnabled() {
    return metrics.isMetricsEnabled();
  }

  /**
   * Enables/disables metrics recording for this adaptor. 
   */
  public void setMetricsEnabled(boolean metricsEnabled) {
    this.metrics.setMetricsEnabled(metricsEnabled);
  }
  
  /**
   * Returns a collection of message processors as defined in the Router.
   * 
   * @return a collection of message processors.
   */
  public Collection getMessageProcessors(){ 
    if(processor instanceof Router){
      return ((Router) processor).getRoutingMap().getMessageProcessors();
    }
    else{
      return null;
    }
  }

  /**
   * Optional. Sets a metrics printer for this adaptor. Property will 
   * take effect only if the Router has enabled metrics, it will be 
   * ignored otherwise. 
   * 
   * Metrics will be printed just before the Adaptor exits and only 
   * if it exits gracefully (i.e. not via 'kill -9', Ctrl+C etc.). 
   * 
   * @param metricsPrinter
   */
  public void setMetricsPrinter(IMetricsPrinter metricsPrinter) {
    this.metricsPrinter = metricsPrinter;
  }

  /**
   * @see Administrable#getAdmin()
   */
  public Object getAdmin() {
    return new Admin();
  }
  
  /**
   * Interface exposed via JMX.
   */
  public interface AdminMBean extends ISimpleComponentMetrics{
  
    /**
     * Dumps adaptor's state.
     * 
     * @return this adaptor's state formatted as HTML table
     */
    String dumpState();
 
    /**
     * Gracefully exits the adaptor.
     */
    void exit();

    void interrupt();
  }

  /**
   * Implementation of the interface exposed via JMX. 
   */
  public class Admin implements AdminMBean {

    /**
     * @see AdminMBean#exit()
     */
    public void exit() {
      Adaptor.this.exit(true);
    }

    /**
     * @see AdminMBean#dumpState()
     */
    public String dumpState() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<table>");
      buffer.append("<tr><td>Adaptor</td><td/>");
      buffer.append("<td>").append(Adaptor.this.state.toString()).append("</td></tr>");
      ArrayList components = new ArrayList();
      components.addAll(Adaptor.this.components);
      for (Iterator iter = components.iterator(); iter.hasNext();) {
        ILifecycleComponent component = (ILifecycleComponent) iter.next();
        buffer.append("<tr><td/>");
        buffer.append("<td>").append(component.getId()).append("</td>");
        buffer.append("<td>").append(component.getState().toString()).append("</td></tr>");
      }
      return buffer.toString();
    }

    /**
     * @see AdminMBean#interrupt()
     */
    public void interrupt() {
      Adaptor.this.interruptRunnables();
    }

    /**
     * @see ISimpleComponentMetrics#getIntervalTime()
     */
    public String getIntervalTime() {
      return metrics.getIntervalTime();
    }

    /**
     * @see ISimpleComponentMetrics#getProcessTime()
     */
    public String getProcessTime() {
      return metrics.getProcessTime();
    }

    /**
     * @see ISimpleComponentMetrics#getUptime()
     */
    public String getUptime() {
      return metrics.getUptime();
    }

    /**
     * @see ISimpleComponentMetrics#getInputMsgs()
     */
    public String getInputMsgs() {
      return metrics.getInputMsgs();
    }

    /**
     * @see ISimpleComponentMetrics#getOutputMsgs()
     */
    public String getOutputMsgs() {
      return metrics.getOutputMsgs();
    }

    /**
     * @see ISimpleComponentMetrics#setMetricsEnabled(boolean)
     */
    public void setMetricsEnabled(boolean metricsEnabled) {
      metrics.setMetricsEnabled(metricsEnabled); 
    }

    /**
     * @see ISimpleComponentMetrics#isMetricsEnabled()
     */
    public boolean isMetricsEnabled() {
      return metrics.isMetricsEnabled();
    }

    /**
     * @see ISimpleComponentMetrics#getDiscardsAndExceptions()
     */
    public String getDiscardsAndExceptions() {
      return metrics.getDiscardsAndExceptions();
    }
  }

}
