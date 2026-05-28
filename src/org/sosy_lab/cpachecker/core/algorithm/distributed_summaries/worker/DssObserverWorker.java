// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.DssArgStateCollector;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.ResultWithWitnessInformation;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

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
public class DssObserverWorker extends DssWorker implements Statistics {

  private final DssConnection connection;
  private final StatusObserver statusObserver;
  private boolean shutdown;
  private Optional<ResultWithWitnessInformation> result;
  private Optional<String> errorMessage;

  private final Map<String, Map<StatisticsKey, String>> stats = new HashMap<>();

  private final BlockGraph blockGraph;
  private final DssArgStateCollector stateCollector;

  public record StatusAndResult(AlgorithmStatus status, ResultWithWitnessInformation result) {

    public StatusAndResult(AlgorithmStatus status, Result pResult) {
      // allows other executors that do not use this observer worker to remain unchanged
      // TODO extract witness information from them as well?
      this(status, ResultWithWitnessInformation.ofResultWithoutInformation(pResult));
    }
  }

  public DssObserverWorker(
      String pId,
      DssConnection pConnection,
      BlockGraph pBlockGraph,
      DssMessageFactory pMessageFactory,
      LogManager pLogger,
      DssArgStateCollector pStateCollector) {
    super(pId, pMessageFactory, pLogger);
    shutdown = false;
    connection = pConnection;
    statusObserver = new StatusObserver();
    errorMessage = Optional.empty();
    result = Optional.empty();
    blockGraph = pBlockGraph;
    stateCollector = pStateCollector;
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage) {
    switch (pMessage.getType()) {
      case RESULT -> {
        // TODO make these clauses also sit behind a guard for witness enabled?
        if (pMessage.getResult() == Result.FALSE) {
          result =
              Optional.of(
                  ResultWithWitnessInformation.ofViolationPath(pMessage.getViolationPath()));
        } else if (pMessage.getResult() == Result.TRUE) {
          result =
              Optional.of(
                  ResultWithWitnessInformation.ofCorrectnessPreConditionCollector(stateCollector));
        } else {
          result =
              Optional.of(
                  ResultWithWitnessInformation.ofResultWithoutInformation(pMessage.getResult()));
        }
        statusObserver.updateStatus(pMessage);
      }
      case VIOLATION_CONDITION, POST_CONDITION -> statusObserver.updateStatus(pMessage);
      case EXCEPTION -> {
        errorMessage = Optional.of(pMessage.getExceptionMessage());
        shutdown = true;
      }
      case STATISTIC -> {
        stats.put(pMessage.getSenderId(), pMessage.getStats());

        stateCollector.collectFromMessage((DssStatisticsMessage) pMessage);

        shutdown = stats.size() == blockGraph.getNodes().size();
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

  private String convert(StatisticsKey pKey, String pNumber) {
    if (pKey.isFormattedAsTime()) {
      return TimeSpan.ofNanos(Long.parseLong(pNumber)).formatAs(TimeUnit.SECONDS);
    }
    return pNumber;
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    Map<StatisticsKey, String> overall = new HashMap<>();
    for (Entry<String, Map<StatisticsKey, String>> statEntry : stats.entrySet()) {
      String blockId = statEntry.getKey();
      writer = writer.put("BlockID " + blockId, blockId).beginLevel();
      for (Entry<StatisticsKey, String> entry : statEntry.getValue().entrySet()) {
        writer = writer.put(entry.getKey().getKey(), convert(entry.getKey(), entry.getValue()));
        overall.merge(
            entry.getKey(),
            entry.getValue(),
            (v1, v2) -> Long.toString(Long.parseLong(v1) + Long.parseLong(v2)));
      }
      writer = writer.endLevel();
    }
    writer = writer.put("Overall", "Sum of all blocks").beginLevel();
    for (Entry<StatisticsKey, String> overallEntry : overall.entrySet()) {
      writer =
          writer.put(
              overallEntry.getKey() + " (overall)",
              convert(overallEntry.getKey(), overallEntry.getValue()));
    }
  }

  @Override
  public String getName() {
    return "ObserverWorker " + getId();
  }
}
