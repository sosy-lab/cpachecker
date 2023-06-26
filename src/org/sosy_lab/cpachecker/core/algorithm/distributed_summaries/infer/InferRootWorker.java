// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.java_smt.api.SolverException;

public class InferRootWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private boolean shutdown;
  private final int numWorkers;
  private Set<String> workerResults;
  private boolean isProof;

  public InferRootWorker(
      String pId,
      BlockSummaryConnection pConnection,
      BlockSummaryAnalysisOptions pOptions,
      int pNumWorkers) {
    super("infer-root-worker-" + pId, pOptions);
    numWorkers = pNumWorkers; // TODO MAKE THIS REFLECT USE
    // TODO LIST PATHS (LIST OF NODE LIST)
    workerResults = ImmutableSet.of();
    connection = pConnection;
    isProof = true;
    shutdown = false;
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException {
    switch (pMessage.getType()) {
      case ERROR_CONDITION -> {
        workerResults.add(pMessage.getUniqueBlockId());
        isProof = false;
      }
      case BLOCK_POSTCONDITION -> {
        workerResults.add(pMessage.getUniqueBlockId());
      }
      default -> {}
    }
    if (workerResults.size() == numWorkers) {
      // TODO add actual results to the payload body
      shutdown = true;
      return ImmutableSet.of(isProof ? proofResult() : violationResult());
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

  private BlockSummaryMessage violationResult() {
    return InferRootViolationsMessage.newInferRootViolations(
        "root", 0, BlockSummaryMessagePayload.empty());
  }
}
