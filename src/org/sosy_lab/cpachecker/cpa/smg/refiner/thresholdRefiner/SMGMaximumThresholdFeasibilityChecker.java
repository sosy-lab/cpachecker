/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedListFinder;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedListFinder;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class SMGMaximumThresholdFeasibilityChecker extends SMGThresholdFeasibilityChecker {

  private int maxLength;

  public SMGMaximumThresholdFeasibilityChecker(
      SMGStrongestPostOperator pStrongestPostOp,
      LogManager pLogger,
      CFA pCfa,
      UnmodifiableSMGState pInitialState,
      Set<ControlAutomatonCPA> pAutomatonCPA,
      BlockOperator pBlockOperator) {
    super(pStrongestPostOp, pLogger, pCfa, pInitialState, pAutomatonCPA, pBlockOperator);
    maxLength = 0;
  }

  @Override
  protected ReachabilityResult isReachable(
      ARGPath pPath, UnmodifiableSMGState pStartingPoint, SMGThresholdPrecision pPrecision)
      throws CPAException {

    // We don't want sideffects of smg transfer relation for smg state propagating.
    SMGState start = pStartingPoint.copyOf();
    Collection<SMGState> next = new ArrayList<>();
    next.add(start);

    cutState = null;
    maxLength = 0;
    int n = 2;
    SMGDoublyLinkedListFinder dllFinder = new SMGDoublyLinkedListFinder(n, n, n);
    SMGSingleLinkedListFinder sllFinder = new SMGSingleLinkedListFinder(n, n, n);
    try {
      CFAEdge edge = null;

      PathIterator iterator = pPath.pathIterator();

      while (iterator.hasNext()) {
        edge = iterator.getOutgoingEdge();

        Collection<SMGState> successors = strongestPostOp.getStrongestPost(next, pPrecision, edge);

        // no successors => path is infeasible
        if (successors.isEmpty()) {
          logger.log(
              Level.FINE,
              "found path to be infeasible: ",
              iterator.getOutgoingEdge(),
              " did not yield a successor");

          return ReachabilityResult.isNotReachable(iterator.getPosition());
        }

        for (SMGState state : successors) {

          if (state.getHeap().getObjects().size() > 1) {
            final List<SMGAbstractionCandidate> absCandidates = new ArrayList<>();
            CLangSMG smg = state.getHeap().copyOf();
            absCandidates.addAll(dllFinder.traverse(smg, state, ImmutableSet.of()));
            absCandidates.addAll(sllFinder.traverse(smg, state, ImmutableSet.of()));
            if (!absCandidates.isEmpty()) {
              int len = absCandidates.get(0).getLength();
              for (SMGAbstractionCandidate candidate : absCandidates) {
                if (candidate.getLength() > len) {
                  len = candidate.getLength();
                }
              }
              maxLength = len > maxLength ? len : maxLength;
            }
            if (cutState == null && maxLength > 0) {
              cutState = iterator.getAbstractState();
            }
          }
        }

        iterator.advance();
        if (cutState == null && maxLength > 0) {
          cutState = iterator.getAbstractState();
        }
        next.clear();
        next.addAll(successors);
      }

      return ReachabilityResult.isReachable(next, edge, iterator.getPosition());
    } catch (CPATransferException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  protected int getMaxLength() {
    return maxLength;
  }

  @Override
  protected ARGState getCutState() {
    return cutState;
  }
}