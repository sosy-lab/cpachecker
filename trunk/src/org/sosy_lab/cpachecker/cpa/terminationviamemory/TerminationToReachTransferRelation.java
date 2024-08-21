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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TerminationToReachTransferRelation extends SingleEdgeTransferRelation {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CToFormulaConverterWithPointerAliasing ctoFormulaConverter;

  public TerminationToReachTransferRelation(
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter) {
    fmgr = pFmgr;
    bfmgr = pBfmgr;
    ctoFormulaConverter = pCtoFormulaConverter;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    TerminationToReachState terminationState = (TerminationToReachState) state;
    return Collections.singleton(
        new TerminationToReachState(
            new HashMap<>(terminationState.getStoredValues()),
            new HashMap<>(terminationState.getNumberOfIterationsMap()),
            new HashSet<>(terminationState.getPathFormulas())));
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
    BooleanFormula newConstraintformula;

    if (location == null) {
      throw new UnsupportedOperationException("TransferRelation requires location information.");
    }
    if (location.isLoopStart()) {
      SSAMap currentValues = predicateState.getPathFormula().getSsa();
      if (terminationState.getStoredValues().containsKey(locationState)) {
        newConstraintformula =
            constructConstraintFormula(
                currentValues, terminationState.getNumberOfIterationsAtLoopHead(locationState));
        terminationState.setNewStoredValues(
            locationState,
            newConstraintformula,
            terminationState.getNumberOfIterationsAtLoopHead(locationState));
        terminationState.increaseNumberOfIterationsAtLoopHead(locationState);
      } else {
        newConstraintformula = constructConstraintFormula(currentValues, 0);
        terminationState.setNewStoredValues(locationState, newConstraintformula, 0);
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
  private BooleanFormula constructConstraintFormula(
      SSAMap pSSAMap, int pNumberOfIterationsAtLoopHead) {
    BooleanFormula extendedFormula = bfmgr.makeTrue();
    for (String variable : pSSAMap.allVariables()) {
      String newVariable = "__Q__" + variable;
      extendedFormula =
          bfmgr.and(
              extendedFormula,
              fmgr.assignment(
                  fmgr.makeVariable(
                      ctoFormulaConverter.getFormulaTypeFromCType(pSSAMap.getType(variable)),
                      newVariable,
                      pNumberOfIterationsAtLoopHead),
                  fmgr.makeVariable(
                      ctoFormulaConverter.getFormulaTypeFromCType(pSSAMap.getType(variable)),
                      variable,
                      pSSAMap.getIndex(variable))));
    }
    return extendedFormula;
  }

  private LocationState getLocationState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      if (state instanceof LocationState) {
        return (LocationState) state;
      }
    }
    throw new UnsupportedOperationException("TransferRelation requires location information.");
  }

  private PredicateAbstractState getPredicateState(Iterable<AbstractState> otherStates) {
    for (AbstractState state : otherStates) {
      if (state instanceof PredicateAbstractState) {
        return (PredicateAbstractState) state;
      }
    }
    throw new UnsupportedOperationException("TransferRelation requires path information.");
  }
}
