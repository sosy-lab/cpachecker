/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;


/**
 * @author Gregor Endler
 *
 * Class providing all logging functionality.
 *
 * The log levels used are the ones from java.util.logging.
 * SEVERE, WARNING and INFO are used normally, the first two denoting (among other things)
 * exceptions. FINE, FINER, FINEST, and ALL correspond to main application, central algorithm,
 * component level, and debug level respectively.
 *
 * The main advantage of this class is that the arguments to the log methods
 * are only converted to strings, if the message is really logged.
 */
@Options(prefix = "log", 
    description = "Possible log levels in descending order "
    + "\n(lower levels include higher ones):"
    + "\nOFF:      no logs published"
    + "\nSEVERE:   error messages"
    + "\nWARNING:  warnings"
    + "\nINFO:     messages"
    + "\nFINE:     logs on main application level"
    + "\nFINER:    logs on central CPA algorithm level"
    + "\nFINEST:   logs published by specific CPAs"
    + "\nALL:      debugging information"
    + "\nCare must be taken with levels of FINER or lower, as output files may "
    + "become quite large and memory usage might become an issue.")
public class LogManager {

  @Option(name="level", toUppercase=true, 
      description="log level of file output",
      values={"OFF", "SEVERE", "WARNING", "INFO", "FINE", "FINER", "FINEST", "ALL"})
  private String logLevelStr = "OFF";

  @Option(name="consoleLevel", toUppercase=true, 
      description="log level of console output",
      values={"OFF", "SEVERE", "WARNING", "INFO", "FINE", "FINER", "FINEST", "ALL"})
  private String consoleLevelStr = "INFO";

  @Option(name="fileExclude", toUppercase=true,
      description="single levels to be excluded from being logged")
  private String[] excludeLevelsFileStr = {};

  @Option(name="consoleExclude", toUppercase=true,
      description="single levels to be excluded from being logged")
  private String[] excludeLevelsConsoleStr = {};

  @Option(name="file", type=Option.Type.OUTPUT_FILE, 
      description="name of the log file")
  private File outputFile = new File("CPALog.txt");

  @Option(description="maximum size of log output strings before they will be truncated")
  private int truncateSize = 10000;
  
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
  private static final Joiner messageFormat = Joiner.on(' ').useForNull("null");
  private final Logger logger;

  /**
   * This class may be used to read the log into a String.
   */
  public static class StringHandler extends Handler {

    private final StringBuilder sb = new StringBuilder();

    @Override
    public void close() throws SecurityException {
      // ignore
    }

    @Override
    public void flush() {
      // ignore
    }

    @Override
    public synchronized void publish(LogRecord record) {
      // code copied from java.util.logging.StreamHandler#publish(LogRecord)
      if (!isLoggable(record)) {
        return;
      }
      String msg;
      try {
        msg = getFormatter().format(record);
      } catch (Exception ex) {
        // We don't want to throw an exception here, but we
        // report the exception to any registered ErrorManager.
        reportError(null, ex, ErrorManager.FORMAT_FAILURE);
        return;
      }

      try {
        sb.append(msg);
      } catch (Exception ex) {
        // We don't want to throw an exception here, but we
        // report the exception to any registered ErrorManager.
        reportError(null, ex, ErrorManager.WRITE_FAILURE);
      }
    }

    public String getLog() {
      return sb.toString();
    }


    public void clear() {
      sb.setLength(0);
      sb.trimToSize(); // free memory
    }
  }

  // class to handle formatting for file output
  private static class FileLogFormatter extends Formatter {
    @Override
    public String format(LogRecord lr) {
      String[] className = lr.getSourceClassName().split("\\.");
      String[] methodName = lr.getSourceMethodName().split("\\.");
      return dateFormat.format(lr.getMillis()) + "\t "
      + "level: " + lr.getLevel().toString() + "\t "
      + className[className.length-1]  + "."
      + methodName[0]  + "\t "
      + lr.getMessage() + "\n\n";
    }
  }

  // class to handle formatting for console output
  public static class ConsoleLogFormatter extends Formatter {
    @Override
    public String format(LogRecord lr) {
      String[] className = lr.getSourceClassName().split("\\.");
      String[] methodName = lr.getSourceMethodName().split("\\.");
      return lr.getMessage() + " ("
      + className[className.length-1]  + "."
      + methodName[0]  + ", "
      + lr.getLevel().toString()
      + ")\n\n";
    }
  }

  private static class LogLevelFilter implements Filter {

    private final List<Level> excludeLevels;

    public LogLevelFilter(List<Level> excludeLevels) {
      this.excludeLevels = excludeLevels;
    }

    @Override
    public boolean isLoggable(LogRecord pRecord) {
      return !(excludeLevels.contains(pRecord.getLevel()));
    }
  }

  public LogManager(Configuration config) throws InvalidConfigurationException {
    this(config, new ConsoleHandler());
  }

  /**
   * Constructor which allows to customize where the console output of the
   * LogManager is written to. Suggestions for the consoleOutputHandler are
   * StringHandler or OutputStreamHandler.
   *
   * The level, filter and formatter of that handler are set by this class.
   *
   * @param consoleOutputHandler A handler, may not be null.
   */
  public LogManager(Configuration config, Handler consoleOutputHandler) throws InvalidConfigurationException {
    Preconditions.checkNotNull(consoleOutputHandler);
    config.inject(this);

    Level logFileLevel = Level.parse(logLevelStr);
    Level logConsoleLevel = Level.parse(consoleLevelStr);

    Level logLevel;
    if (logFileLevel.intValue() > logConsoleLevel.intValue()) {
      logLevel = logConsoleLevel; // smaller level is more detailed logging
    } else {
      logLevel = logFileLevel;
    }

    logger = Logger.getAnonymousLogger();
    logger.setLevel(logLevel);
    logger.setUseParentHandlers(false);

    if (logLevel == Level.OFF) {
      return;
    }

    // create console logger
    setupHandler(consoleOutputHandler, new ConsoleLogFormatter(), logConsoleLevel, excludeLevelsConsoleStr);

    // create file logger
    if (logFileLevel != Level.OFF && outputFile != null) {
      try {
        Files.createParentDirs(outputFile);

        Handler outfileHandler = new FileHandler(outputFile.getAbsolutePath(), false);
        
        setupHandler(outfileHandler, new FileLogFormatter(), logFileLevel, excludeLevelsFileStr);

      } catch (IOException e) {
        // redirect log messages to console
        if (logConsoleLevel.intValue() > logFileLevel.intValue()) {
          logger.getHandlers()[0].setLevel(logFileLevel);
        }

        logger.log(Level.WARNING, "Could not open log file " + e.getMessage() + ", redirecting log output to console");
      }
    }
  }

  private void setupHandler(Handler handler, Formatter formatter, Level level, String[] excludeLevelsStr) {
    //build up list of Levels to exclude from logging
    if (excludeLevelsStr.length != 0) {
      ImmutableList.Builder<Level> excludeLevels = ImmutableList.builder();
      for (String s : excludeLevelsStr) {
        excludeLevels.add(Level.parse(s));
      }
      handler.setFilter(new LogLevelFilter(excludeLevels.build()));
    } else {
      handler.setFilter(null);
    }

    //handler with format for the console logger
    handler.setFormatter(formatter);

    //log only records of priority equal to or greater than the level defined in the configuration
    handler.setLevel(level);

    logger.addHandler(handler);
  }

  /**
   * Returns true if a message with the given log level would be logged.
   * @param priority the log level
   * @return whether this log level is enabled
   */
  public boolean wouldBeLogged(Level priority) {
    return (logger.isLoggable(priority));
  }

  /**
   * Logs any message occurring during program execution.
   * @param priority the log level for the message
   * @param args the message (can be an arbitrary number of objects containing any information), will be concatenated by " "
   */
  public void log(Level priority, Object... args) {
    log(priority, 1, args);
  }

  /**
   * Logs any message occurring during program execution.
   * @param priority the log level for the message
   * @param callStackOffset how many frames two ignore on call stack (useful for helper methods)
   * @param args the message (can be an arbitrary number of objects containing any information), will be concatenated by " "
   */
  public void log(Level priority, int callStackOffset, Object... args) {

    //Since some toString() methods may be rather costly, only log if the level is
    //sufficiently high.
    if (wouldBeLogged(priority))  {

      String[] argsStr = new String[args.length];
      int size = args.length;
      for (int i = 0; i < args.length; i++) {
        String arg = args[i].toString();
        if ((truncateSize > 0) && (arg.length() > truncateSize)) {
          argsStr[i] = "<ARGUMENT OMITTED BECAUSE " + arg.length() + " CHARACTERS LONG>";
        } else {
          argsStr[i] = arg;
        }
        size += argsStr[i].length();
      }

      LogRecord record = new LogRecord(priority, messageFormat.join(argsStr));
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      callStackOffset += 2; // add 2 for this method and the getStackTrace method

      record.setSourceClassName(trace[callStackOffset].getClassName());
      record.setSourceMethodName(trace[callStackOffset].getMethodName());

      logger.log(record);
    }
  }

  /**
   * Logs any Exception occurring during program execution by composing a message of
   * the exception's properties, and optionally an additional message passed.
   *
   * After logging, print the stack trace.
   * TODO remove this, better log stack trace with level debug (if enabled)
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  public void logException(Level priority, Throwable e, String additionalMessage) {

    if (wouldBeLogged(priority)) {

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
      record.setSourceClassName(trace[2].getClassName());
      record.setSourceMethodName(trace[2].getMethodName());

      logger.log(record);
    }

    e.printStackTrace();
    System.err.println();

  }

  public void flush() {
    for (Handler handler : logger.getHandlers()) {
      handler.flush();
    }
  }
  public void close() {
    for (Handler handler : logger.getHandlers()) {
      handler.close();
    }
  }
}
