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
package org.sosy_lab.cpachecker.pcc.util;

import java.io.PrintStream;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "pcc")
public class ProofStatesInfoCollector implements Statistics {

  @Option(secure = true,
      description = "collects information about value analysis states in proof")
  private boolean collectValueAnalysisStateInfo = false;

  private int numProofStates = 0;
  private int numValuesInValueAnalysisStates = 0;

  public ProofStatesInfoCollector(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public void addInfoForStates(final AbstractState[] partialProofStates) {
    numProofStates += partialProofStates.length;

    if (collectValueAnalysisStateInfo) {
      collectValueAnalysisInfo(partialProofStates);
    }
  }

  private void collectValueAnalysisInfo(final AbstractState[] partialProofStates) {
    ValueAnalysisState vState;
    for (AbstractState state : partialProofStates) {
      vState = AbstractStates.extractStateByType(state, ValueAnalysisState.class);
      if (vState != null) {
        numValuesInValueAnalysisStates += vState.getConstantsMapView().size();
      }
    }
  }

  public String getInfoAsString() {
    if (collectValueAnalysisStateInfo) {
      return "Proof state info:\n #states in proof:\t" + numProofStates
          + "\n #values stored in value analysis:\t" + numValuesInValueAnalysisStates;
    } else {
      return "No proof state information collected.";
    }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println(getInfoAsString());

  }

  @Override
  public @Nullable
  String getName() {
    return null;
  }

}
