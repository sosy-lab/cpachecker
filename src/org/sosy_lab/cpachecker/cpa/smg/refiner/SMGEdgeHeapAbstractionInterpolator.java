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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGEdgeInterpolator.SMGHeapAbstractionInterpoaltionResult;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class SMGEdgeHeapAbstractionInterpolator {

  private final LogManager logger;
  private final SMGFeasibilityChecker checker;

  public SMGEdgeHeapAbstractionInterpolator(LogManager pLogger, SMGFeasibilityChecker pChecker) {
    super();
    logger = pLogger;
    checker = pChecker;
  }

  /**
   * Calculates necessary blocks for heap abstractions to keep an infeasible
   * error path without heap abstraction infeasible when executing heap abstraction
   * on the given node of the path, based on the given precision.
   *
   * @param pState the smg state on the node of the given cfaNode.
   * @param pRemainingErrorPath the remaining infeasible error path,that starts with
   *        given cfa node.
   * @param pPrecision the current precision of the program.
   * @param pStateLocation the location of the given smgState.
   * @param pAllTargets should we check for all errors, or only the one in the target
   * @return a set of abstraction blocks that refine the given precision, so that the resulting
   * smg state after heap abstraction is strong enough for the given precision to
   * enable
   */
  public SMGHeapAbstractionInterpoaltionResult calculateHeapAbstractionBlocks(SMGState pState,
      ARGPath pRemainingErrorPath, SMGPrecision pPrecision, CFANode pStateLocation,
      CFAEdge pCurrentEdge, boolean pAllTargets)
      throws CPAException, InterruptedException {

    SMGState state = pState;

    if (!pPrecision.allowsHeapAbstractionOnNode(pStateLocation)) {
      return SMGHeapAbstractionInterpoaltionResult.emptyAndUnchanged();
    }

    logger.log(Level.ALL, "Begin interpolating heap abstraction on node " + pStateLocation.getNodeNumber());

    SMGState abstractionTest = new SMGState(pState);
    Set<SMGAbstractionBlock> result = new HashSet<>();
    result.addAll(pPrecision.getAbstractionBlocks(pStateLocation));
    SMGAbstractionCandidate candidate = abstractionTest.executeHeapAbstractionOneStep(result);
    boolean change = false;

    while (!candidate.isEmpty()) {

      if (isRemainingPathFeasible(pRemainingErrorPath, abstractionTest, pCurrentEdge,
          pAllTargets)) {
        result.add(candidate.createAbstractionBlock(state));
        abstractionTest = new SMGState(state);
      } else {
        state.executeHeapAbstractionOneStep(result);
        change = true;
      }

      candidate = abstractionTest.executeHeapAbstractionOneStep(result);
    }

    logger.log(Level.ALL, "Finish interpolating heap abstraction on node " + pStateLocation.getNodeNumber());

    if (!change && result.isEmpty()) {
      return SMGHeapAbstractionInterpoaltionResult.emptyAndUnchanged();
    } else {
      return new SMGHeapAbstractionInterpoaltionResult(result, change);
    }
  }

  private boolean isRemainingPathFeasible(ARGPath pRemainingErrorPath, SMGState pAbstractionTest,
      CFAEdge pCurrentEdge, boolean pAllTargets) throws CPAException, InterruptedException {

    return checker.isRemainingPathFeasible(pRemainingErrorPath, pAbstractionTest, pCurrentEdge, pAllTargets);
  }
}