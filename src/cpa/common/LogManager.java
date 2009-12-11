/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.*;

import cmdline.CPAMain;

/**
 * @author Gregor Endler
 * 
 * Class providing all logging functionality.
 * 
 * The log levels used are the ones from java.util.logging.
 * SEVERE, WARNING and INFO are used normally, the first two denoting (among other things)
 * exceptions. FINE, FINER, FINEST, and ALL correspond to main application, central CPA algorithm,
 * specific CPA level, and debug level respectively. The debug information is further divided into
 * debug levels 1-4, denoted e.g. by the string DEBUG_1 in the message of the log.
 *  
 */
public class LogManager {

  private static LogManager instance = null;
  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
  private Level logLevel = Level.parse(CPAMain.cpaConfig.getProperty("log.level").toUpperCase());
  private Level logConsoleLevel = Level.parse(CPAMain.cpaConfig.getProperty("log.consoleLevel").toUpperCase());
  private ArrayList<Level> excludeLevelsFile = new ArrayList<Level>();
  private ArrayList<Level> excludeLevelsConsole = new ArrayList<Level>();
  private FileHandler outfileHandler;
  private Logger fileLogger;
  private Logger consoleLogger;

  //inner class to handle formatting for file output
  private class CPALogFormatter extends Formatter {
    @Override
    public String format(LogRecord lr) {
      String[] className = lr.getSourceClassName().split("\\.");
      String[] methodName = lr.getSourceMethodName().split("\\.");
      return "log: " + lr.getLevel().toString() + "\t" 
      + dateFormat.format(lr.getMillis()) + "    "
      + className[0]  + "."  
      + methodName[0]  + "\t" 
      + lr.getMessage() + "\n\n";
    }
  }

  //inner class to handle formatting for console output
  private class CPAConsoleLogFormatter extends Formatter {
    @Override
    public String format(LogRecord lr) {
      String[] className = lr.getSourceClassName().split("\\.");
      String[] methodName = lr.getSourceMethodName().split("\\.");
      return lr.getMessage() + " (" 
      + className[0]  + "."  
      + methodName[0]  + ", " 
      + lr.getLevel().toString()
      + ")\n\n";
    }
  }

  private LogManager() throws SecurityException, IOException {

    if(logLevel != Level.OFF) {

      String outfilePath = CPAMain.cpaConfig.getProperty("output.path");
      String outfileName = CPAMain.cpaConfig.getProperty("log.file");

      //build up list of Levels to exclude from logging
      String[] excludeFile = CPAMain.cpaConfig.getPropertiesArray("log.fileExclude");
      if (excludeFile != null) {
        for (String s : excludeFile) {
          excludeLevelsFile.add(Level.parse(s));
        }
      }

      //if no filename is given, use default value
      if (outfileName == null) {
        outfileName = "CPALog.txt";
      }

      //create or fetch file logger
      fileLogger = Logger.getLogger("resMan.fileLogger");

      //handler with format for the fileLogger
      outfileHandler = new FileHandler(outfilePath + outfileName, false);
      outfileHandler.setFormatter(new CPALogFormatter());

      //only file output when using the fileLogger 
      fileLogger.setUseParentHandlers(false);
      fileLogger.addHandler(outfileHandler);
      //log only records of priority equal to or greater than the level defined in the configuration
      fileLogger.setLevel(logLevel);
    }

    if (logConsoleLevel != Level.OFF) {

      //build up list of Levels to exclude from logging
      String[] excludeConsole = CPAMain.cpaConfig.getPropertiesArray("log.fileExclude");
      if (excludeConsole != null) {
        for (String s : excludeConsole) {
          excludeLevelsConsole.add(Level.parse(s));
        }
      }

      //create or fetch console logger
      consoleLogger = Logger.getLogger("resMan.consoleLogger");
      //set format for console output
      //per default, the console handler is found in the handler array of each logger's parent at [0]
      consoleLogger.getParent().getHandlers()[0].setFormatter(new CPAConsoleLogFormatter());
      //need to set the level for both the logger and its handler
      consoleLogger.getParent().getHandlers()[0].setLevel(logConsoleLevel);
      consoleLogger.setLevel(logConsoleLevel);
    }
  }

  public static LogManager getInstance() {
    try {
      if (instance == null) {
        instance = new LogManager();
      } 
    }catch (IOException e) {
      System.err.println("ERROR, could not find path of logfile location, check configuration");
      System.exit(1);
    }
    return instance;
  }

  //Logs any message occurring during program execution.
  //args can be an arbitrary number of objects containing any information.
  public void log(Level priority, Object... args) {

    //Since some toString() methods may be rather costly, only log if the level is 
    //sufficiently high. Ensure priority != OFF (since it is possible to abuse the logging 
    //system by publishing logs with Level OFF).
    if ((priority.intValue() >= logLevel.intValue() || 
        priority.intValue() >= logConsoleLevel.intValue()) && 
        priority != Level.OFF &&
        (!excludeLevelsFile.contains(priority) ||
         !excludeLevelsConsole.contains(priority)))  {

      StringBuffer buf = new StringBuffer();

      for (Object o : args) {
        if (o != null) {
          buf.append(o.toString() + " ");
        } else {
          buf.append("null ");
        }
      }

      LogRecord record = new LogRecord(priority, buf.toString());
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      record.setSourceClassName(trace[2].getFileName());
      record.setSourceMethodName(trace[2].getMethodName());

      if (priority.intValue() >= logLevel.intValue()
          && !excludeLevelsFile.contains(priority)) {
        fileLogger.log(record);
      }
      if (priority.intValue() >= logConsoleLevel.intValue()
          && !excludeLevelsConsole.contains(priority)) {
        consoleLogger.log(record);
      }
    }
  }

  //Logs any Exception occurring during program execution by composing a message of
  //the exception's properties, and optionally an additional message passed.
  //After logging, print the stack trace.
  public void logException(Level priority, Exception e, String additionalMessage) {

    if ((priority.intValue() >= logLevel.intValue() ||
        priority.intValue() >= logConsoleLevel.intValue()) && 
        priority != Level.OFF) {

      String logMessage = e.getMessage() + ", " + e.getStackTrace()[0];

      if (additionalMessage == null || additionalMessage.equals("")) {
        logMessage = "Exception " + "(" + logMessage + ")";
      } else {
        logMessage = "Exception: " + additionalMessage + " (" + logMessage + ")";
      }

      //The following is copied from log().
      //It should not be replaced with a call to log() because of the fixed reference
      //to the correct position of the caller in the stack trace.
      LogRecord record = new LogRecord(priority, logMessage);
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      record.setSourceClassName(trace[2].getFileName());
      record.setSourceMethodName(trace[2].getMethodName());

      if (priority.intValue() >= logLevel.intValue()) {
        fileLogger.log(record);
      }
      if (priority.intValue() >= logConsoleLevel.intValue()) {
        consoleLogger.log(record);
      }

    }

    e.printStackTrace();
    System.err.println();

  }

  public void flush() {
    if(outfileHandler != null) {
      outfileHandler.flush();
    }
  }

  public Level getLogLevel() {
    return logLevel;
  }

  public Level getLogConsoleLevel() {
    return logConsoleLevel;
  }

}
