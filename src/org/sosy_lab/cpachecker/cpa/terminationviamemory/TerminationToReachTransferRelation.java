// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
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
    TerminationToReachState terminationState = (TerminationToReachState) state;
    Map<LocationState, Map<Integer, Set<Formula>>> newStoredValuesMap = new HashMap<>();
    Map<LocationState, Map<Integer, Set<Formula>>> oldStoredValuesMap =
        terminationState.getStoredValues();
    for (LocationState locationState : oldStoredValuesMap.keySet()) {
      newStoredValuesMap.put(locationState, new HashMap<>());
      for (Entry<Integer, Set<Formula>> storedValues :
          oldStoredValuesMap.get(locationState).entrySet()) {
        newStoredValuesMap
            .get(locationState)
            .put(storedValues.getKey(), new HashSet<>(storedValues.getValue()));
      }
    }
    return Collections.singleton(
        new TerminationToReachState(
            newStoredValuesMap,
            new HashMap<>(terminationState.getNumberOfIterationsMap()),
            new HashMap<>(terminationState.getPathFormulas())));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    LocationState locationState = getLocationState(pOtherStates);
    CFANode location = AbstractStates.extractLocation(locationState);
    PredicateAbstractState predicateState = getPredicateState(pOtherStates);
    TerminationToReachState terminationState = (TerminationToReachState) pState;

    if (location == null) {
      throw new UnsupportedOperationException("TransferRelation requires location information.");
    }
    if (location.isLoopStart()) {
      terminationState.putNewPathFormula(
          locationState, predicateState.getPathFormula().getFormula());
      if (terminationState.getStoredValues().containsKey(locationState)) {
        terminationState.setNewStoredValues(
            locationState,
            extractLoopHeadVariables(predicateState.getPathFormula()),
            terminationState.getNumberOfIterationsAtLoopHead(locationState));
        terminationState.increaseNumberOfIterationsAtLoopHead(locationState);
      } else {
        terminationState.setNewStoredValues(
            locationState, extractLoopHeadVariables(predicateState.getPathFormula()), 0);
        terminationState.increaseNumberOfIterationsAtLoopHead(locationState);
      }
    }
    return Collections.singleton(pState);
  }

  /**
   * Stores new assumptions about value of variables seen. For instance, if there is x@2 in SSAmap
   * then it will add a condition to the stored values: __Q__x0 = x@2 Where the storing variables
   * are of the form __Q__[name of variable][number of loop iterations].
   */
  private Set<Formula> extractLoopHeadVariables(PathFormula pPathFormula) {
    SSAMap ssaMap = pPathFormula.getSsa();
    Set<Formula> newStoredIndices = new HashSet<>();
    for (Formula variable : fmgr.extractVariables(pPathFormula.getFormula()).values()) {
      newStoredIndices.add(fmgr.instantiate(fmgr.uninstantiate(variable), ssaMap));
    }
    return newStoredIndices;
  }

  private LocationState getLocationState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      if (state instanceof LocationState locationState) {
        return locationState;
      }
    }
    throw new UnsupportedOperationException("TransferRelation requires location information.");
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
