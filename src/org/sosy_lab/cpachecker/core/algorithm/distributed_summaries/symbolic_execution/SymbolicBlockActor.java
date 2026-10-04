// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import java.io.IOException;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Runs the {@link SymbolicBlockWorker} of one block like a worker of distributed summary synthesis:
 * in a thread of its own, with a solver context of its own, reading from its own message queue and
 * processing one message at a time.
 */
final class SymbolicBlockActor implements DssActor {

  private final SymbolicBlockWorker worker;
  private final DssConnection connection;
  private final SymbolicBlockScheduler.AnalysisFactory analysisFactory;
  private final DssMessageFactory messageFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  private @Nullable SymbolicBlockAnalysis analysis;

  SymbolicBlockActor(
      SymbolicBlockWorker pWorker,
      DssConnection pConnection,
      SymbolicBlockScheduler.AnalysisFactory pAnalysisFactory,
      DssMessageFactory pMessageFactory,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) {
    worker = pWorker;
    connection = pConnection;
    analysisFactory = pAnalysisFactory;
    messageFactory = pMessageFactory;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
  }

  @Override
  public void run() {
    final DssConnection ownConnection = connection;
    try (ownConnection;
        SymbolicBlockAnalysis ownAnalysis = analysisFactory.create(shutdownNotifier)) {
      analysis = ownAnalysis;
      broadcast(worker.initialize(ownAnalysis));
      while (!shutdownRequested()) {
        broadcast(processMessage(nextMessage()));
      }
    } catch (InterruptedException e) {
      // Once the analysis is over, interrupting the remaining actors is how they are stopped.
      if (!shutdownNotifier.shouldShutdown()) {
        report(e);
      }
    } catch (CPAException
        | SolverException
        | InvalidConfigurationException
        | IOException
        | RuntimeException e) {
      if (!shutdownNotifier.shouldShutdown()) {
        logger.logDebugException(e, "Analysis of block " + getId() + " failed");
        report(e);
      }
    } finally {
      analysis = null;
    }
  }

  private void report(Exception pException) {
    connection
        .getBroadcaster()
        .broadcastToAll(messageFactory.createDssExceptionMessage(getId(), pException));
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage)
      throws InterruptedException, SolverException {
    // A busy actor only notices an interrupt once its queue is empty, which may never happen.
    shutdownNotifier.shutdownIfNecessary();
    SymbolicBlockAnalysis current = analysis;
    if (current == null) {
      throw new IllegalStateException("Actor of block " + getId() + " is not running");
    }
    return worker.processMessage(current, pMessage);
  }

  @Override
  public void broadcast(Collection<DssMessage> pMessages) {
    DssMessageBroadcaster broadcaster = connection.getBroadcaster();
    for (DssMessage message : pMessages) {
      switch (message.getType()) {
        case POST_CONDITION -> broadcaster.broadcastToIds(message, worker.postconditionReceivers());
        case VIOLATION_CONDITION ->
            broadcaster.broadcastToIds(message, worker.violationConditionReceivers());
        case RESULT, EXCEPTION, WITNESS -> broadcaster.broadcastToAll(message);
      }
    }
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return worker.shutdownRequested();
  }

  @Override
  public String getId() {
    return worker.getBlockId();
  }
}
