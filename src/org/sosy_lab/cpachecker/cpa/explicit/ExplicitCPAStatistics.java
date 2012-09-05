/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.DelegatingExplicitRefiner;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ExplicitCPAStatistics implements Statistics {

  private AbstractARGBasedRefiner refiner = null;

  public ExplicitCPAStatistics() {}

  @Override
  public String getName() {
    return "ExplicitCPA";
  }

  public void addRefiner(AbstractARGBasedRefiner refiner) {
    this.refiner = refiner;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    int maxNumberOfVariables            = 0;
    int maxNumberOfGlobalVariables      = 0;

    long totalNumberOfVariables         = 0;
    long totalNumberOfGlobalVariables   = 0;

    for (AbstractState currentAbstractState : reached.getReached()) {
      ExplicitState currentState = AbstractStates.extractStateByType(currentAbstractState, ExplicitState.class);

      int numberOfVariables         = currentState.getConstantsMap().size();
      int numberOfGlobalVariables   = getNumberOfGlobalVariables(currentState);

      totalNumberOfVariables        = totalNumberOfGlobalVariables + numberOfVariables;
      totalNumberOfGlobalVariables  = totalNumberOfGlobalVariables + numberOfGlobalVariables;

      maxNumberOfVariables          = Math.max(maxNumberOfVariables, numberOfVariables);
      maxNumberOfGlobalVariables    = Math.max(maxNumberOfGlobalVariables, numberOfGlobalVariables);
    }

    out.println("Max. number of variables: " + maxNumberOfVariables);
    out.println("Max. number of globals variables: " + maxNumberOfGlobalVariables);

    out.println("Avg. number of variables: " + ((totalNumberOfVariables * 10000) / reached.size()) / 10000.0);
    out.println("Avg. number of global variables: " + ((totalNumberOfGlobalVariables * 10000) / reached.size()) / 10000.0);

    if (refiner != null) {
      if (refiner instanceof DelegatingExplicitRefiner) {
        ((DelegatingExplicitRefiner)refiner).printStatistics(out, result, reached);
      }
    }
  }

  private int getNumberOfGlobalVariables(ExplicitState state) {
    int numberOfGlobalVariables = 0;

    for(String variableName : state.getConstantsMap().keySet()) {
      if(variableName.contains("::")) {
        numberOfGlobalVariables++;
      }
    }

    return numberOfGlobalVariables;
  }
}
