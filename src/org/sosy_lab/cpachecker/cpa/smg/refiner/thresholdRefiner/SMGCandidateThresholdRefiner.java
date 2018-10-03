/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner.thresholdRefiner;

import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;

public class SMGCandidateThresholdRefiner extends SMGThresholdRefiner {

  private final SMGCandidateThresholdFeasibilityChecker checker;

  private SMGCandidateThresholdRefiner(
      LogManager pLogger,
      PathExtractor pPathExtractor,
      ARGCPA pArgCpa,
      SMGCPA pSmgCpa,
      Set<ControlAutomatonCPA> automatonCpas) {
    super(pLogger, pPathExtractor, pArgCpa, pSmgCpa);
    checker =
        new SMGCandidateThresholdFeasibilityChecker(
            strongestPostOpForCEX,
            smgCpa.getLogger(),
            smgCpa.getCFA(),
            initialState,
            automatonCpas,
            smgCpa.getBlockOperator());
  }

  public static final SMGCandidateThresholdRefiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SMGCandidateThresholdRefiner.class);
    SMGCPA smgCpa = CPAs.retrieveCPAOrFail(pCpa, SMGCPA.class, SMGCandidateThresholdRefiner.class);
    Set<ControlAutomatonCPA> automatonCpas =
        CPAs.asIterable(pCpa).filter(ControlAutomatonCPA.class).toSet();

    return new SMGCandidateThresholdRefiner(
        smgCpa.getLogger(),
        new PathExtractor(smgCpa.getLogger(), smgCpa.getConfiguration()),
        argCpa,
        smgCpa,
        automatonCpas);
  }

  @Override
  protected CounterexampleInfo performRefinementForPaths(
      ARGReachedSet pArgReached, List<ARGPath> pTargetPaths)
      throws CPAException, InterruptedException {

    CounterexampleInfo cex = isAnyPathFeasible(pArgReached, pTargetPaths);

    int lengthThreshold = 0;
    ARGState cutState = null;
    if (cex.isSpurious()) {
      cutState = checker.getCutState();
      List<Integer> sortedMaxLengths = checker.getSortedMaxLengths();
      for (Integer len : sortedMaxLengths) {
        lengthThreshold = len + 1;
        for (ARGPath path : pTargetPaths) {
          if (!checker.isReachableWithAbstraction(
              path,
              lengthThreshold,
              (SMGThresholdPrecision)
                  SMGThresholdPrecision.createStaticPrecision(
                      true, smgCpa.getBlockOperator(), lengthThreshold))) {
            refineUsingMaxLength(pArgReached, cutState, lengthThreshold);
            return cex;
          }
        }
      }
    }
    if (cex.isSpurious()) {
      refineUsingMaxLength(pArgReached, cutState, 2 * lengthThreshold);
    }

    return cex;
  }

  @Override
  protected boolean isErrorPathFeasible(ARGPath pErrorPath)
      throws CPAException, InterruptedException {
    return checker.isFeasible(pErrorPath);
  }
}