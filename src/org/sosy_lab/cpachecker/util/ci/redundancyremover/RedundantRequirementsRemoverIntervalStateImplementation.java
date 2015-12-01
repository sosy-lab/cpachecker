/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover.RedundantRequirementsRemoverImplementation;


public class RedundantRequirementsRemoverIntervalStateImplementation extends
    RedundantRequirementsRemoverImplementation<IntervalAnalysisState, Interval> {

  @Override
  public int compare(Interval pO1, Interval pO2) {
    // TODO
    // one of arguments null -> NullPointerException
    // 0 if bounds the same for both
    // -1 if both bounds of p01 contained in bounds of p02
    // 1 if both bounds p02 contained in bounds of p01
    // -1 if p01 lower bound smaller than p02 bound
    // -1 if p01 lower bound equal to p02 lower bound and p01 higher bound smaller than p02 higher bound
    // otherwise 1
    return 0;
  }

  @Override
  protected Interval getAbstractValue(IntervalAnalysisState pAbstractState, String pVarOrConst) {
    // TODO
    // if pVarOrConst number, return interval [pVarOrConst,pVarOrConst]
    // if state contains pVarOrConst return interval saved in state
    // otherwise unboundedInterval
    return null;
  }

  @Override
  protected Interval[] emptyArrayOfSize(int pSize) {
    return new Interval[Math.max(0, pSize)];
  }

  @Override
  protected Interval[][] emptyMatrixOfSize(int pSize) {
    return new Interval[Math.max(0, pSize)][];
  }

  @Override
  protected IntervalAnalysisState extractState(AbstractState pWrapperState) {
    // TODO AbstractStates.extractState..
    return null;
  }

}
