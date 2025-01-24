// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssResultMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssStatisticsMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DssObserverWorker extends DssWorker {

  private final DssConnection connection;
  private final StatusObserver statusObserver;
  private boolean shutdown;
  private Optional<Result> result;
  private Optional<String> errorMessage;

  private final Map<String, Map<String, Object>> stats = new HashMap<>();

  private final int numberOfBlocks;

  public record StatusAndResult(AlgorithmStatus status, Result result) {}

  public DssObserverWorker(
      String pId,
      DssConnection pConnection,
      int pNumberOfBlocks,
      DssMessageFactory pMessageFactory,
      LogManager pLogger) {
    super(pId, pMessageFactory, pLogger);
    shutdown = false;
    connection = pConnection;
    statusObserver = new StatusObserver();
    errorMessage = Optional.empty();
    result = Optional.empty();
    numberOfBlocks = pNumberOfBlocks;
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage) {
    switch (pMessage.getType()) {
      case FOUND_RESULT -> {
        result = Optional.of(((DssResultMessage) pMessage).getResult());
        statusObserver.updateStatus(pMessage);
      }
      case VIOLATION_CONDITION, BLOCK_POSTCONDITION -> statusObserver.updateStatus(pMessage);
      case ERROR -> {
        shutdown = true;
        errorMessage = Optional.of(((DssExceptionMessage) pMessage).getErrorMessage());
      }
      case STATISTICS -> {
        stats.put(pMessage.getBlockId(), ((DssStatisticsMessage) pMessage).getStats());
        shutdown = stats.keySet().size() == numberOfBlocks;
      }
      default -> throw new AssertionError("Unknown message type: " + pMessage.getType());
    }
    return ImmutableList.of();
  }

  public StatusAndResult observe() throws CPAException {
    super.run();
    if (errorMessage.isPresent()) {
      throw new CPAException(errorMessage.orElseThrow());
    }
    if (result.isEmpty()) {
      throw new CPAException("Analysis finished but no result is present...");
    }
    return new StatusAndResult(statusObserver.finish(), result.orElseThrow());
  }

  public Map<String, Map<String, Object>> getStats() {
    return stats;
  }

  @Override
  public DssConnection getConnection() {
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

    private void updateStatus(DssMessage pMessage) {
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
