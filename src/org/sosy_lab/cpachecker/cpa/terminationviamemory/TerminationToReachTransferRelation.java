// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;

public class TerminationToReachTransferRelation extends SingleEdgeTransferRelation {
  private final BooleanFormulaManagerView bfmgr;
  public TerminationToReachTransferRelation(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state,
      Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    CFANode location = AbstractStates.extractLocation(pState);
    LocationState locationState = getLocationState(pOtherStates);
    PredicateAbstractState predicateState = getPredicateState(pOtherStates);
    TerminationToReachState terminationState = (TerminationToReachState) pState;
    if (location == null) {
      throw new UnsupportedOperationException("TransferRelation requires location information.");
    }
    if (location.isLoopStart()) {
      SSAMap currentValues = predicateState.getPathFormula().getSsa();
      if (terminationState.getStoredValues().containsKey(locationState)) {
        BooleanFormula newConstraintformula = constructConstraintFormula(
            currentValues,
            terminationState.getStoredValues().get(locationState),
            terminationState.getNumberOfIterationsAtLoopHead(locationState));
        terminationState.setNewStoredValues(locationState, newConstraintformula);
        terminationState.increaseNumberOfIterationsAtLoopHead(locationState);
      } else {
        BooleanFormula newConstraintformula = constructConstraintFormula(
            currentValues,
            bfmgr.makeTrue(),
            0);
        terminationState.setNewStoredValues(locationState, newConstraintformula);
        terminationState.increaseNumberOfIterationsAtLoopHead(locationState);
      }
    }

    return super.strengthen(pState, pOtherStates, pCfaEdge, precision);
  }
  /** Stores new assumptions about value of variables seen.
   * For instance, if there is x@2 in SSAmap then it will add a condition to the stored values:
   * __Q__x0 = x@2
   * Where the storing variables are of the form __Q__[name of variable][number of loop iterations].
   * */
  private BooleanFormula constructConstraintFormula(SSAMap pCurrentValues,
                              BooleanFormula pAssignment,
                              int pNumberOfIterationsAtLoopHead) {
    BooleanFormula extendedFormula = pAssignment;
    for (String variable : pCurrentValues.allVariables()) {
      String newVariable = "__Q__" + variable + "#" + pNumberOfIterationsAtLoopHead;
      extendedFormula = bfmgr.and(ImmutableList.of(extendedFormula,
          bfmgr.equivalence(bfmgr.makeVariable(newVariable, pNumberOfIterationsAtLoopHead),
                            bfmgr.makeVariable(variable, pCurrentValues.getIndex(variable)))));
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
