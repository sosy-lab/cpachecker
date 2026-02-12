// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

public class TerminationToReachTransferRelation extends SingleEdgeTransferRelation {
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;

  public TerminationToReachTransferRelation(
      FormulaManagerView pFormulaManagerView, PathFormulaManager pPathFormulaManager) {
    fmgr = pFormulaManagerView;
    pfmgr = pPathFormulaManager;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    TerminationToReachState terminationState = (TerminationToReachState) state;
    if (terminationState.isTerminating()) {
      return ImmutableList.of();
    }
    return ImmutableList.of(state);
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
      ImmutableMap.Builder<Pair<LocationState, CallstackState>, PathFormula>
          newPathFormulaForPrefix = ImmutableMap.builder();

      for (Entry<Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
          entry : terminationState.getStoredValues().entrySet()) {
        if (!entry.getKey().equals(pairKey)) {
          newStoredValues.put(entry.getKey(), entry.getValue());
          newNumberOfIterations.put(
              entry.getKey(), terminationState.getNumberOfIterationsAtLoopHead(entry.getKey()));
          newPathFormulaForPrefix.put(
              entry.getKey(), terminationState.getPathFormulasForPrefix().get(entry.getKey()));
          if (terminationState.getPathFormulasForIteration().containsKey(entry.getKey())) {
            newPathFormulaForIteration.put(
                entry.getKey(), terminationState.getPathFormulasForIteration().get(entry.getKey()));
          }
        }
      }
      ImmutableMap.Builder<Integer, ImmutableSet<Formula>> newValues = ImmutableMap.builder();

      if (terminationState.getStoredValues().containsKey(pairKey)) {
        newValues.putAll(terminationState.getStoredValues().get(pairKey));
        newValues.put(
            terminationState.getNumberOfIterationsAtLoopHead(pairKey),
            extractLoopHeadVariables(predicateState.getPathFormula(), pairKey.getSecond()));
        newStoredValues.put(pairKey, newValues.buildOrThrow());
        newNumberOfIterations.put(
            pairKey, terminationState.getNumberOfIterationsAtLoopHead(pairKey) + 1);

        if (terminationState.getPathFormulasForIteration().containsKey(pairKey)) {
          newPathFormulaForIteration.put(
              pairKey,
              pfmgr.makeConjunction(
                  ImmutableList.of(
                      terminationState.getPathFormulasForIteration().get(pairKey),
                      predicateState.getPathFormula())));
        } else {
          newPathFormulaForIteration.put(pairKey, predicateState.getPathFormula());
        }
        newPathFormulaForPrefix.put(
            pairKey, terminationState.getPathFormulasForPrefix().get(pairKey));
      } else {
        newValues.put(
            0, extractLoopHeadVariables(predicateState.getPathFormula(), pairKey.getSecond()));
        newStoredValues.put(pairKey, newValues.buildOrThrow());
        newNumberOfIterations.put(pairKey, 1);
        newPathFormulaForPrefix.put(pairKey, predicateState.getPathFormula());
      }
      return ImmutableList.of(
          new TerminationToReachState(
              newStoredValues.buildOrThrow(),
              newNumberOfIterations.buildOrThrow(),
              newPathFormulaForIteration.buildOrThrow(),
              newPathFormulaForPrefix.buildOrThrow()));
    }
    return ImmutableList.of(pState);
  }

  private ImmutableSet<Formula> extractLoopHeadVariables(
      PathFormula pPathFormula, CallstackState pCallstackState) {
    SSAMap ssaMap = pPathFormula.getSsa();
    ImmutableSet.Builder<Formula> newStoredIndices = ImmutableSet.builder();
    for (Entry<String, Formula> variable :
        fmgr.extractVariables(pPathFormula.getFormula()).entrySet()) {
      if (variable.getKey().startsWith(pCallstackState.getCurrentFunction())) {
        newStoredIndices.add(fmgr.instantiate(fmgr.uninstantiate(variable.getValue()), ssaMap));
      }
    }
    return newStoredIndices.build();
  }

  private LocationState getLocationState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      LocationState possibleState = AbstractStates.extractStateByType(state, LocationState.class);
      if (possibleState != null) {
        return possibleState;
      }
    }
    throw new UnsupportedOperationException(
        "TransferRelation requires information from PredicateCPA.");
  }

  private CallstackState getCallStackState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      CallstackState possibleState = AbstractStates.extractStateByType(state, CallstackState.class);
      if (possibleState != null) {
        return possibleState;
      }
    }
    throw new UnsupportedOperationException(
        "TransferRelation requires information from PredicateCPA.");
  }

  private PredicateAbstractState getPredicateState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      PredicateAbstractState possibleState =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      if (possibleState != null) {
        return possibleState;
      }
    }
    throw new UnsupportedOperationException(
        "TransferRelation requires information from PredicateCPA.");
  }
}
