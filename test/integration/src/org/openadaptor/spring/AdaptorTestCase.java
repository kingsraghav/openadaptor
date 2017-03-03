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

package org.openadaptor.spring;

import junit.framework.TestCase;

import org.openadaptor.util.ResourceUtil;

public class AdaptorTestCase extends TestCase {
  protected static final String RESOURCE_LOCATION = "test/integration/src/";

  private void runAdaptor(String configUrl) {
    SpringAdaptor app = new SpringAdaptor();
    app.addConfigUrl(ResourceUtil.getResourcePath(this, RESOURCE_LOCATION, configUrl));
    app.run();
  }
  
  public void testAdaptor1() {
    runAdaptor("adaptor1.xml");
  }
  
  public void testAdaptor1a() {
    runAdaptor("adaptor1a.xml");
  }
  
  public void testAdaptor2() {
    runAdaptor("adaptor2.xml");
  }
  
  public void testAdaptor2a() {
    runAdaptor("adaptor2a.xml");
  }
  
  public void testAdaptor3() {
    runAdaptor("adaptor3.xml");
  }
  
  public void testAdaptor3a() {
    runAdaptor("adaptor3a.xml");
  }
  
  public void testAdaptor4() {
    runAdaptor("adaptor4.xml");
  }

  public void testAdaptor4a() {
    runAdaptor("adaptor4a.xml");
  }

  public void testAdaptor5() {
    runAdaptor("adaptor5.xml");
  }
  
  public void testAdaptor6() {
    runAdaptor("adaptor6.xml");
  }
}
