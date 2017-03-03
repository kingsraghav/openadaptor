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

package org.openadaptor.core.processor;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.core.Component;
import org.openadaptor.core.IDataProcessor;

/**
 * Utility {@link IDataProcessor} which allows multiple processors to be grouped and treated
 * as one.
 * It can group a sequence of data processors, fan-outs are not supported.
 *
 * @author Eddy Higgins
 */
public class ProcessorGroup extends Component implements IDataProcessor {

  private static Log log = LogFactory.getLog(ProcessorGroup.class);

  static String name = ProcessorGroup.class.getName();

  /**
   * list of the processors in this group.
   */
  private IDataProcessor[] processors;

  /**
   * Sets the ordered list of {@link IDataProcessor} to apply
   */
  public void setProcessors(IDataProcessor[] processors) {
    this.processors = processors;
  }

 /**
   * delegates the call to the list of {@link IDataProcessor}s
   */
  public IDataProcessor[] getProcessors() {
    return (processors);
  }

  //END   Bean getters/setters

  /**
   * Displays warning if there are no processors configured. This is not an error but
   * just means that the data will not be transformed
   * <p/>
   *
   * Invoke validation of the component properties provided by each of the processors
   * in this group.
   */
  public void validate(List exceptions) {
    if ( processors == null ) {
      log.warn("No processors configured. The data will be passed through unmolested");
    } else {
      for (int i = 0; i < processors.length; i++)
        processors[i].validate(exceptions);
    }
  }

  /**
   * delegates the call to the list of {@link IDataProcessor}s
   */
  public void reset(Object context) {
    if (processors != null) {
      for (int i = 0; i < processors.length; i++) {
        processors[i].reset(context);
      }
    } else {
      log.warn("ProcessorGroup has no configured processors.");
    }
  }

  /**
   * Apply each of the processers in this group to the record, in order. Each processor takes,
   * as input, the output from the previous stage.
   * <p>
   *  Note that for a given input record, if a processor in the group raises or lowers the
   * cardinality, then the next processors in the group will be called a corresponding
   * number of times (more or less respectively).
   *
   * @param record - Object containing the record
   *
   * @return record array, empty if the record is to be skipped.
   */
  public Object[] process(Object record) {
    Object[] input; //This will get set immediately in the loop
    Object[] output = new Object[] { record }; //Prime the loop.
    if (processors != null) {
      //Walk through each processor. Each one takes the ouput of the
      //previous one as input.
      for (int i = 0; i < processors.length; i++) {
        log.debug("applying processor: " + processors[i]);
        input = output;
        output = new Object[0];
        for (int j = 0; j < input.length; j++) {
          Object[] results = processors[i].process(input[j]);
          int count = results.length;
          if (count > 0) {// Add 'em to output
            Object[] tmp = new Object[output.length + count];
            System.arraycopy(output, 0, tmp, 0, output.length);
            System.arraycopy(results, 0, tmp, output.length, results.length);
            output = tmp;
          }
        }
      }
    } else {
      log.warn("ProcessorGroup has no configured processors.");
    }
    return output;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(name).append(" with ");
    sb.append(processors == null ? "0" : String.valueOf(processors.length));
    return sb.append(" processors").toString();
  }
}
