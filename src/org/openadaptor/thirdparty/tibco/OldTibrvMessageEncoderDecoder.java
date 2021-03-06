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

package org.openadaptor.thirdparty.tibco;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;

/**
 * Legacy encoder/decoder for Tibrv Messages.
 * This class only exists to maintain some backwards compatibility with the previous
 * Tibrv connectors, which were extremely limited and incomplete.
 * Users are strongly encouraged to avoid using the older mechanism.
 * 
 * @deprecated - Only exists for backwards compatibility with oa 3.x versions 3.4.4 and earlier
 * 
 * @since 3.4.5 solely for backwards compatibility with 3.4.4 behaviour
 * 
 * @author Eddy Higgins (higginse)
 */
public class OldTibrvMessageEncoderDecoder implements ITibrvMessageDecoder,ITibrvMessageEncoder {
  private static final Log log = LogFactory.getLog(OldTibrvMessageEncoderDecoder.class);

  public static final String DEFAULT_FIELD_NAME="DATA"; //Backwards compatibility

  private String fieldName=DEFAULT_FIELD_NAME;

  /**
   * name of field this component expects the data to be in
   * @deprecated This only exists for backwards compatibility
   * @param fieldName
   */
  public void setFieldName(final String fieldName) {
    this.fieldName = fieldName;
  }

  /**
   * Decode a supplied Tibco Rendezvous Message (TibrvMsg).
   * @param msg TibrvMsg instance
   * @return Map containing the result of decoding
   */
  public Object decode(TibrvMsg msg) throws TibrvException {
    Object result=null;
    if (msg!=null) { //Something to process!
      log.debug("Rendezvous message with subject:"+msg.getSendSubject());
      result=msg.getField(fieldName).data;
    }
    return result;
  }

  public TibrvMsg encode(Object data) throws TibrvException {
    TibrvMsg msg=new TibrvMsg();
    msg.update(fieldName, data.toString());
    return msg;
  }
}
