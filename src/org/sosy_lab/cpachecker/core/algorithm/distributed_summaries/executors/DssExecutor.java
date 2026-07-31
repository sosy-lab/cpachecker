// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.DssWitnessArgStateCollector;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.ResultWithWitnessInformation;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The DSS framework can use different executors to run the analysis. An executor is responsible to
 * set up the workers and run the analysis given a CFA and its block decomposition.
 *
 * <p>It is the executor's decision how to schedule the workers, e.g., using multiple threads or a
 * single worker.
 */
public interface DssExecutor extends StatisticsProvider {

  /**
   * Execute the DSS analysis on the given CFA and its block decomposition.
   *
   * @param cfa The CFA to analyze
   * @param blockGraph The block decomposition of the CFA
   * @param stateCollector collecting states for correctness witness
   * @return The status and result of the analysis
   */
  StatusAndResult execute(
      CFA cfa, BlockGraph blockGraph, DssWitnessArgStateCollector stateCollector)
      throws CPAException,
          IOException,
          InterruptedException,
          InvalidConfigurationException,
          SolverException;

  record StatusAndResult(AlgorithmStatus status, ResultWithWitnessInformation result) {

    // allows executors that do not provide witness information to remain unchanged
    public StatusAndResult(AlgorithmStatus pStatus, Result pResult) {
      this(pStatus, ResultWithWitnessInformation.ofResultWithoutInformation(pResult));
    }
  }

  class StatusObserver {

    private final Map<String, AlgorithmStatus> statusMap;

    public StatusObserver() {
      statusMap = new HashMap<>();
    }

    public void updateStatus(DssMessage pMessage) {
      switch (pMessage.getType()) {
        case VIOLATION_CONDITION, POST_CONDITION ->
            statusMap.put(pMessage.getSenderId(), pMessage.getAlgorithmStatus());
        case RESULT, EXCEPTION, STATISTIC, WITNESS -> {}
      }
    }

    public AlgorithmStatus finish() {
      return statusMap.values().stream()
          .reduce(AlgorithmStatus::update)
          .orElse(AlgorithmStatus.SOUND_AND_PRECISE);
    }
  }
}
