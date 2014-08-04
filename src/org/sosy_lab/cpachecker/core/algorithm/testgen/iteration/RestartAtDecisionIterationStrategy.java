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

import static org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils.addReachedStatesToOtherReached;

import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;


public class RestartAtDecisionIterationStrategy extends AbstractIterationStrategy {

  public RestartAtDecisionIterationStrategy(StartupConfig pStartupConfig,
      ReachedSetFactory pReachedSetFactory, IterationModel pModel, TestGenStatistics pStats) {
    super(pStartupConfig, pModel, pReachedSetFactory, pStats);
  }

  @Override
  public void updateIterationModelForNextIteration(PredicatePathAnalysisResult pResult) {
    reinitializeLocalReachedWithInitial(pResult.getWrongState(), pResult.getDecidingState());
  }


  protected void reinitializeLocalReachedWithInitial(AbstractState wrongState, AbstractState rootState){
    Precision wrongStatePrec = getModel().getGlobalReached().getPrecision(wrongState);
    Precision rootStatePrec = getModel().getGlobalReached().getPrecision(rootState);
    ReachedSet newReached = reachedSetFactory.create();
//    addReachedStatesToOtherReached(getModel().getLocalReached(), newReached);
    getModel().setLocalReached(newReached);
//    getModel().getLocalReached().reAddToWaitlist(rootState);
    getModel().getLocalReached().add(rootState, rootStatePrec);
    ReachedSetUtils.addToReachedOnly(getModel().getLocalReached(), wrongState, wrongStatePrec);
  }

  @Override
  protected void updateReached() {
    addReachedStatesToOtherReached(getModel().getLocalReached(), getModel().getGlobalReached());
  }

}
