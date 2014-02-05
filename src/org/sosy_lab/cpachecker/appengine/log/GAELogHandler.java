/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.appengine.log;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * A log handler implementation that uses a {@link Writer} to save log records.
 * This handler is very similar to {@link StreamHandler} but circumvents calls
 * to a {@link SecurityManager} to avoid problems with setting log level, setting formatter, closing
 * and flushing.
 */
public class GAELogHandler extends Handler {

  private Level level;
  private Formatter formatter;
  private Writer writer;
  private boolean headIsWritten = false;

  public GAELogHandler(OutputStream out, Formatter formatter, Level level) {
    this.writer = new OutputStreamWriter(out);
    this.formatter = formatter;
    this.level = level;
  }

  @Override
  public synchronized void publish(LogRecord record) {
    if (!isLoggable(record)) { return; }

    if (record.getLevel().intValue() >= level.intValue()) {
      String msg = formatter.format(record);
      try {
        if (!headIsWritten) {
          writer.write(formatter.getHead(this));
          headIsWritten = true;
        }
        writer.write(msg);
      } catch (Exception e) {
        reportError(null, e, ErrorManager.WRITE_FAILURE);
      }
    }
  }

  @Override
  public boolean isLoggable(LogRecord record) {
    return (writer != null && record != null);
  }

  /**
   * Flushes and closes the writer.
   * Use this method to actually write the log.
   */
  public void flushAndClose() {
    if (writer != null) {
      try {
        if (!headIsWritten) {
          writer.write(formatter.getHead(this));
          headIsWritten = true;
        }
        writer.write(formatter.getTail(this));
        writer.flush();
        writer.close();
      } catch (Exception e) {
        reportError(null, e, ErrorManager.CLOSE_FAILURE);
      }
      writer = null;
    }
  }

  /**
   * Does nothing.
   */
  @Override
  public void flush() {}

  /**
   * Does nothing.
   */
  @Override
  public void close() throws SecurityException {}
}
