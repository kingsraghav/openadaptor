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

package org.openadaptor.auxil.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.openadaptor.core.lifecycle.ILifecycleComponent;
import org.openadaptor.core.recordable.IComponentMetrics;
import org.openadaptor.core.recordable.IRecordableComponent;

/**
 * A class that oversees creation of all {@link IComponentMetrics} 
 * in an adaptor. It ensures that all {@link AggregateMetrics} are
 * always aware of all detailed {@link ComponentMetrics} in the 
 * system.
 *
 * @author Kris Lachor
 */
public class ComponentMetricsFactory {
  
  private static Collection standardMetricsCol = new ArrayList();
  
  private static Collection aggregateMetricsCol = new ArrayList();

  /**
   * Private constructor.
   */
  private ComponentMetricsFactory() {
  }
 
  /**
   * Creates an instance of {@link ComponentMetrics}.
   * 
   * @param recordableComponent the component for which metrics are created.
   * @return an instance of {@link ComponentMetrics}.
   */
  public static synchronized IComponentMetrics newStandardMetrics(IRecordableComponent recordableComponent){
    IComponentMetrics componentMetrics = new ComponentMetrics(recordableComponent);
    updateAggregateMetrics(recordableComponent, componentMetrics);
    return componentMetrics;
  }
  
  private static void updateAggregateMetrics(IRecordableComponent recordableComponent, IComponentMetrics metrics){
    if(recordableComponent instanceof ILifecycleComponent){
      ((ILifecycleComponent) recordableComponent).addListener(metrics);
    }
    for(Iterator it = aggregateMetricsCol.iterator(); it.hasNext();){
      AggregateMetrics aggregateMetrics = (AggregateMetrics) it.next();
      aggregateMetrics.addComponentMetrics(metrics);
    }
  }

  /**
   * Creates an instance of {@link ReaderMetrics}.
   */
  public static synchronized IComponentMetrics newReaderMetrics(IRecordableComponent recordableComponent){
   IComponentMetrics readerMetrics = new ReaderMetrics(recordableComponent);
   updateAggregateMetrics(recordableComponent, readerMetrics);
   return readerMetrics;
  }
  
  /**
   * Creates an instance of {@link AggregateMetrics}.
   * 
   * @param recordableComponent the component for which metrics are created.
   * @return an instance of {@link AggregateMetrics}.
   */
  public static synchronized IComponentMetrics newAggregateMetrics(IRecordableComponent recordableComponent){
    AggregateMetrics aggregateMetrics = new AggregateMetrics(recordableComponent);
    aggregateMetricsCol.add(aggregateMetrics);
    for(Iterator it = standardMetricsCol.iterator(); it.hasNext();){
      IComponentMetrics componentMetrics = (IComponentMetrics) it.next();
      aggregateMetrics.addComponentMetrics(componentMetrics);
    }
    return aggregateMetrics;
  }
}
