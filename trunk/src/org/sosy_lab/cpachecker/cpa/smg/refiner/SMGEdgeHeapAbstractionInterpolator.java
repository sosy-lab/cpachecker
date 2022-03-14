// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGEdgeInterpolator.SMGHeapAbstractionInterpoaltionResult;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class SMGEdgeHeapAbstractionInterpolator {

  private final LogManager logger;
  private final SMGFeasibilityChecker checker;
  private final BlockOperator blockOperator;

  public SMGEdgeHeapAbstractionInterpolator(
      LogManager pLogger, SMGFeasibilityChecker pChecker, BlockOperator pBlockOperator) {
    logger = pLogger;
    checker = pChecker;
    blockOperator = pBlockOperator;
  }

  /**
   * Calculates necessary blocks for heap abstractions to keep an infeasible error path without heap
   * abstraction infeasible when executing heap abstraction on the given node of the path, based on
   * the given precision.
   *
   * @param pState the smg state on the node of the given cfaNode.
   * @param pRemainingErrorPath the remaining infeasible error path,that starts with given cfa node.
   * @param pPrecision the current precision of the program.
   * @param pStateLocation the location of the given smgState.
   * @param pAllTargets should we check for all errors, or only the one in the target
   * @return a set of abstraction blocks that refine the given precision, so that the resulting smg
   *     state after heap abstraction is strong enough for the given precision to enable
   */
  public SMGHeapAbstractionInterpoaltionResult calculateHeapAbstractionBlocks(
      SMGState pState,
      ARGPath pRemainingErrorPath,
      SMGPrecision pPrecision,
      CFANode pStateLocation,
      CFAEdge pCurrentEdge,
      boolean pAllTargets)
      throws CPAException, InterruptedException {

    SMGState state = pState;

    if (!pPrecision.allowsHeapAbstractionOnNode(pStateLocation, blockOperator)) {
      return SMGHeapAbstractionInterpoaltionResult.emptyAndUnchanged();
    }

    logger.log(
        Level.ALL,
        "Begin interpolating heap abstraction on node " + pStateLocation.getNodeNumber());

    SMGState abstractionTest = pState.copyOf();
    Set<SMGAbstractionBlock> result =
        new HashSet<>(pPrecision.getAbstractionBlocks(pStateLocation));
    SMGAbstractionCandidate candidate = abstractionTest.executeHeapAbstractionOneStep(result);
    boolean change = false;

    while (!candidate.isEmpty()) {

      if (isRemainingPathFeasible(
          pRemainingErrorPath, abstractionTest, pCurrentEdge, pAllTargets)) {
        result.add(candidate.createAbstractionBlock(state));
        abstractionTest = state.copyOf();
      } else {
        state.executeHeapAbstractionOneStep(result);
        change = true;
      }

      candidate = abstractionTest.executeHeapAbstractionOneStep(result);
    }

    logger.log(
        Level.ALL,
        "Finish interpolating heap abstraction on node " + pStateLocation.getNodeNumber());

    if (!change && result.isEmpty()) {
      return SMGHeapAbstractionInterpoaltionResult.emptyAndUnchanged();
    } else {
      return new SMGHeapAbstractionInterpoaltionResult(result, change);
    }
  }

  private boolean isRemainingPathFeasible(
      ARGPath pRemainingErrorPath,
      UnmodifiableSMGState pAbstractionTest,
      CFAEdge pCurrentEdge,
      boolean pAllTargets)
      throws CPAException, InterruptedException {

    return checker.isRemainingPathFeasible(
        pRemainingErrorPath, pAbstractionTest, pCurrentEdge, pAllTargets);
  }
}
