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
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BackwardBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.ForwardBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryAnalysisWorker extends BlockSummaryWorker {

  private final BlockNode block;

  private final BlockAnalysis forwardAnalysis;
  private final BlockAnalysis backwardAnalysis;

  private boolean shutdown;

  private final BlockSummaryConnection connection;

  private final StatTimer forwardAnalysisTime = new StatTimer("Forward Analysis");
  private final StatTimer backwardAnalysisTime = new StatTimer("Backward Analysis");

  /**
   * {@link BlockSummaryAnalysisWorker}s trigger forward and backward analyses to find a
   * verification verdict.
   *
   * @param pId unique id of worker that will be prefixed with 'analysis-worker-'
   * @param pOptions analysis options for distributed analysis
   * @param pConnection unique connection to other actors
   * @param pBlock block where this analysis works on
   * @param pCFA complete CFA of which pBlock is a subgraph
   * @param pSpecification specification that should not be violated
   * @param pShutdownManager handler for unexpected shutdowns
   * @throws CPAException exceptions that are logged
   * @throws InterruptedException thrown if user exits program
   * @throws InvalidConfigurationException thrown if configuration contains unexpected values
   * @throws IOException thrown if socket and/or files are not readable
   */
  BlockSummaryAnalysisWorker(
      String pId,
      BlockSummaryAnalysisOptions pOptions,
      BlockSummaryConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("analysis-worker-" + pId, pOptions);
    block = pBlock;
    connection = pConnection;

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();
    Configuration backwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getBackwardConfiguration()).build();

    Specification backwardSpecification =
        Specification.fromFiles(
            ImmutableSet.of(
                Path.of("config/specification/MainEntry.spc"),
                Path.of("config/specification/TerminatingFunctions.spc")),
            pCFA,
            backwardConfiguration,
            getLogger(),
            pShutdownManager.getNotifier());

    forwardAnalysis =
        new ForwardBlockAnalysis(
            getLogger(),
            pBlock,
            pCFA,
            pSpecification,
            forwardConfiguration,
            pShutdownManager,
            pOptions);

    backwardAnalysis =
        new BackwardBlockAnalysis(
            getLogger(),
            pBlock,
            pCFA,
            backwardSpecification,
            backwardConfiguration,
            pShutdownManager,
            pOptions);
    /*
    addTimer(forwardAnalysis);
    addTimer(backwardAnalysis);

    stats.forwardTimer.register(forwardAnalysisTime);
    stats.backwardTimer.register(backwardAnalysisTime);*/
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage message)
      throws InterruptedException, CPAException, IOException, SolverException {
    switch (message.getType()) {
      case ERROR_CONDITION:
        return processErrorCondition(message);
      case BLOCK_POSTCONDITION:
        return processBlockPostCondition(message);
      case ERROR:
        // fall through
      case FOUND_RESULT:
        shutdown = true;
        return ImmutableSet.of();
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  private Collection<BlockSummaryMessage> processBlockPostCondition(BlockSummaryMessage message)
      throws CPAException, InterruptedException, SolverException {
    BlockSummaryMessageProcessing processing =
        forwardAnalysis.getDistributedCPA().getProceedOperator().proceed(message);
    if (processing.end()) {
      return processing;
    }
    return forwardAnalysis(processing);
  }

  private Collection<BlockSummaryMessage> processErrorCondition(BlockSummaryMessage message)
      throws SolverException, InterruptedException, CPAException {
    DistributedCompositeCPA distributed = backwardAnalysis.getDistributedCPA();
    BlockSummaryMessageProcessing processing = distributed.getProceedOperator().proceed(message);
    if (processing.end()) {
      return processing;
    }
    return backwardAnalysis(processing);
  }

  // return post condition
  private Collection<BlockSummaryMessage> forwardAnalysis(
      Collection<BlockSummaryMessage> pPostConditionMessages)
      throws CPAException, InterruptedException, SolverException {
    forwardAnalysisTime.start();
    forwardAnalysis
        .getDistributedCPA()
        .getProceedOperator()
        .synchronizeKnowledge(backwardAnalysis.getDistributedCPA());
    // stats.forwardAnalysis.inc();
    Collection<BlockSummaryMessage> response = forwardAnalysis.analyze(pPostConditionMessages);
    forwardAnalysisTime.stop();
    return response;
  }

  // return pre-condition
  protected Collection<BlockSummaryMessage> backwardAnalysis(
      BlockSummaryMessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    assert pMessageProcessing.size() == 1 : "BackwardAnalysis can only be based on one message";
    backwardAnalysisTime.start();
    backwardAnalysis
        .getDistributedCPA()
        .getProceedOperator()
        .synchronizeKnowledge(forwardAnalysis.getDistributedCPA());
    // stats.backwardAnalysis.inc();
    Collection<BlockSummaryMessage> response = backwardAnalysis.analyze(pMessageProcessing);
    backwardAnalysisTime.stop();
    return response;
  }

  @Override
  public void run() {
    try {
      broadcast(forwardAnalysis.performInitialAnalysis());
      super.run();
    } catch (CPAException pE) {
      getLogger().logException(Level.SEVERE, pE, "Worker stopped working...");
      broadcastOrLogException(
          ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), pE)));
    } catch (InterruptedException pE) {
      getLogger().logException(Level.SEVERE, pE, "Thread interrupted unexpectedly.");
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  /*  private void addTimer(BlockAnalysis pBlockAnalysis) {
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedSerializeTime, StatTimerType.SERIALIZE);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedDeserializeTime, StatTimerType.DESERIALIZE);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedForwardTime, StatTimerType.PROCEED_F);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedBackwardTime, StatTimerType.PROCEED_B);
    pBlockAnalysis
        .getDistributedCPA()
        .registerTimer(stats.proceedCombineTime, StatTimerType.COMBINE);
  }*/

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + shutdownRequested() + '}';
  }

  public BlockAnalysis getForwardAnalysis() {
    return forwardAnalysis;
  }
}
