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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class TimeoutWorker extends Worker {

  private final long wait;

  private boolean doWait;

  protected TimeoutWorker(LogManager pLogger, long pMillis) {
    super(pLogger);
    wait = pMillis;
    doWait = true;
  }

  @Override
  public Collection<Message> processMessage(
      Message pMessage) throws InterruptedException, IOException, SolverException, CPAException {
    return ImmutableSet.of();
  }

  @Override
  public void run() {
    try {
      if (doWait) {
        Thread.sleep(wait);
      }
      broadcast(ImmutableSet.of(Message.newResultMessage("timeout", 0, Result.UNKNOWN)));
    } catch (InterruptedException pE) {
      doWait = false;
      run();
      logger.log(Level.SEVERE, "Thread interrupted because of", pE);
    } catch (IOException pE) {
      logger.log(Level.SEVERE, "Cannot broadcast timeout message properly because of", pE);
      logger.log(Level.INFO, "Trying to send timeout message one last time...");
      try {
        broadcast(ImmutableSet.of(Message.newResultMessage("timeout", 0, Result.UNKNOWN)));
      } catch (IOException | InterruptedException pEx) {
        logger.log(Level.SEVERE, "Failed to send timeout message.", pEx);
      }
    }
  }

}
