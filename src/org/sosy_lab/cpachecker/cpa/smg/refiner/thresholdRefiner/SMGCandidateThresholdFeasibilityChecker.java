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
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cpa.smg.SMGHeapAbstractionThreshold;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractListCandidateSequence;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class SMGCandidateThresholdFeasibilityChecker extends SMGThresholdFeasibilityChecker {

  private Map<Integer, Integer> lengthMap;

  public SMGCandidateThresholdFeasibilityChecker(
      SMGStrongestPostOperator pStrongestPostOp,
      LogManager pLogger,
      CFA pCfa,
      UnmodifiableSMGState pInitialState,
      Set<ControlAutomatonCPA> pAutomatonCPA,
      BlockOperator pBlockOperator) {
    super(pStrongestPostOp, pLogger, pCfa, pInitialState, pAutomatonCPA, pBlockOperator);
    lengthMap = new HashMap<>();
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
    lengthMap = new HashMap<>();
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

          if (state.getHeap().getObjects().size() >= 1) {

            SMGState stateCopy = state.copyOf();
            SMGAbstractionCandidate res = null;
            Map<Integer, Integer> tempMap = new HashMap<>();
            Map<Integer, Integer> checkMap = new HashMap<>();
            do {
              res = stateCopy.executeHeapAbstractionOneStep(ImmutableSet.of());

              if (!res.isEmpty()) {
                SMGObject startObject =
                    ((SMGAbstractListCandidateSequence<?>) res).getCandidate().getStartObject();
                int startObjectId = startObject.getId();
                int len = res.getLength();
                if (!lengthMap.containsKey(startObjectId)) {
                  tempMap.put(startObjectId, len);
                } else {
                  checkMap.put(startObjectId, len);
                  if (len > lengthMap.get(startObjectId)) {
                    lengthMap.put(startObjectId, len);
                  }
                }
              }
            } while (!res.isEmpty());
            Set<Integer> diff = Sets.difference(lengthMap.keySet(), checkMap.keySet());
            if (tempMap.size() == 1 && diff.size() == 1) {
              int diffStartObjectId = Iterables.getOnlyElement(diff);
              int tempStartObjectId = (Iterables.getOnlyElement(tempMap.keySet()));
              int diffLength = lengthMap.get(diffStartObjectId);
              int tempLength = tempMap.get(tempStartObjectId);
              int maxLength = Math.max(tempLength, diffLength);
              lengthMap.remove(diffStartObjectId);
              lengthMap.put(tempStartObjectId, maxLength);
            } else {
              for (Entry<Integer, Integer> entry : tempMap.entrySet()) {
                if (!lengthMap.values().contains(entry.getValue())) {
                  lengthMap.put(entry.getKey(), entry.getValue());
                }
              }
            }
          }
        }

        iterator.advance();
        if (cutState == null && !lengthMap.isEmpty()) {
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

  protected boolean isReachableWithAbstraction(
      ARGPath pPath, int pThreshold, SMGThresholdPrecision pPrecision) throws CPAException {

    // We don't want sideffects of smg transfer relation for smg state propagating.
    SMGState start = initialState.copyOf();
    Collection<SMGState> next = new ArrayList<>();
    next.add(start);

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

          return false;
        }

        for (SMGState state : successors) {
          state.executeHeapAbstraction(
              ImmutableSet.of(),
              new SMGHeapAbstractionThreshold(pThreshold, pThreshold, pThreshold),
              false);
        }

        iterator.advance();
        next.clear();
        next.addAll(successors);
      }

      return true;
    } catch (CPATransferException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  protected List<Integer> getSortedMaxLengths() {
    List<Integer> lengths = new ArrayList<>(new HashSet<>(lengthMap.values()));
    Collections.sort(lengths);
    return lengths;
  }

  @Override
  protected ARGState getCutState() {
    return cutState;
  }
}