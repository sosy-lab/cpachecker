// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.SolverException;

public class AnalysisWorker extends Worker {

  protected final BlockNode block;

  protected final BlockAnalysis forwardAnalysis;
  protected final BlockAnalysis backwardAnalysis;

  AnalysisWorker(
      String pId,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      SSAMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super("analysis-worker-" + pBlock.getId(), pLogger);
    block = pBlock;

    Configuration backwardConfiguration = Configuration.builder()
        .copyFrom(pConfiguration)
        .loadFromFile(
            "config/includes/predicateAnalysisBackward.properties")
        .clearOption("analysis.initialStatesFor")
        .setOption("analysis.initialStatesFor", "TARGET")
        .setOption("CompositeCPA.cpas",
            "cpa.location.LocationCPABackwards, cpa.block.BlockCPABackward, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
        .setOption("backwardSpecification", "config/specification/MainEntry.spc")
        .setOption("specification", "config/specification/MainEntry.spc")
        .setOption("cpa.predicate.abstractAtTargetState", "false")
        .setOption("cpa.predicate.blk.alwaysAtJoin", "false")
        .setOption("cpa.predicate.blk.alwaysAtBranch", "false")
        .setOption("cpa.predicate.blk.alwaysAtProgramExit", "false")
        .setOption("cpa.predicate.blk.alwaysAfterThreshold", "false")
        .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
        .build();

    Specification backwardSpecification =
        Specification.fromFiles(ImmutableSet.of(Path.of("config/specification/MainEntry.spc")),
            pCFA, backwardConfiguration, logger, pShutdownManager.getNotifier());

    Configuration forwardConfiguration =
        Configuration.builder().copyFrom(pConfiguration).setOption("CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.block.BlockCPA, cpa.predicate.PredicateCPA")
            .setOption("cpa.predicate.blk.alwaysAtJoin", "false")
            .setOption("cpa.predicate.blk.alwaysAtBranch", "false")
            .setOption("cpa.predicate.blk.alwaysAtProgramExit", "false")
            .setOption("cpa.predicate.blk.alwaysAfterThreshold", "false")
            .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
            .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
            .build();

    forwardAnalysis = new ForwardAnalysis(pId, pLogger, pBlock, pCFA, pTypeMap, pSpecification,
        forwardConfiguration,
        pShutdownManager);

    backwardAnalysis = new BackwardAnalysis(pId, pLogger, pBlock, pCFA,
        pTypeMap, backwardSpecification,
        backwardConfiguration, pShutdownManager);
  }

  @Override
  public Collection<Message> processMessage(Message message)
      throws InterruptedException, CPAException, IOException, SolverException {
    switch (message.getType()) {
      case ERROR_CONDITION:
        return processErrorCondition(message);
      case BLOCK_POSTCONDITION:
        return processBlockPostCondition(message);
      case ERROR:
      case FOUND_RESULT:
        shutdown();
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  private Collection<Message> processBlockPostCondition(Message message)
      throws CPAException, InterruptedException, SolverException {
    MessageProcessing processing = forwardAnalysis.getDistributedCPA().proceed(message);
    if (processing.end()) {
      return processing;
    }
    return forwardAnalysis(processing);
  }

  private Collection<Message> processErrorCondition(Message message)
      throws SolverException, InterruptedException, CPAException {
    DistributedCompositeCPA distributed = backwardAnalysis.getDistributedCPA();
    MessageProcessing processing = distributed.proceed(message);
    if(processing.end()) {
      return processing;
    }
    return backwardAnalysis(processing);
  }

  // return post condition
  private Collection<Message> forwardAnalysis(Collection<Message> pPostConditionMessages)
      throws CPAException, InterruptedException, SolverException {
    return forwardAnalysis.analyze(pPostConditionMessages);
  }

  // return pre-condition
  protected Collection<Message> backwardAnalysis(MessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    assert pMessageProcessing.size() == 1 : "BackwardAnalysis can only be based on one message";
    return backwardAnalysis.analyze(pMessageProcessing);
  }

  @Override
  public void run() {
    try {
      if (!block.isSelfCircular() && !block.getPredecessors().isEmpty()) {
        List<Message> initialMessages = ImmutableList.copyOf(forwardAnalysis(ImmutableSet.of(
            Message.newBlockPostCondition("", block.getStartNode().getNodeNumber(), Payload.empty(),
                false, ImmutableSet.of()))));
        if (initialMessages.size() == 1) {
          Message message = initialMessages.get(0);
          if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
            forwardAnalysis.getDistributedCPA().setFirstMessage(message);
          }
        }
        broadcast(initialMessages);
      }
      super.run();
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      logger.log(Level.SEVERE, "Worker run into an error: %s", pE);
      logger.log(Level.SEVERE, "Stopping analysis...");
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

}
