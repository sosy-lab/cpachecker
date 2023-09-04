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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.java_smt.api.SolverException;

public class InferRootWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private boolean shutdown;
  private int strengthenCounter;
  private final String functionEntry;
  private Optional<Integer> expectedStrengthens;
  private MessageType lastReceivedType;
  private int currentMaxRun;

  private static final String VIOLATION_PATHS = "violation_paths";

  public InferRootWorker(
      String pId, BlockSummaryConnection pConnection, InferOptions pOptions, String pFunctionEntry)
      throws InvalidConfigurationException {
    super("infer-root-worker-" + pId, new BlockSummaryAnalysisOptions(pOptions.getParentConfig()));
    connection = pConnection;
    shutdown = false;
    functionEntry = pFunctionEntry;
    expectedStrengthens = Optional.empty();
    strengthenCounter = 0;
    lastReceivedType = MessageType.BLOCK_POSTCONDITION;
    currentMaxRun = 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException, IOException {

    if (!messageFromEntryFunction(pMessage)) {
      return ImmutableSet.of();
    }

    return switch (pMessage.getType()) {
      case ERROR_CONDITION, BLOCK_POSTCONDITION -> {
        strengthenCounter++;
        int runOrder = (int) pMessage.getPayload().get(InferDCPAAlgorithm.RUN_ORDER);
        if (runOrder >= currentMaxRun) {
          lastReceivedType = pMessage.getType();
          currentMaxRun = runOrder;
        }

        if (expectedStrengthens.isPresent()
            && strengthenCounter >= expectedStrengthens.orElseThrow()) {
          shutdown = true;
          yield ImmutableSet.of(resultMessage());
        }
        yield ImmutableSet.of();
      }
      case INFER_ACKNOWLEDGMENT -> {
        int totalMainMessages = (int) pMessage.getPayload().get(InferWorker.TOTAL_BLOCK_MESSAGES);
        expectedStrengthens = Optional.of(totalMainMessages);
        if (strengthenCounter >= expectedStrengthens.orElseThrow()) {
          shutdown = true;
          yield ImmutableSet.of(resultMessage());
        }

        yield ImmutableSet.of();
      }
      default -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
    };
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

  private BlockSummaryMessage resultMessage() {
    return lastReceivedType == MessageType.ERROR_CONDITION
        ? violationResult(ImmutableSet.of())
        : proofResult();
  }
}
