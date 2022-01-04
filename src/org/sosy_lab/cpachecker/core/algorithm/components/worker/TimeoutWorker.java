// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.Optional;
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
  public Optional<Message> processMessage(
      Message pMessage) throws InterruptedException, IOException, SolverException, CPAException {
    return Optional.empty();
  }

  @Override
  public void run() {
    try {
      if (doWait) {
        wait(wait);
      }
      broadcast(answer(Message.newResultMessage("timeout", 0, Result.UNKNOWN)));
    } catch (InterruptedException | IOException pE) {
      doWait = false;
      run();
    }
  }

}
