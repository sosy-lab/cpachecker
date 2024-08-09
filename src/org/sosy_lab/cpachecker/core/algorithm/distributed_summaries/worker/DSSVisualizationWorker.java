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
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization.DSSMessageLogger;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DSSVisualizationWorker extends DSSWorker {

  private final DSSMessageLogger messageLogger;
  private final DSSConnection connection;
  private final LogManager logger;
  private boolean shutdown = false;

  DSSVisualizationWorker(
      String id,
      BlockGraph pTree,
      DSSConnection pConnection,
      DSSOptions pOptions,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(id, pLogger);
    logger = pLogger;
    connection = pConnection;
    messageLogger = new DSSMessageLogger(pTree, pOptions.getParentConfig());
    try {
      messageLogger.logBlockGraph();
    } catch (IOException e) {
      logger.logException(
          Level.WARNING,
          e,
          "VisualizationWorker failed to log the BlockTree. "
              + "The visualization might contain old data or will not work. "
              + "However, the analysis continues normally.");
    }
  }

  @Override
  public Collection<DSSMessage> processMessage(DSSMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    messageLogger.log(pMessage);
    boolean stop = false;
    while (connection.hasPendingMessages()) {
      DSSMessage m = connection.read();
      messageLogger.log(m);
      stop |= m.getType() == MessageType.ERROR || m.getType() == MessageType.FOUND_RESULT;
    }
    if (stop) {
      shutdown = true;
    }
    return ImmutableSet.of();
  }

  @Override
  public DSSConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
