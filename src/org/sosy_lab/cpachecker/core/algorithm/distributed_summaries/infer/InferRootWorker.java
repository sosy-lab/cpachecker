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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.java_smt.api.SolverException;

public class InferRootWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private boolean shutdown;
  private final int expectedStrengthens;
  private int strengthenCounter;
  private final String functionEntry;

  // A single strengthen can send multiple messages
  // We only want to increment the strengthen counter once per strengthen
  private Set<String> uniqueStrengthens;

  private static final String VIOLATION_PATHS = "violation_paths";

  public InferRootWorker(
      String pId,
      BlockSummaryConnection pConnection,
      InferOptions pOptions,
      int pExpectedStrengthens,
      String pFunctionEntry)
      throws InvalidConfigurationException {
    super("infer-root-worker-" + pId, new BlockSummaryAnalysisOptions(pOptions.getParentConfig()));
    connection = pConnection;
    shutdown = false;
    functionEntry = pFunctionEntry;
    expectedStrengthens = pExpectedStrengthens;
    strengthenCounter = 0;
    uniqueStrengthens = new HashSet<>();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException, IOException {

    if (!messageFromEntryFunction(pMessage) || !isUniqueStrengthn(pMessage)) {
      return ImmutableSet.of();
    }

    strengthenCounter++;

    if (strengthenCounter < expectedStrengthens) {
      return ImmutableSet.of();
    } else {
      return switch (pMessage.getType()) {
        case ERROR_CONDITION -> {
          yield ImmutableSet.of(violationResult(ImmutableSet.of()));
        }
        case BLOCK_POSTCONDITION -> {
          yield ImmutableSet.of(proofResult());
        }
        default -> {
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

  private boolean isUniqueStrengthn(BlockSummaryMessage pMessage) {
    String strengthenUUUID =
        (String) pMessage.getPayload().get(InferDCPAAlgorithm.UNIQUE_STRENGTHEN_ID);
    boolean isUnique = !uniqueStrengthens.contains(strengthenUUUID);
    uniqueStrengthens.add(strengthenUUUID);
    return isUnique;
  }
}
