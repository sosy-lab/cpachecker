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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage.WitnessType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.DssExecutor.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.DssWitnessArgStateCollector;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.ResultWithWitnessInformation;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Observer worker that monitors messages of analysis workers and detects termination conditions.
 *
 * <p>It accumulates {@link AlgorithmStatus} from every POST_CONDITION and VIOLATION_CONDITION
 * message it sees, merging them across all senders. This combined status reflects the overall
 * soundness of the analysis across all blocks.
 *
 * <p>The observer shuts down as soon as a RESULT or EXCEPTION message arrives.
 */
public class DssObserverWorker extends DssWorker {

  private final DssConnection connection;
  private final StatusObserver statusObserver;
  private boolean shutdown;
  private Optional<Result> finalResult;
  private Optional<SegmentedPaths> violationWitness;
  private Optional<String> errorMessage;

  private int witnessMessagesReceived;

  private final Set<String> receivedWitnesses;
  private final BlockGraph blockGraph;
  private final DssWitnessArgStateCollector stateCollector;

  public DssObserverWorker(
      String pId,
      DssConnection pConnection,
      BlockGraph pBlockGraph,
      DssMessageFactory pMessageFactory,
      LogManager pLogger,
      DssWitnessArgStateCollector pStateCollector) {
    super(pId, pMessageFactory, pLogger);
    shutdown = false;
    connection = pConnection;
    statusObserver = new StatusObserver();
    errorMessage = Optional.empty();
    finalResult = Optional.empty();
    violationWitness = Optional.empty();
    blockGraph = pBlockGraph;
    stateCollector = pStateCollector;
    receivedWitnesses = new HashSet<>();
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage) throws InterruptedException {
    switch (pMessage.getType()) {
      case RESULT -> {
        finalResult = Optional.of(pMessage.getResult());
        logger.log(
            Level.INFO, "Received result", pMessage.getResult(), ", waiting for witness messages");
        statusObserver.updateStatus(pMessage);
        shutdown = allPostAnalysisMessagesReceived();
      }
      case VIOLATION_CONDITION, POST_CONDITION -> statusObserver.updateStatus(pMessage);
      case EXCEPTION -> {
        errorMessage = Optional.of(pMessage.getExceptionMessage());
        shutdown = true;
      }
      case WITNESS -> {
        receivedWitnesses.add(pMessage.getSenderId());
        if (pMessage.getWitnessType() == WitnessType.VIOLATION) {
          violationWitness = Optional.of(pMessage.getViolationPath());
        } else {
          stateCollector.collectFromMessage((DssWitnessMessage) pMessage);
        }
        witnessMessagesReceived++;
        shutdown = allPostAnalysisMessagesReceived();
      }
    }
    return ImmutableList.of();
  }

  private boolean allPostAnalysisMessagesReceived() {
    if (finalResult.isEmpty()) {
      return false;
    }
    int expectedWitnessMessages =
        switch (finalResult.orElseThrow()) {
          case TRUE -> blockGraph.getNodes().size();
          case FALSE -> 1;
          default -> 0;
        };
    return receivedWitnesses.size() == blockGraph.getNodes().size()
        && witnessMessagesReceived == expectedWitnessMessages;
  }

  public StatusAndResult observe() throws CPAException {
    super.run();
    if (errorMessage.isPresent()) {
      throw new CPAException(errorMessage.orElseThrow());
    }
    if (finalResult.isEmpty()) {
      throw new CPAException("Analysis finished but no result is present...");
    }
    ResultWithWitnessInformation result =
        switch (finalResult.orElseThrow()) {
          case TRUE ->
              ResultWithWitnessInformation.ofCorrectnessPreConditionCollector(stateCollector);
          case FALSE ->
              ResultWithWitnessInformation.ofViolationPath(violationWitness.orElseThrow());
          default ->
              ResultWithWitnessInformation.ofResultWithoutInformation(finalResult.orElseThrow());
        };
    return new StatusAndResult(statusObserver.finish(), result);
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
        case RESULT, EXCEPTION, WITNESS -> {}
      }
    }

    private AlgorithmStatus finish() {
      return statusMap.values().stream()
          .reduce(AlgorithmStatus::update)
          .orElse(AlgorithmStatus.SOUND_AND_PRECISE);
    }
  }
}
