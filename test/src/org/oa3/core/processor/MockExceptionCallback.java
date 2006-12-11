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
package org.oa3.core.processor;

import java.util.ArrayList;
import java.util.List;

import org.oa3.core.exception.ProcessorException;

/*
 * File: $Header: /cvs/oa3/test/src/org/oa3/processor/mock/MockExceptionCallback.java,v 1.2 2006/10/20 15:25:24 higginse
 * Exp $ Rev: $Revision: 1.2 $ Created Dec 9, 2005 by Kevin Scully
 */
public class MockExceptionCallback {

  private List payloadExceptionList = new ArrayList();

  public List getPayloadExceptionList() {
    return payloadExceptionList;
  }

  private class PayloadExceptionEntry {
    Object payload;

    ProcessorException exception;

    PayloadExceptionEntry(Object payload, ProcessorException exception) {
      this.payload = payload;
      this.exception = exception;
    }

    Object getPayload() {
      return payload;
    }

    ProcessorException getException() {
      return exception;
    }
  }

}