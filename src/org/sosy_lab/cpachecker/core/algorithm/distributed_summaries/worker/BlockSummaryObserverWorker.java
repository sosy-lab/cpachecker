// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryResultMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryObserverWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private final StatusObserver statusObserver;
  private boolean shutdown;
  private Optional<Result> result;
  private Optional<String> errorMessage;

  public BlockSummaryObserverWorker(
      String pId, BlockSummaryConnection pConnection, BlockSummaryAnalysisOptions pOptions) {
    super(pId, pOptions);
    shutdown = false;
    connection = pConnection;
    statusObserver = new StatusObserver();
    errorMessage = Optional.empty();
    result = Optional.empty();
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    switch (pMessage.getType()) {
      case FOUND_RESULT:
        shutdown = true;
        result = Optional.of(((BlockSummaryResultMessage) pMessage).getResult());
        statusObserver.updateStatus(pMessage);
        break;
      case ERROR_CONDITION_UNREACHABLE:
        // fall-through
      case ERROR_CONDITION:
        // fall-through
      case BLOCK_POSTCONDITION:
        statusObserver.updateStatus(pMessage);
        break;
      case ERROR:
        errorMessage = Optional.of(((BlockSummaryErrorMessage) pMessage).getErrorMessage());
        shutdown = true;
        break;
      default:
        throw new AssertionError("Unknown message type: " + pMessage.getType());
    }
    return ImmutableList.of();
  }

  public Pair<AlgorithmStatus, Result> observe() throws CPAException {
    super.run();
    if (errorMessage.isPresent()) {
      throw new CPAException(errorMessage.orElseThrow());
    }
    if (result.isEmpty()) {
      throw new CPAException("Analysis finished but no result is present...");
    }
    return Pair.of(statusObserver.finish(), result.orElseThrow());
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  public static class StatusObserver {

    public enum StatusSoundness {
      SOUND,
      UNSOUND
    }

    public enum StatusPropertyChecked {
      CHECKED,
      UNCHECKED
    }

    public enum StatusPrecise {
      PRECISE,
      IMPRECISE
    }

    private final Map<String, AlgorithmStatus> statusMap;

    private StatusObserver() {
      statusMap = new HashMap<>();
    }

    private void updateStatus(BlockSummaryMessage pMessage) {
      pMessage
          .getOptionalStatus()
          .ifPresent(status -> statusMap.put(pMessage.getUniqueBlockId(), status));
    }

    private AlgorithmStatus finish() {
      return statusMap.values().stream()
          .reduce(AlgorithmStatus::update)
          .orElse(AlgorithmStatus.NO_PROPERTY_CHECKED);
    }
  }
}
