// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.Lists;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationCondition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationConditionSynthesizer;
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

public class PredicateStateViolationConditionSynthesizer implements ViolationConditionSynthesizer {

  private final PathFormulaManagerImpl backwardManager;
  private final PredicateCPA predicateCPA;

  public PredicateStateViolationConditionSynthesizer(
      PathFormulaManagerImpl pBackwardManager, PredicateCPA pPredicateCPA) {
    backwardManager = pBackwardManager;
    predicateCPA = pPredicateCPA;
  }

  @Override
  public ViolationCondition computeViolationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws CPATransferException, InterruptedException, SolverException {
    PathFormula result;
    if (pPreviousCondition == null) {
      result = backwardManager.makeEmptyPathFormula();
    } else {
      PredicateAbstractState counterexampleState =
          Objects.requireNonNull(
              AbstractStates.extractStateByType(pPreviousCondition, PredicateAbstractState.class));
      if (counterexampleState.isAbstractionState()) {
        result = counterexampleState.getAbstractionFormula().getBlockFormula();
      } else {
        result = counterexampleState.getPathFormula();
      }
    }
    for (CFAEdge cfaEdge : Lists.reverse(pARGPath.getFullPath())) {
      result = backwardManager.makeAnd(result, cfaEdge);
    }
    if (predicateCPA.getSolver().isUnsat(result.getFormula())) {
      return ViolationCondition.infeasibleCondition();
    }
    return ViolationCondition.feasibleCondition(
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            result,
            (PredicateAbstractState)
                predicateCPA.getInitialState(
                    Objects.requireNonNull(
                        AbstractStates.extractLocation(pARGPath.getFirstState())),
                    StateSpacePartition.getDefaultPartition())));
  }
}
