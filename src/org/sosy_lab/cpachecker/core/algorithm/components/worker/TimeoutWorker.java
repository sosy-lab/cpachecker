// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class TimeoutWorker extends Worker {

  private final long wait;

  private boolean doWait;
  private final Timer timer;

  protected TimeoutWorker(LogManager pLogger, long pMillis) {
    super(pLogger);
    wait = pMillis;
    doWait = true;
    timer = new Timer();
  }

  @Override
  public Collection<Message> processMessage(
      Message pMessage) throws InterruptedException, IOException, SolverException, CPAException {
    switch (pMessage.getType()) {
      case ERROR:
      case FOUND_RESULT:
        timer.cancel();
        shutdown();
      case ERROR_CONDITION:
      case ERROR_CONDITION_UNREACHABLE:
      case BLOCK_POSTCONDITION:
        break;
      default:
        throw new AssertionError("Unknown MessageType " + pMessage.getType());
    }
    return ImmutableSet.of();
  }

  @Override
  public void run() {
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        try {
          broadcast(ImmutableSet.of(Message.newResultMessage("timeout", 0, Result.UNKNOWN)));
        } catch (IOException pE) {
          logger.log(Level.SEVERE, "Cannot broadcast timeout message properly because of", pE);
          logger.log(Level.INFO, "Trying to send timeout message one last time...");
          doWait = false;
          run();
        } catch (InterruptedException pE) {
          doWait = false;
          run();
        }
      }
    };
    if (doWait) {
      timer.schedule(task, wait);
      super.run();
    } else {
      try {
        broadcast(ImmutableSet.of(Message.newResultMessage("timeout", 0, Result.UNKNOWN)));
      } catch (IOException | InterruptedException pE) {
        logger.log(Level.SEVERE, "TimeoutWorker failed to send message.", pE);
      }
    }
  }

}
