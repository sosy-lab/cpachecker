// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

public class TerminationToReachTransferRelation extends SingleEdgeTransferRelation {
  private final FormulaManagerView fmgr;

  public TerminationToReachTransferRelation(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    return Collections.singleton(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    LocationState locationState = getLocationState(pOtherStates);
    CallstackState callstackState = getCallStackState(pOtherStates);
    CFANode location = AbstractStates.extractLocation(locationState);
    PredicateAbstractState predicateState = getPredicateState(pOtherStates);
    TerminationToReachState terminationState = (TerminationToReachState) pState;

    if (location == null) {
      throw new UnsupportedOperationException("TransferRelation requires location information.");
    }
    if (location.isLoopStart()) {
      Pair<LocationState, CallstackState> pairKey = Pair.of(locationState, callstackState);

      ImmutableMap.Builder<
              Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
          newStoredValues = ImmutableMap.builder();
      ImmutableMap.Builder<Pair<LocationState, CallstackState>, Integer> newNumberOfIterations =
          ImmutableMap.builder();
      ImmutableMap.Builder<Pair<LocationState, CallstackState>, PathFormula>
          newPathFormulaForIteration = ImmutableMap.builder();

      for (Pair<LocationState, CallstackState> pairKey2 :
          terminationState.getStoredValues().keySet()) {
        if (!pairKey2.equals(pairKey)) {
          newStoredValues.put(pairKey2, terminationState.getStoredValues().get(pairKey2));
          newNumberOfIterations.put(
              pairKey2, terminationState.getNumberOfIterationsAtLoopHead(pairKey2));
          newPathFormulaForIteration.put(
              pairKey2, terminationState.getPathFormulas().get(pairKey2));
        }
      }
      newPathFormulaForIteration.put(pairKey, predicateState.getPathFormula());
      ImmutableMap.Builder<Integer, ImmutableSet<Formula>> newValues = ImmutableMap.builder();

      if (terminationState.getStoredValues().containsKey(pairKey)) {
        newValues.putAll(terminationState.getStoredValues().get(pairKey));
        newValues.put(
            terminationState.getNumberOfIterationsAtLoopHead(pairKey),
            extractLoopHeadVariables(predicateState.getPathFormula()));
        newStoredValues.put(pairKey, newValues.build());
        newNumberOfIterations.put(
            pairKey, terminationState.getNumberOfIterationsAtLoopHead(pairKey) + 1);
      } else {
        newValues.put(0, extractLoopHeadVariables(predicateState.getPathFormula()));
        newStoredValues.put(pairKey, newValues.build());
        newNumberOfIterations.put(pairKey, 1);
      }
      return Collections.singleton(
          new TerminationToReachState(
              newStoredValues.build(),
              newNumberOfIterations.build(),
              newPathFormulaForIteration.build()));
    } else {
      return Collections.singleton(pState);
    }
  }

  private ImmutableSet<Formula> extractLoopHeadVariables(PathFormula pPathFormula) {
    SSAMap ssaMap = pPathFormula.getSsa();
    ImmutableSet.Builder<Formula> newStoredIndices = ImmutableSet.builder();
    for (Formula variable : fmgr.extractVariables(pPathFormula.getFormula()).values()) {
      newStoredIndices.add(fmgr.instantiate(fmgr.uninstantiate(variable), ssaMap));
    }
    return newStoredIndices.build();
  }

  private LocationState getLocationState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      if (state instanceof LocationState locationState) {
        return locationState;
      }
    }
    throw new UnsupportedOperationException("TransferRelation requires location information.");
  }

  private CallstackState getCallStackState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      Optional<CallstackStateEqualsWrapper> possibleCallStack =
          AbstractStates.extractOptionalCallstackWraper(state);
      if (possibleCallStack.isPresent()) {
        return possibleCallStack.get().getState();
      }
    }
    throw new UnsupportedOperationException("TransferRelation requires call-stack information.");
  }

  private PredicateAbstractState getPredicateState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      if (state instanceof PredicateAbstractState predicateAbstractState) {
        return predicateAbstractState;
      }
    }
    throw new UnsupportedOperationException("TransferRelation requires path information.");
  }
}
