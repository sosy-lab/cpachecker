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
package org.sosy_lab.cpachecker.core.algorithm.testgen.model;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.algorithm.testgen.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;


public class SameAlgorithmRestartAtDecisionIterationStrategy implements TestGenIterationStrategy {

  protected IterationModel model;
  private ReachedSetFactory reachedSetFactory;
  private TestGenStatistics stats;


  public SameAlgorithmRestartAtDecisionIterationStrategy(StartupConfig pStartupConfig,
      ReachedSetFactory pReachedSetFactory, IterationModel pModel, TestGenStatistics pStats) {
    this.model = pModel;
    this.reachedSetFactory = pReachedSetFactory;
    stats = pStats;

  }

  @Override
  public void updateIterationModelForNextIteration(PredicatePathAnalysisResult pResult) {
    addReachedStatesToOtherReached(model.getLocalReached(), model.getGlobalReached());
    ReachedSet newReached = reachedSetFactory.create();
    newReached.add(model.getGlobalReached().getFirstState(), model.getGlobalReached().getPrecision(model.getGlobalReached().getFirstState()));
    newReached.add(pResult.getWrongState(), model.getGlobalReached().getPrecision(pResult.getWrongState()));
    newReached.removeOnlyFromWaitlist(pResult.getWrongState());
    model.setLocalReached(newReached);

  }

  @Override
  public IterationModel getModel() {
    return model;
  }

  @Override
  public boolean runAlgorithm() throws PredicatedAnalysisPropertyViolationException, CPAException, InterruptedException {
    stats.beforeCpaAlgortihm();
    boolean ret = model.getAlgorithm().run(model.getLocalReached());
    stats.afterCpaAlgortihm();
    return ret;
  }

  private void addReachedStatesToOtherReached(ReachedSet pCurrentReached, ReachedSet pGlobalReached) {
  for (AbstractState state : pCurrentReached) {
    pGlobalReached.add(state,pCurrentReached.getPrecision(state));
    pGlobalReached.removeOnlyFromWaitlist(state);
  }

}

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (getModel().getAlgorithm() instanceof StatisticsProvider) {
      StatisticsProvider sProvider = (StatisticsProvider) getModel().getAlgorithm();
      sProvider.collectStatistics(pStatsCollection);
    }
  }

}
