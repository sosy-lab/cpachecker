// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.NoopAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class RootWorker extends Worker {

  private final BlockNode root;
  private final BlockAnalysis analysis;

  public RootWorker(String pId, BlockNode pNode, LogManager pLogger, CFA pCfa, Specification pSpecification, Configuration pConfiguration, ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pLogger);
    root = pNode;
    if (!root.isRoot() || !root.isEmpty() || !root.getLastNode().equals(root.getStartNode())) {
      throw new AssertionError("Root nodes must be empty and do not have predecessors: " + pNode);
    }
    analysis = new NoopAnalysis(pId, logger, pNode, pCfa, AnalysisDirection.FORWARD, pSpecification, pConfiguration, pShutdownManager);
  }

  @Override
  public Collection<Message> processMessage(
      Message pMessage) throws InterruptedException, IOException, SolverException, CPAException {
    switch (pMessage.getType()) {
      case ERROR_CONDITION:
        if (pMessage.getTargetNodeNumber() == root.getLastNode().getNodeNumber()
            && root.getSuccessors().stream()
            .anyMatch(block -> block.getId().equals(pMessage.getUniqueBlockId()))) {
          BooleanFormula formula = analysis.getFmgr().parse(pMessage.getPayload());
          if (analysis.getSolver().isUnsat(formula)) {
            return ImmutableSet.of(Message.newErrorConditionUnreachableMessage(root.getId()));
          } else {
            return ImmutableSet.of(Message.newResultMessage(root.getId(), 0, Result.FALSE));
          }
        }
        return ImmutableSet.of();
      case FOUND_RESULT:
      case ERROR:
        shutdown();
      case BLOCK_POSTCONDITION:
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("Unknown MessageType " + pMessage.getType());
    }
  }

  @Override
  public void run() {
    try {
      broadcast(ImmutableSet.of(
          Message.newBlockPostCondition(root.getId(), root.getLastNode().getNodeNumber(),
              analysis.getBmgr().makeTrue(), analysis.getFmgr(), true)));
      super.run();
    } catch (InterruptedException | IOException pE) {
      logger.log(Level.SEVERE, "Worker run into an error: %s", pE);
      logger.log(Level.SEVERE, "Stopping analysis...");
    }
  }
}
