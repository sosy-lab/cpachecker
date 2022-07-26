// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class TimeoutBlockSummaryWorker extends BlockSummaryWorker {

  private final long waitTime;
  private final Timer timer;
  private final TimerTask task;
  private boolean shutdown;
  private final Connection connection;

  protected TimeoutBlockSummaryWorker(
      TimeSpan pTimeSpan, Connection pConnection, AnalysisOptions pOptions)
      throws InvalidConfigurationException {
    super("timeout-worker", pOptions);
    waitTime = pTimeSpan.asMillis();
    timer = new Timer();
    connection = getConnection();
    task =
        new TimerTask() {
          @Override
          public void run() {
            broadcastOrLogException(
                ImmutableSet.of(
                    ActorMessage.newResultMessage(getId(), 0, Result.UNKNOWN, ImmutableSet.of())));
            shutdown = true;
            timer.cancel();
            timer.purge();
          }
        };
  }

  @Override
  public Collection<ActorMessage> processMessage(ActorMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    switch (pMessage.getType()) {
      case ERROR:
        // fall through
      case FOUND_RESULT:
        shutdown = true;
        timer.cancel();
        timer.purge();
        return ImmutableSet.of();
      case ERROR_CONDITION:
        // fall through
      case ERROR_CONDITION_UNREACHABLE:
        // fall through
      case BLOCK_POSTCONDITION:
        // fall through
        break;
      default:
        throw new AssertionError(
            "Unknown type of "
                + pMessage.getType().getDeclaringClass()
                + ": "
                + pMessage.getType());
    }
    return ImmutableSet.of();
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void run() {
    // abort in waitTime milliseconds
    timer.schedule(task, waitTime);
    // read incoming messages to terminate in case analysis finished early
    super.run();
  }
}
