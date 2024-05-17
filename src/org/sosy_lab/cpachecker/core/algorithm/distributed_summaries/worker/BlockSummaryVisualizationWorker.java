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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization.BlockSummaryMessageLogger;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryVisualizationWorker extends BlockSummaryWorker {

  private final BlockSummaryMessageLogger messageLogger;
  private final BlockSummaryConnection connection;
  private final LogManager logger;
  private boolean shutdown = false;

  BlockSummaryVisualizationWorker(
      String id,
      BlockGraph pTree,
      BlockSummaryConnection pConnection,
      BlockSummaryAnalysisOptions pOptions,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(id, pLogger);
    logger = pLogger;
    connection = pConnection;
    messageLogger = new BlockSummaryMessageLogger(pTree, pOptions.getParentConfig());
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
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    messageLogger.log(pMessage);
    boolean stop = false;
    while (connection.hasPendingMessages()) {
      BlockSummaryMessage m = connection.read();
      messageLogger.log(m);
      stop |= m.getType() == MessageType.ERROR || m.getType() == MessageType.FOUND_RESULT;
    }
    if (stop) {
      shutdown = true;
    }
    return ImmutableSet.of();
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
