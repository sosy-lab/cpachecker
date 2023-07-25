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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.java_smt.api.SolverException;

public class InferRootWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private boolean shutdown;
  // private ImmutableSet.Builder<ImmutableSet<CFANode>> violationPathsBuilder;
  // private Map<String, ImmutableSet<CFANode>> violationPaths;
  private final int expectedStrengthens;
  private int strengthenCounter;
  private final String functionEntry;

  private static final String VIOLATION_PATHS = "violation_paths";

  public InferRootWorker(
      String pId,
      BlockSummaryConnection pConnection,
      InferOptions pOptions,
      int pNumBlocks,
      int pExpectedStrengthens,
      String pFunctionEntry)
      throws InvalidConfigurationException {
    super("infer-root-worker-" + pId, new BlockSummaryAnalysisOptions(pOptions.getParentConfig()));
    // violationPathsBuilder = ImmutableSet.builder();
    connection = pConnection;
    shutdown = false;
    functionEntry = pFunctionEntry;
    if(pNumBlocks == 1) {
      expectedStrengthens = 1;
    } else {
      expectedStrengthens = pExpectedStrengthens + 1;
    }
    strengthenCounter = 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException, IOException {

    if (!messageFromEntryFunction(pMessage)) {
      return ImmutableSet.of();
    }

    strengthenCounter++;

    if (strengthenCounter < expectedStrengthens) {
      return ImmutableSet.of();
    } else {
      return switch (pMessage.getType()) {
        case ERROR_CONDITION -> {
          // BlockSummaryMessagePayload payload = pMessage.getPayload();
          // if (payload.containsKey(SerializeARGStateOperator.COUNTEREXAMPLE_PATH)) {
          //   Object obj = payload.get(SerializeARGStateOperator.COUNTEREXAMPLE_PATH);
          //   ImmutableSet<CFANode> path = (ImmutableSet<CFANode>) obj;
          //   violationPathsBuilder.add(path);
          //   violationPaths.put(pMessage.getUniqueBlockId(), path);
          // } else {
          //   violationPaths.put(pMessage.getUniqueBlockId(), ImmutableSet.of());
          // }
          yield ImmutableSet.of(violationResult(ImmutableSet.of()));
        }
        case BLOCK_POSTCONDITION -> {
          // violationPaths.remove(pMessage.getUniqueBlockId());
          yield ImmutableSet.of(proofResult());
        }
        default -> {
          // TODO we shouldn't get here. Maybe throw an error
          shutdown = true;
          yield ImmutableSet.of();
        }
      };
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

  private boolean messageFromEntryFunction(BlockSummaryMessage pMessage) {
    String messageFunction =
        (String) pMessage.getPayload().getOrDefault(InferDCPAAlgorithm.MESSAGE_FUNCTION, "");
    return messageFunction.equals(functionEntry);
  }
}
