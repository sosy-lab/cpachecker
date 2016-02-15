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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.Deque;
import java.util.Set;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGStateInformation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

public class SMGEdgeInterpolator extends GenericEdgeInterpolator<SMGState, SMGStateInformation, SMGInterpolant> {

  private final SMGFeasibilityChecker checker;
  private final Set<ControlAutomatonCPA> automatons;

  /**
   * the number of interpolations
   */
  private int numberOfInterpolationQueries = 0;

  public SMGEdgeInterpolator(StrongestPostOperator<SMGState> pStrongestPostOperator,
      SMGFeasibilityChecker pFeasibilityChecker,
      SMGCPA pSMGCPA,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa, InterpolantManager<SMGState, SMGInterpolant> pSmgInterpolantManager, Set<ControlAutomatonCPA> pAutomatons)
          throws InvalidConfigurationException {
    super(pStrongestPostOperator, pFeasibilityChecker,
        pSmgInterpolantManager,
        pSMGCPA.getInitialState(pCfa.getMainFunction()),
        SMGCPA.class, pConfig,
        pShutdownNotifier, pCfa);
    checker = pFeasibilityChecker;
    automatons = pAutomatons;
  }

  @Override
  public boolean isRemainingPathFeasible(ARGPath pRemainingErrorPath, SMGState pState)
      throws CPAException, InterruptedException {
    numberOfInterpolationQueries++;
    return checker.isFeasible(pRemainingErrorPath, pState, automatons);
  }

  @Override
  public SMGInterpolant deriveInterpolant(ARGPath pErrorPath, CFAEdge pCurrentEdge, Deque<SMGState> pCallstack,
      PathPosition pOffset, SMGInterpolant pInputInterpolant) throws CPAException, InterruptedException {
    numberOfInterpolationQueries = 0;
    return super.deriveInterpolant(pErrorPath, pCurrentEdge, pCallstack, pOffset, pInputInterpolant);
  }

  @Override
  public int getNumberOfInterpolationQueries() {
    return numberOfInterpolationQueries;
  }

}
