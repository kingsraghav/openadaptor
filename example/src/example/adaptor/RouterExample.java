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

package example.adaptor;

import java.util.HashMap;
import java.util.Map;

import org.openadaptor.auxil.connector.iostream.reader.FileReadConnector;
import org.openadaptor.auxil.connector.iostream.reader.string.LineReader;
import org.openadaptor.auxil.connector.iostream.writer.FileWriteConnector;
import org.openadaptor.auxil.convertor.delimited.DelimitedStringToOrderedMapConvertor;
import org.openadaptor.auxil.convertor.xml.OrderedMapToXmlConvertor;
import org.openadaptor.core.adaptor.Adaptor;
import org.openadaptor.core.router.Router;

/**
 * This example code is equivalent to the router.xml spring example
 * 
 * It shows how to programmatically construct an adaptor.
 * In this example the configuration of the router is identical to that of
 * the router in router.xml.
 * 
 * @author perryj
 */
public class RouterExample {

  public static void main(String[] args) {
    
    // see Simple.java
    
    FileReadConnector reader = new FileReadConnector("Reader");
    reader.setDataReader(new LineReader());
    
    // see Simple.java
    
    DelimitedStringToOrderedMapConvertor mapConverter;
    mapConverter = new DelimitedStringToOrderedMapConvertor("MapConverter");
    mapConverter.setFieldNames(new String[] {"field"});
    
    // see Simple.java

    OrderedMapToXmlConvertor xmlConverter;
    xmlConverter = new OrderedMapToXmlConvertor("XmlConverter");
    xmlConverter.setRootElementTag("data");
    
    // see Simple.java

    FileWriteConnector writer = new FileWriteConnector("Writer");
    
    // Router
    
    Map processMap = new HashMap();
    processMap.put(reader, mapConverter);
    processMap.put(mapConverter, xmlConverter);
    processMap.put(xmlConverter, writer);
    Router router = new Router();
    router.setProcessMap(processMap);
    
    // create Adaptor and set 
    
    Adaptor adaptor = new Adaptor();
    adaptor.setMessageProcessor(router);
    
    // this starts the adaptor
    
    adaptor.run();
  }

}
