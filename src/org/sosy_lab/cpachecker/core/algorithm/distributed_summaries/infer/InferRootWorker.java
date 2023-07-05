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
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.SerializeARGStateOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.java_smt.api.SolverException;

public class InferRootWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private boolean shutdown;
  private final int numBlocks;
  private Set<String> workerResults;
  private ImmutableSet.Builder<ImmutableSet<CFANode>> violationPathsBuilder;

  private final String VIOLATION_PATHS = "violation_paths";

  public InferRootWorker(
      String pId, BlockSummaryConnection pConnection, InferOptions pOptions, int pNumBlocks)
      throws InvalidConfigurationException {
    super("infer-root-worker-" + pId, new BlockSummaryAnalysisOptions(pOptions.getParentConfig()));
    numBlocks = pNumBlocks;
    violationPathsBuilder = ImmutableSet.builder();
    workerResults = new HashSet<>();
    connection = pConnection;
    shutdown = false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException, IOException {
    switch (pMessage.getType()) {
      case ERROR_CONDITION -> {
        BlockSummaryMessagePayload payload = pMessage.getPayload();
        if (payload.containsKey(SerializeARGStateOperator.COUNTEREXAMPLE_PATH)) {
          Object obj = payload.get(SerializeARGStateOperator.COUNTEREXAMPLE_PATH);
          ImmutableSet<CFANode> path = (ImmutableSet<CFANode>) obj;
          violationPathsBuilder.add(path);
        }
        workerResults.add(pMessage.getUniqueBlockId());
      }
      case BLOCK_POSTCONDITION -> {
        workerResults.add(pMessage.getUniqueBlockId());
      }
      default -> {}
    }
    if (workerResults.size() == numBlocks) {

      shutdown = true;
      ImmutableSet<ImmutableSet<CFANode>> violationPaths = violationPathsBuilder.build();
      if (violationPaths.isEmpty()) {
        return ImmutableSet.of(proofResult());
      } else {
        return ImmutableSet.of(violationResult(violationPaths));
      }
    } else {
      return ImmutableSet.of();
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

  @Override
  public void run() {
    super.run();
  }

  private BlockSummaryMessage proofResult() {
    return InferRootProofMessage.newInferRootProof("root", 0, BlockSummaryMessagePayload.empty());
  }

  private BlockSummaryMessage violationResult(ImmutableSet<ImmutableSet<CFANode>> pViolationPaths) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(VIOLATION_PATHS, pViolationPaths)
            .buildPayload();
    return InferRootViolationsMessage.newInferRootViolations("root", 0, payload);
  }
}
