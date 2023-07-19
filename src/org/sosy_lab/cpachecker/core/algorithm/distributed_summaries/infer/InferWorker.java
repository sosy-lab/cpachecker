// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class InferWorker extends BlockSummaryWorker {

  private final BlockNode block;

  private final InferDCPAAlgorithm dcpaAlgorithm;

  private boolean shutdown;

  private final BlockSummaryConnection connection;

  private final String blockEntryFunction;

  /**
   * {@link InferWorker}s trigger CEGAR refinement using forward and backward analyses to find a
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
  public InferWorker(
      String pId,
      InferOptions pOptions,
      BlockSummaryConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("infer-worker-" + pId, new BlockSummaryAnalysisOptions(pOptions.getParentConfig()));
    block = pBlock;
    connection = pConnection;
    blockEntryFunction = block.getFirst().getFunctionName();

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();

    dcpaAlgorithm =
        new InferDCPAAlgorithm(
            getLogger(), pBlock, pCFA, pSpecification, forwardConfiguration, pShutdownManager);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage message)
      throws InterruptedException, CPAException, SolverException {

    ImmutableSet<String> calledFunctions =
        (ImmutableSet<String>)
            message
                .getPayload()
                .getOrDefault(InferDCPAAlgorithm.CALLED_FUNCTIONS, ImmutableSet.of());
    if (!calledFunctions.contains(blockEntryFunction)) {
      return ImmutableSet.of();
    }

    switch (message.getType()) {
      case ERROR_CONDITION -> {
        AbstractState state = dcpaAlgorithm.getDCPA().getDeserializeOperator().deserialize(message);
        dcpaAlgorithm.runAnalysisUnderCondition(Optional.of(state));
      }
      default -> {}
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

  @Override
  public void run() {
    try {
      Collection<BlockSummaryMessage> messages = dcpaAlgorithm.runInitialAnalysis();
      broadcast(messages);
      super.run();
    } catch (CPAException e) {
      getLogger().logException(Level.SEVERE, e, "Worker stopped working...");
      broadcastOrLogException(
          ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), e)));
    } catch (InterruptedException e) {
      getLogger().logException(Level.SEVERE, e, "Thread interrupted unexpectedly.");
    } finally {
      shutdown = true;
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{block=" + block + ", finished=" + shutdownRequested() + '}';
  }
}
