package com.ibm.mq;

import java.io.OutputStreamWriter;

import org.openadaptor.StubException;

public class MQException extends Exception {
  
  public static final int MQRC_CONNECTION_BROKEN = 2009;
  public static final int MQRC_NO_MSG_AVAILABLE = 2033;
  public static OutputStreamWriter log = null;
  
  public int reasonCode;
  
  private static final long serialVersionUID = 1L;

  public MQException() {
    throw new StubException(StubException.WARN_MQ_JAR);
  }

  public MQException(String message, Throwable cause) {
    throw new StubException(StubException.WARN_MQ_JAR);
  }

  public MQException(String message) {
    throw new StubException(StubException.WARN_MQ_JAR);
  }

  public MQException(Throwable cause) {
    throw new StubException(StubException.WARN_MQ_JAR);
  }   
  
  public static void logExclude(int logCode) {
  }
  
  public static void logExclude(Integer logCode) {
  }
}
