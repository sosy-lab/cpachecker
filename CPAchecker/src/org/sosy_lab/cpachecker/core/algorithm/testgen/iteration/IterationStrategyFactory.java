/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.testgen.iteration;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenAlgorithm.IterationStrategySelector;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.TestGenIterationStrategy.IterationModel;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;


public class IterationStrategyFactory {


  private StartupConfig startupConfig;
  private CFA cfa;
  private ReachedSetFactory reachedSetFactory;
  private TestGenStatistics stats;
  private boolean produceDebugFiles;


  public IterationStrategyFactory(StartupConfig pStartupConfig, CFA pCfa, ReachedSetFactory pReachedSetFactory,
      TestGenStatistics pStats, boolean pProduceDebugFiles) {
    super();
    startupConfig = pStartupConfig;
    cfa = pCfa;
    reachedSetFactory = pReachedSetFactory;
    stats = pStats;
    produceDebugFiles = pProduceDebugFiles;
  }

  public TestGenIterationStrategy createStrategy(IterationStrategySelector pIterationStrategySelector,
      Algorithm pAlgorithm) throws InvalidConfigurationException {
    TestGenIterationStrategy iterationStrategy;
    IterationModel model = new IterationModel(pAlgorithm, null, null);
    switch (pIterationStrategySelector) {
    case AUTOMATON_CONTROLLED:
      iterationStrategy =
          new AutomatonControlledIterationStrategy(startupConfig, cfa, model, reachedSetFactory, stats, produceDebugFiles);
      break;
    case SAME_ALGORITHM_RESTART:
      iterationStrategy =
          new RestartAtRootIterationStrategy(startupConfig, reachedSetFactory, model, stats);
      break;
    case SAME_ALGORITHM_FILTER_WAITLIST:
      iterationStrategy =
          new RestartAtDecisionIterationStrategy(startupConfig, reachedSetFactory, model, stats);
      break;
    default:
      throw new InvalidConfigurationException("Invald iteration strategy selected");
    }
    return iterationStrategy;
  }

}
