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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization.DssMessageLogger;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DssVisualizationWorker extends DssWorker {

  private final DssMessageLogger messageLogger;
  private final DssConnection connection;
  private boolean shutdown = false;

  DssVisualizationWorker(
      String id,
      BlockGraph pTree,
      DssConnection pConnection,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(id, pMessageFactory, pLogger);
    connection = pConnection;
    messageLogger = new DssMessageLogger(pTree, pOptions.getParentConfig());
    try {
      messageLogger.logBlockGraph();
    } catch (IOException e) {
      pLogger.logException(
          Level.WARNING,
          e,
          "VisualizationWorker failed to log the BlockTree. "
              + "The visualization might contain old data or will not work. "
              + "However, the analysis continues normally.");
    }
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    messageLogger.log(pMessage);
    boolean stop = false;
    while (connection.hasPendingMessages()) {
      DssMessage m = connection.read();
      messageLogger.log(m);
      stop |= m.getType() == MessageType.ERROR || m.getType() == MessageType.FOUND_RESULT;
    }
    if (stop) {
      shutdown = true;
    }
    return ImmutableSet.of();
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
