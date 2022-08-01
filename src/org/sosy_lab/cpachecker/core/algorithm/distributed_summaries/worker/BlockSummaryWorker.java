// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class BlockSummaryWorker implements BlockSummaryActor {

  private final LogManager logger;
  private final StringBuildingLogHandler logHandler;
  private final Path logfile;
  private final String id;

  /**
   * Abstract definition of a Worker. All workers enter the same routine of receiving and producing
   * messages.
   *
   * @param pId the id of the worker
   */
  protected BlockSummaryWorker(String pId, AnalysisOptions pOptions) {
    id = pId;
    logHandler = new StringBuildingLogHandler();
    logger = BasicLogManager.createWithHandler(logHandler);
    logfile = Paths.get(pOptions.getLogDirectory().toString(), id + ".log");
  }

  @Override
  public void broadcast(Collection<BlockSummaryMessage> pMessage) throws InterruptedException {
    pMessage.forEach(m -> logger.log(Level.INFO, m));
    for (BlockSummaryMessage message : pMessage) {
      getConnection().write(message);
    }
  }

  protected void broadcastOrLogException(Collection<BlockSummaryMessage> pMessage) {
    try {
      broadcast(pMessage);
    } catch (InterruptedException pE) {
      logger.logfException(
          Level.SEVERE, pE, "Broadcasting %s messages interrupted unexpectedly.", pMessage);
    }
  }

  @Override
  public void run() {
    final Connection connection = getConnection();
    try (connection) {
      while (!shutdownRequested()) {
        broadcast(processMessage(nextMessage()));
        if (Thread.currentThread().isInterrupted()) {
          break;
        }
      }
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      logger.logfException(
          Level.SEVERE, pE, "%s faced a problem while processing messages.", getId());
      broadcastOrLogException(ImmutableList.of(BlockSummaryMessage.newErrorMessage(getId(), pE)));
    } finally {
      logger.logf(Level.INFO, "Worker %s finished and shuts down.", id);
      try {
        IO.openOutputFile(logfile, StandardCharsets.UTF_8).write(logHandler.getLog());
      } catch (IOException ignore) {
        // could not write logfile
        logger.logf(Level.SEVERE, "Unable to write logfiles to %s", logfile);
      }
    }
  }

  public boolean hasPendingMessages() {
    return getConnection().hasPendingMessages();
  }

  @Override
  public final String getId() {
    return id;
  }

  public LogManager getLogger() {
    return logger;
  }
}
