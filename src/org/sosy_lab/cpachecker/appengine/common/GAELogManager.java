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
package org.sosy_lab.cpachecker.appengine.common;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.sosy_lab.common.LogManager;

import com.google.common.base.Joiner;


public class GAELogManager implements LogManager {

  private Handler handler;

  public GAELogManager(Handler handler) {
    this.handler = handler;
  }

  @Override
  public boolean wouldBeLogged(Level pPriority) {
    return true;
  }

  @Override
  public void log(Level pPriority, Object... pArgs) {
    Joiner joiner = Joiner.on(" ").skipNulls();
    handler.publish(new LogRecord(pPriority, joiner.join(pArgs)));
  }

  @Override
  public void logf(Level pPriority, String pFormat, Object... pArgs) {
    handler.publish(new LogRecord(pPriority, String.format(pFormat, pArgs)));
  }

  @Override
  public void logUserException(Level pPriority, Throwable pE, String pAdditionalMessage) {
    handler.publish(new LogRecord(pPriority, pE.getMessage()+" "+pAdditionalMessage));
  }

  @Override
  public void logDebugException(Throwable pE, String pAdditionalMessage) {
    handler.publish(new LogRecord(Level.ALL ,pE.getMessage()+" "+pAdditionalMessage));
  }

  @Override
  public void logDebugException(Throwable pE) {
    handler.publish(new LogRecord(Level.ALL ,pE.getMessage()));
  }

  @Override
  public void logException(Level pPriority, Throwable pE, String pAdditionalMessage) {
    handler.publish(new LogRecord(Level.ALL ,pE.getMessage()+" "+pAdditionalMessage));
  }

  @Override
  public void flush() {
    handler.flush();
  }

  @Override
  public void close() {
    handler.close();
  }

}
