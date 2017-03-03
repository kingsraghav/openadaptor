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

package org.openadaptor.auxil.processor.xml;

import java.io.StringWriter;
import java.net.URL;
import java.util.List;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DocumentSource;
import org.openadaptor.core.Component;
import org.openadaptor.core.IDataProcessor;
import org.openadaptor.core.exception.ProcessingException;
import org.openadaptor.core.exception.ValidationException;
import org.openadaptor.util.FileUtils;

/**
 * Applies the XSLT defined in the properties to the record and returns the result as an String.
 * <p />
 * 
 * Can support transforms on either XML String or dom4j document records.
 * <p />
 * 
 * Uses the dom4j library to apply the transform
 * 
 * @author Russ Fennell
 */
public class XsltProcessor extends Component implements IDataProcessor {

  private static final Log log = LogFactory.getLog(XsltProcessor.class);

  private String xsltFile;

  private Transformer transform;

  /**
   * Sets the location of the file containing the XSLT
   * 
   * @param xsltFile
   *          the path to the file
   */
  public void setXsltFile(String xsltFile) {
    this.xsltFile = xsltFile;
  }

  /**
   * Hook to perform any validation of the component properties required by the implementation. Defult behaviour should
   * be a no-op.
   */
  public void validate(List exceptions) {
    try {
      loadXSLT();
    } catch (RuntimeException ex) {
      exceptions.add(ex);
    }
  }

  public void reset(Object context) {
  }

  /**
   * Trys to load the XSLT from the file defined in the properties (will also try to find the file on the classpath if
   * it can).
   * 
   * @throws ValidationException
   *           if the XSLT file is not defined in the properties, the file cannot be found or there was an error parsing
   *           it
   */
  private void loadXSLT() {
    if (xsltFile == null)
      throw new ValidationException("xsltFile property not set", this);

    // if the file doesn't exist try to get it via the classpath
    URL url = FileUtils.toURL(xsltFile);
    if (url == null)
      throw new ValidationException("File not found: " + xsltFile, this);

    // load the transform
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      ErrorListener errorListener = new ErrorListener() {
		
		public void warning(TransformerException exception) throws TransformerException {
			log.warn(exception.getMessage(), exception);
		}
		
		public void fatalError(TransformerException exception) throws TransformerException {
			throw exception;
		}
		
		public void error(TransformerException exception) throws TransformerException {
			throw exception;
		}
	};
	factory.setErrorListener(errorListener);
      transform = factory.newTransformer(new StreamSource(url.getPath()));

      log.info("Loaded XSLT [" + xsltFile + "] successfully");
    } catch (TransformerConfigurationException e) {
      throw new ValidationException("Failed to load XSLT: " + e.getMessage(), this);
    }
  }

  /**
   * Apply the transform to the record. The record can be either a XML string or a dom4j document object
   * 
   * @param record
   *          the message record
   * 
   * @return a String[] with one String resulting from the transform
   * 
   * @throws ProcessingException
   *           if the record type is not supported
   */
  public Object[] process(Object record) throws ProcessingException {
    if (record == null)
      return null;

    if (record instanceof String)
      return transform((String) record);

    if (record instanceof Document)
      return transform((Document) record);

    // if we get this far then we cannot process the record
    throw new ProcessingException("Invalid record (type: " + record.getClass().toString() + "). Cannot apply transform", this);
  }

  /**
   * Applies the transform to the XML String
   * 
   * @param s
   *          the XML text
   * 
   * @return an array containing a single XML string representing the transformed XML string supplied
   */
  private Object[] transform(String s) {
    return transform(createDOMFromString(s));
  }

  /**
   * Applies the transform to the Dom4J document
   * 
   * @param d
   *          the document to transform
   * 
   * @return an array containing a single XML string representing the transformed document
   */
  private Object[] transform(Document d) {
    try {
      return new String[] { transform(transform, d) };
    } catch (TransformerException e) {
      throw new ProcessingException("Transform failed: " + e.getMessage(), this);
    }
  }

  public static String transform(Transformer transformer, Document d) throws TransformerException {
    DocumentSource source = new DocumentSource(d);
    StringWriter sw = new StringWriter();
    Result result = new StreamResult(sw);
    transformer.transform(source, result);
    return sw.toString();
  }
  
  /**
   * Use the XML supplied to create a DOM document
   * 
   * @param xml
   *          valid XML
   * 
   * @return dom4j document object
   * 
   * @throws ProcessingException
   *           if the supplied XML cannot be parsed
   */
  private Document createDOMFromString(String xml) {
    try {
      return DocumentHelper.parseText(xml);
    } catch (DocumentException e) {
      throw new ProcessingException("Failed to parse XML: " + e.getMessage(), this);
    }
  }
}
