// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.util.MessageLogger;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

public class VisualizationWorker extends Worker {

  private final Multimap<String, Message> messages;
  private final MessageLogger messageLogger;

  protected VisualizationWorker(LogManager pLogger, BlockTree pTree, Solver pSolver) {
    super(pLogger);
    messages = ArrayListMultimap.create();
    messageLogger = new MessageLogger(pTree, pSolver);
  }

  @Override
  public Collection<Message> processMessage(Message pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    messages.put(pMessage.getUniqueBlockId(), pMessage);
    logger.log(Level.ALL, pMessage);
    switch (pMessage.getType()) {
      case ERROR_CONDITION:
      case BLOCK_POSTCONDITION:
      case ERROR_CONDITION_UNREACHABLE:
        messageLogger.log(pMessage);
        break;
      case FOUND_RESULT:
      case ERROR:
        messageLogger.log(pMessage);
        while (!connection.isEmpty()) {
          Message message = connection.read();
          messageLogger.log(message);
        }
        shutdown();
        break;
      default:
        throw new AssertionError("Unknown message type: " + pMessage.getType());
    }
    return ImmutableSet.of();
  }
}
