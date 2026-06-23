// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Observer worker that detects termination conditions based on the received messages.
 *
 * <p>DssObserverWorker detects a termination condition when:
 *
 * <ul>
 *   <li>All blocks report that no violations are reachable (SAFE verdict)
 *   <li>A root block reports a reachable violation (UNSAFE verdict)
 *   <li>An exception occurs during analysis
 * </ul>
 *
 * To function correctly, this DssObserverWorker must receive the messages of all analysis workers.
 *
 * <p>The observer also collects statistics from all workers for analysis reporting.
 */
public class DssObserverWorker extends DssWorker {

  private final DssConnection connection;
  private final StatusObserver statusObserver;
  private boolean shutdown;
  private Optional<Result> result;
  private Optional<String> errorMessage;

  private final Map<String, DssStatisticsMessage> stats = new HashMap<>();

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
      case RESULT -> {
        result = Optional.of(pMessage.getResult());
        statusObserver.updateStatus(pMessage);
      }
      case VIOLATION_CONDITION, POST_CONDITION -> statusObserver.updateStatus(pMessage);
      case EXCEPTION -> {
        errorMessage = Optional.of(pMessage.getExceptionMessage());
        shutdown = true;
      }
      case STATISTIC -> {
        stats.put(pMessage.getSenderId(), (DssStatisticsMessage) pMessage);
        shutdown = stats.size() == numberOfBlocks;
      }
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

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  public static class StatusObserver {

    private final Map<String, AlgorithmStatus> statusMap;

    private StatusObserver() {
      statusMap = new HashMap<>();
    }

    private void updateStatus(DssMessage pMessage) {
      switch (pMessage.getType()) {
        case VIOLATION_CONDITION, POST_CONDITION ->
            statusMap.put(pMessage.getSenderId(), pMessage.getAlgorithmStatus());
        case RESULT, EXCEPTION, STATISTIC -> {}
      }
    }

    private AlgorithmStatus finish() {
      return statusMap.values().stream()
          .reduce(AlgorithmStatus::update)
          .orElse(AlgorithmStatus.NO_PROPERTY_CHECKED);
    }
  }

  public ImmutableMap<String, DssStatisticsMessage> getCollectedStats() {
    return ImmutableMap.copyOf(stats);
  }
}
