// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.Lists;
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
    for (CFAEdge cfaEdge : Lists.reverse(pARGPath.getFullPath())) {
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
