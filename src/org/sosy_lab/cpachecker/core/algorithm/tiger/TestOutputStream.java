/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.io.OutputStream;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;

public class TestOutputStream extends OutputStream {

  /** The logger where to log the written bytes. */
  private LogManager logger;

  /** The level. */
  private Level level;

  /** The internal memory for the written bytes. */
  private String mem;

  /**
   * Creates a new log output stream which logs bytes to the specified logger with the specified
   * level.
   *
   * @param pLogger the logger where to log the written bytes
   * @param pInfo the level
   */
  public TestOutputStream(LogManager pLogger, Level pInfo) {
    setLogger(pLogger);
    setLevel(pInfo);
    mem = "";
  }

  /**
   * Sets the logger where to log the bytes.
   *
   * @param logger the logger
   */
  public void setLogger(LogManager logger) {
    this.logger = logger;
  }

  /**
   * Returns the logger.
   *
   * @return DOCUMENT ME!
   */
  public LogManager getLogger() {
    return logger;
  }

  /**
   * Sets the logging level.
   *
   * @param level DOCUMENT ME!
   */
  public void setLevel(Level level) {
    this.level = level;
  }

  /**
   * Returns the logging level.
   *
   * @return DOCUMENT ME!
   */
  public Level getLevel() {
    return level;
  }

  /**
   * Writes a byte to the output stream. This method flushes automatically at the end of a line.
   *
   * @param b DOCUMENT ME!
   */
  @Override
  public void write(int b) {
    byte[] bytes = new byte[1];
    bytes[0] = (byte) (b & 0xff);
    mem = mem + new String(bytes);

    if (mem.endsWith("\n")) {
      mem = mem.substring(0, mem.length() - 1);
      flush();
    }
  }

  /**
   * Flushes the output stream.
   */
  @Override
  public void flush() {
    logger.log(level, mem);
    mem = "";
  }
}
