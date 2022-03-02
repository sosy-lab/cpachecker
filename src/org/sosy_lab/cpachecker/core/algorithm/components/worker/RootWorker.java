// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.SolverException;

public class RootWorker extends Worker {

  private final BlockNode root;
  private final BlockAnalysis analysis;

  public RootWorker(
      String pId,
      AnalysisOptions pOptions,
      BlockNode pNode,
      LogManager pLogger,
      CFA pCfa,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super("root-worker", pLogger, pOptions);
    root = pNode;
    if (!root.isRoot() || !root.isEmpty() || !root.getLastNode().equals(root.getStartNode())) {
      throw new AssertionError("Root nodes must be empty and do not have predecessors: " + pNode);
    }
    analysis =
        new NoopAnalysis(pId, logger, pNode, pCfa, new UpdatedTypeMap(SSAMap.emptySSAMap()),
            AnalysisDirection.FORWARD,
            pSpecification, pConfiguration, pShutdownManager, pOptions);
  }

  @Override
  public Collection<Message> processMessage(
      Message pMessage) throws InterruptedException, IOException, SolverException, CPAException {
    switch (pMessage.getType()) {
      case ERROR_CONDITION:
        if (pMessage.getTargetNodeNumber() == root.getLastNode().getNodeNumber()
            && root.getSuccessors().stream()
            .anyMatch(block -> block.getId().equals(pMessage.getUniqueBlockId()))) {
          MessageProcessing processing = analysis.getDistributedCPA().proceedBackward(pMessage);
          if (processing.end()) {
            return processing;
          }
          return ImmutableSet.of(
              Message.newResultMessage(root.getId(), root.getLastNode().getNodeNumber(),
                  Result.FALSE, new HashSet<>(Splitter.on(",")
                      .splitToList(pMessage.getPayload().getOrDefault(Payload.VISITED, "")))));
        }
        return ImmutableSet.of();
      case FOUND_RESULT:
        // fall through
      case ERROR:
        shutdown();
        return ImmutableSet.of();
      case BLOCK_POSTCONDITION:
        // fall through
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("Unknown MessageType " + pMessage.getType());
    }
  }

  @Override
  public void run() {
    try {
      Collection<Message> initialMessage = analysis.initialAnalysis();
      analysis.getDistributedCPA().setLatestOwnPostConditionMessage(initialMessage.stream().findAny().orElseThrow());
      broadcast(initialMessage);
      super.run();
    } catch (InterruptedException | IOException | CPAException pE) {
      logger.log(Level.SEVERE, "Worker run into an error: %s", pE);
      logger.log(Level.SEVERE, "Stopping analysis...");
      throw new AssertionError(pE);
    }
  }
}
