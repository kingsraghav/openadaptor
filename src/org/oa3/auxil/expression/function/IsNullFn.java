/*
 * [[
 * Copyright (C) 2001 - 2006 The Software Conservancy as Trustee. All rights
 * reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Nothing in this notice shall be deemed to grant any rights to
 * trademarks, copyrights, patents, trade secrets or any other intellectual
 * property of the licensor or any contributor except as expressly stated
 * herein. No patent license is granted separate from the Software, for
 * code that you delete from the Software, or for combinations of the
 * Software with other software or hardware.
 * ]]
 */
package org.oa3.auxil.expression.function;

import org.apache.log4j.Logger;
import org.oa3.auxil.expression.ExpressionException;

/**
 * Replaces NULL with the specified replacement value in much the same fashion as the isNull() function in T-SQL.
 * 
 * @author Russ Fennell
 */
public class IsNullFn extends AbstractFunction {
  static Logger log = Logger.getLogger(IsNullFn.class);

  /**
   * Calls super constructor with an name of "isnull" and an argCount of 2
   */
  public IsNullFn() {
    super("isnull", 2);
  }

  /**
   * @param args
   *          Object array which is expected to hold the value to be tested and the default value to return if the first
   *          is null
   * 
   * @return the string or the replacement value if it is null
   * 
   * @throws ExpressionException
   *           if the default value is null
   */
  protected Object operate(Object[] args) throws ExpressionException {
    String value = getArgAsString(args[0], null);

    if (value == null) {
      value = getArgAsString(args[1], null);
      validateNotNull(value, 1);
      log.debug("Using default value [" + value + ']');
    }

    return value;
  }

}