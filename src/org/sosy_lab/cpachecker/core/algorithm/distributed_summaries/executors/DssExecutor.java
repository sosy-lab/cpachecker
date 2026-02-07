// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors;

import java.io.IOException;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
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
   * @return The status and result of the analysis
   */
  StatusAndResult execute(CFA cfa, BlockGraph blockGraph)
      throws CPAException,
          IOException,
          InterruptedException,
          InvalidConfigurationException,
          SolverException;
}
