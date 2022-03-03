// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.StatTimerSum.StatTimerType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.api.SolverException;

public class AnalysisWorker extends Worker {

  protected final BlockNode block;

  protected final BlockAnalysis forwardAnalysis;
  protected final BlockAnalysis backwardAnalysis;

  private final StatTimer forwardAnalysisTime = new StatTimer("Forward Analysis");
  private final StatTimer backwardAnalysisTime = new StatTimer("Backward Analysis");

  AnalysisWorker(
      String pId,
      AnalysisOptions pOptions,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager,
      UpdatedTypeMap pTypeMap)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("analysis-worker-" + pBlock.getId(), pLogger, pOptions);
    block = pBlock;

    Configuration forwardConfiguration = Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();
    Configuration backwardConfiguration = Configuration.builder().loadFromFile(pOptions.getBackwardConfiguration()).build();

    Specification backwardSpecification =
        Specification.fromFiles(ImmutableSet.of(Path.of("config/specification/MainEntry.spc"),
                Path.of("config/specification/TerminatingFunctions.spc")),
            pCFA, backwardConfiguration, logger, pShutdownManager.getNotifier());

    forwardAnalysis = new ForwardAnalysis(pId, pLogger, pBlock, pCFA, pTypeMap, pSpecification,
        forwardConfiguration,
        pShutdownManager, pOptions);

    backwardAnalysis = new BackwardAnalysis(pId, pLogger, pBlock, pCFA,
        pTypeMap, backwardSpecification,
        backwardConfiguration, pShutdownManager, pOptions);

    addTimer(forwardAnalysis);
    addTimer(backwardAnalysis);

    stats.forwardTimer.register(forwardAnalysisTime);
    stats.backwardTimer.register(backwardAnalysisTime);
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
        // fall through
      case FOUND_RESULT:
        shutdown();
        return ImmutableSet.of();
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
    if (processing.end()) {
      return processing;
    }
    return backwardAnalysis(processing);
  }

  // return post condition
  private Collection<Message> forwardAnalysis(Collection<Message> pPostConditionMessages)
      throws CPAException, InterruptedException, SolverException {
    forwardAnalysisTime.start();
    forwardAnalysis.getDistributedCPA().synchronizeKnowledge(backwardAnalysis.getDistributedCPA());
    stats.forwardAnalysis.inc();
    Collection<Message> response = forwardAnalysis.analyze(pPostConditionMessages);
    forwardAnalysisTime.stop();
    return response;
  }

  // return pre-condition
  protected Collection<Message> backwardAnalysis(MessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    assert pMessageProcessing.size() == 1 : "BackwardAnalysis can only be based on one message";
    backwardAnalysisTime.start();
    backwardAnalysis.getDistributedCPA().synchronizeKnowledge(forwardAnalysis.getDistributedCPA());
    stats.backwardAnalysis.inc();
    Collection<Message> response = backwardAnalysis.analyze(pMessageProcessing);
    backwardAnalysisTime.stop();
    return response;
  }

  @Override
  public void run() {
    try {
      broadcast(forwardAnalysis.initialAnalysis());
      super.run();
    } catch (CPAException | InterruptedException | IOException pE) {
      logger.log(Level.SEVERE, "Worker run into an error: %s", pE);
      logger.log(Level.SEVERE, "Stopping analysis...");
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  private void addTimer(BlockAnalysis pBlockAnalysis) {
    pBlockAnalysis.getDistributedCPA().registerTimer(stats.proceedSerializeTime, StatTimerType.SERIALIZE);
    pBlockAnalysis.getDistributedCPA().registerTimer(stats.proceedDeserializeTime, StatTimerType.DESERIALIZE);
    pBlockAnalysis.getDistributedCPA().registerTimer(stats.proceedForwardTime, StatTimerType.PROCEED_F);
    pBlockAnalysis.getDistributedCPA().registerTimer(stats.proceedBackwardTime, StatTimerType.PROCEED_B);
    pBlockAnalysis.getDistributedCPA().registerTimer(stats.proceedCombineTime, StatTimerType.COMBINE);
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

}
