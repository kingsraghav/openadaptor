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

package org.openadaptor.auxil.expression.function;

import java.util.Date;

import org.openadaptor.auxil.expression.ExpressionException;

/**
 * Function to convert a long value into a Date().
 * <p>
 * Is it essentially a wrapper around new Date(long longValue);
 * @deprecated ScriptProcessor or ScriptFilterProcessor may be used in place of Expressions
 */
public class DateFn extends AbstractFunction {
  public static final String NAME = "date";

  // private static final Log log = LogFactory.getLog(DateFn.class);

  public DateFn() {
    super(DateFn.NAME, 1);
  }

  /**
   * Create a <code>Date</code> object from a <code>Long</code> value.
   * <p>
   * (It will also allow a <code>Date</code> argument, returning it unchanged)
   * 
   * @param args
   *          Object[] which should contain a single numeric argument, or a Date.
   * @return Object containing the <code>Date</code> corresponding to the supplied long value.
   * @throws ExpressionException
   *           If argument is null or not a Long.
   */
  protected Object operate(Object[] args) throws ExpressionException {
    if (args[0] instanceof Date) {
      return args[0];
    } else {
      return new Date(getArgAsNumber(args, 0).longValue());
    }
  }
}
