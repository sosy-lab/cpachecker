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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization.MessageLogger;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class VisualizationWorker extends Worker {

  private final MessageLogger messageLogger;

  protected VisualizationWorker(
      BlockTree pTree, AnalysisOptions pOptions, Configuration pConfiguration)
      throws InvalidConfigurationException {
    super("visualization-worker", pOptions);
    messageLogger = new MessageLogger(pTree, pConfiguration);
    try {
      messageLogger.logTree();
    } catch (IOException pE) {
      logger.log(Level.WARNING, "Logger was not able to print the tree to a file because of " + pE);
    }
  }

  @Override
  public Collection<Message> processMessage(Message pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    messageLogger.log(pMessage);
    boolean stop = false;
    while (!connection.isEmpty()) {
      Message m = connection.read();
      messageLogger.log(m);
      stop |= m.getType() == MessageType.ERROR || m.getType() == MessageType.FOUND_RESULT;
    }
    if (stop) {
      shutdown();
    }
    return ImmutableSet.of();
  }
}
