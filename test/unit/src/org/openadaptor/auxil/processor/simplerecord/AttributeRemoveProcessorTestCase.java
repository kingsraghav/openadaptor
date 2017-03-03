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
package org.openadaptor.auxil.processor.simplerecord;

import java.util.ArrayList;
import java.util.List;

import org.openadaptor.auxil.processor.simplerecord.AttributeRemoveProcessor;
import org.openadaptor.core.IDataProcessor;
import org.openadaptor.core.exception.RecordException;

/**
 * Basic tests for AttributeRemoveProcessor.
 * 
 * @author Kevin Scully
 */
public class AttributeRemoveProcessorTestCase extends AbstractTestAttributeModifyProcessor {

  protected static final String TARGET_ATTRIBUTE_NAME = "target";

  protected static final String EXPRESSION_EVAL_RESULT = "id";

  /**
   * Test Processor is an instance of AttributeRemoveProcessor.
   * 
   * @return The Test Processor.
   */
  protected IDataProcessor createProcessor() {
    AttributeRemoveProcessor processor = new AttributeRemoveProcessor();
    return processor;
  }

  /** Remove the record attribute matching the result of evaluating the expression set for the processor */
  public void testProcessAccessorSet() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(simpleRecordAccessor);
    getAttributeModifyProcessor().setAttributeName(null);
    getAttributeModifyProcessor().setExpression(expression);
    simpleRecordAccessorMock.expects(once()).method("asSimpleRecord").with(eq(record)).will(returnValue(record));
    expressionMock.expects(once()).method("evaluate").with(eq(record)).will(returnValue(EXPRESSION_EVAL_RESULT));
    recordMock.expects(once()).method("clone").will(returnValue(record));
    recordMock.expects(once()).method("getRecord").will(returnValue(record));
    recordMock.expects(once()).method("remove").with(eq(EXPRESSION_EVAL_RESULT));
    // Do the test
    try {
      testProcessor.process(record);
    } catch (RecordException e) {
      fail("Unexpected Exception [" + e + "]");
    }
  }

  public void testProcessNoAccessorSet() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(null);
    getAttributeModifyProcessor().setAttributeName(null);
    getAttributeModifyProcessor().setExpression(expression);
    expressionMock.expects(once()).method("evaluate").with(eq(record)).will(returnValue(EXPRESSION_EVAL_RESULT));
    recordMock.expects(once()).method("clone").will(returnValue(record));
    recordMock.expects(never()).method("getRecord");
    recordMock.expects(once()).method("remove").with(eq(EXPRESSION_EVAL_RESULT));
    // Do the test
    try {
      testProcessor.process(record);
    } catch (RecordException e) {
      fail("Unexpected Exception [" + e + "]");
    }
  }

  /**
   * Test that process works as expected when the SimpleRecordAccessor is set.
   * <p>
   * This is the absolute minimum testing needed to show that SimpleRecordAccessors are being handled correctly by this
   * Processor.
   */
  public void testProcessRecordWithoutAccessor() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(null);
    getAttributeModifyProcessor().setAttributeName(null);
    getAttributeModifyProcessor().setExpression(expression);
    simpleRecordAccessorMock.expects(never()).method("asSimpleRecord");
    expressionMock.expects(once()).method("evaluate").with(eq(record)).will(returnValue(EXPRESSION_EVAL_RESULT));
    recordMock.expects(once()).method("clone").will(returnValue(record));
    recordMock.expects(never()).method("getRecord");
    recordMock.expects(once()).method("remove").with(eq(EXPRESSION_EVAL_RESULT));
    // Do the test
    try {
      testProcessor.process(record);
    } catch (RecordException e) {
      fail("Unexpected Exception [" + e + "]");
    }
  }

  /** Remove the record attribute matching the attributeName set for the processor */
  public void testProcessRecordWithoutExpression() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(null);
    getAttributeModifyProcessor().setAttributeName(TARGET_ATTRIBUTE_NAME);
    getAttributeModifyProcessor().setExpression(null);
    recordMock.expects(once()).method("clone").will(returnValue(record));
    recordMock.expects(never()).method("getRecord");
    recordMock.expects(once()).method("remove").with(eq(getAttributeModifyProcessor().getAttributeName()));
    // Do the test
    try {
      testProcessor.process(record);
    } catch (RecordException e) {
      fail("Unexpected Exception [" + e + "]");
    }
  }

  public void testValidateAttributeSet() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(null);
    getAttributeModifyProcessor().setAttributeName(TARGET_ATTRIBUTE_NAME);
    getAttributeModifyProcessor().setExpression(null);
    // test
    List exceptions = new ArrayList();
    getAttributeModifyProcessor().validate(exceptions);
    assertTrue("Should have no validate exceptions", exceptions.isEmpty());

  }

  public void testValidateExpressionSet() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(null);
    getAttributeModifyProcessor().setAttributeName(null);
    getAttributeModifyProcessor().setExpression(expression);
    // test
    List exceptions = new ArrayList();
    getAttributeModifyProcessor().validate(exceptions);
    assertTrue("Should have no validate exceptions", exceptions.isEmpty());

  }

  public void testValidateNothingSet() {
    // Set Expectations
    getAbstractSimpleRecordProcessor().setSimpleRecordAccessor(null);
    getAttributeModifyProcessor().setAttributeName(null);
    getAttributeModifyProcessor().setExpression(null);
    // test
    List exceptions = new ArrayList();
    getAttributeModifyProcessor().validate(exceptions);
    assertTrue("Should have one validate exceptions", exceptions.size() == 1);

  }

}
