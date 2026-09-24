// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateViolationConditionOperator implements ViolationConditionOperator {

  private final PathFormulaManagerImpl backwardManager;
  private final PredicateCPA cpa;
  private final boolean hasRootAsPredecessor;

  public PredicateViolationConditionOperator(
      PathFormulaManagerImpl pBackwardManager, PredicateCPA pCpa, boolean pHasRootAsPredecessor) {
    backwardManager = pBackwardManager;
    cpa = pCpa;
    hasRootAsPredecessor = pHasRootAsPredecessor;
  }

  /**
   * Adds the pointer-target set the forward analysis built along the path to the context of {@code
   * pFormula}. The forward states of the path know which bases exist and which fields are tracked;
   * the backward walk needs the same knowledge to encode accesses to them.
   */
  private PathFormula withPointerTargetSetOf(ARGPath pPath, PathFormula pFormula)
      throws InterruptedException {
    PointerTargetSet forward = null;
    for (ARGState state : pPath.asStatesList()) {
      PredicateAbstractState predicateState =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      if (predicateState != null) {
        forward = predicateState.getPathFormula().getPointerTargetSet();
      }
    }
    if (forward == null) {
      return pFormula;
    }
    SSAMapBuilder ssa = pFormula.getSsa().builder();
    PointerTargetSet merged =
        backwardManager.mergePts(pFormula.getPointerTargetSet(), forward, ssa);
    return pFormula.withContext(ssa.build(), merged);
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    PathFormula result;
    if (pPreviousCondition.isEmpty()) {
      result = backwardManager.makeEmptyPathFormula();
    } else {
      PredicateAbstractState counterexampleState =
          Objects.requireNonNull(
              AbstractStates.extractStateByType(
                  pPreviousCondition.orElseThrow(), PredicateAbstractState.class));
      if (counterexampleState.isAbstractionState()) {
        result = counterexampleState.getAbstractionFormula().getBlockFormula();
      } else {
        result = counterexampleState.getPathFormula();
      }
    }
    // Walking the path backwards has to know the memory layout that the forward analysis of this
    // block discovered along it. Starting from an empty pointer-target set loses the bases and the
    // tracked fields, so a write through a pointer to a field of a composite cannot be encoded and
    // the condition comes out unsatisfiable even though the path is feasible. The forward states of
    // the path carry that layout, so seed the walk with it.
    result = withPointerTargetSetOf(pARGPath, result);
    for (CFAEdge cfaEdge : pARGPath.getFullPath().reverse()) {
      result = backwardManager.makeAnd(result, cfaEdge);
    }
    if (hasRootAsPredecessor) {
      if (cpa.getSolver().isUnsat(result.getFormula())) {
        return Optional.empty();
      }
    }
    return Optional.of(
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            result,
            (PredicateAbstractState)
                cpa.getInitialState(
                    Objects.requireNonNull(
                        AbstractStates.extractLocation(pARGPath.getFirstState())),
                    StateSpacePartition.getDefaultPartition())));
  }
}
