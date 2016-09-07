/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationStatistics;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Set;

public interface LassoAnalysis {

  /**
   * Tries to prove (non)-termination of a lasso given as {@link CounterexampleInfo}.
   * @param pLoop the Loop the is currently analyzed
   * @param pCounterexample
   *           the {@link CounterexampleInfo} representing the potentially non-terminating lasso
   * @param pRelevantVariables
   *           all variables that might be relevant to prove (non-)termination
   * @return the {@link LassoAnalysisResult}
   * @throws CPATransferException if the extraction of stem or loop fails
   * @throws InterruptedException if a shutdown was requested
   */
  LassoAnalysisResult checkTermination(
      Loop pLoop, CounterexampleInfo pCounterexample, Set<CVariableDeclaration> pRelevantVariables)
      throws CPATransferException, InterruptedException;

  /**
   * Frees all created resources and the solver context.
   */
  void close();

  static LassoAnalysis create(
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      TerminationStatistics pStatistics)
      throws InvalidConfigurationException {
    return new LassoAnalysisImpl(pLogger, pConfig, pShutdownNotifier, pCfa, pStatistics);
  }


}
