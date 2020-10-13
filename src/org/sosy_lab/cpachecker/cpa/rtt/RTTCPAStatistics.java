// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rtt;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class RTTCPAStatistics implements Statistics {



  public RTTCPAStatistics() {}

  @Override
  public String getName() {
    return "RTTCPA";
  }



  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    int maxNumberOfVariables            = 0;
    int maxNumberOfGlobalVariables      = 0;

    long totalNumberOfVariables         = 0;
    long totalNumberOfGlobalVariables   = 0;

    for (AbstractState currentAbstractState : reached) {
      RTTState currentState = AbstractStates.extractStateByType(currentAbstractState, RTTState.class);

      int numberOfVariables         = currentState.getConstantsMap().size();
      int numberOfGlobalVariables   = getNumberOfGlobalVariables(currentState);

      totalNumberOfVariables        = totalNumberOfGlobalVariables + numberOfVariables;
      totalNumberOfGlobalVariables  = totalNumberOfGlobalVariables + numberOfGlobalVariables;

      maxNumberOfVariables          = Math.max(maxNumberOfVariables, numberOfVariables);
      maxNumberOfGlobalVariables    = Math.max(maxNumberOfGlobalVariables, numberOfGlobalVariables);
    }

    out.println("Max. number of variables: " + maxNumberOfVariables);
    out.println("Max. number of globals variables: " + maxNumberOfGlobalVariables);

    out.println("Avg. number of variables: " + ((totalNumberOfVariables * 10000.0) / reached.size()) / 10000.0);
    out.println("Avg. number of global variables: " + ((totalNumberOfGlobalVariables * 10000.0) / reached.size()) / 10000.0);

  }

  private int getNumberOfGlobalVariables(RTTState state) {
    int numberOfGlobalVariables = 0;

    for (String variableName : state.getConstantsMap().keySet()) {
      if (variableName.contains("::")) {
        numberOfGlobalVariables++;
      }
    }

    return numberOfGlobalVariables;
  }
}
