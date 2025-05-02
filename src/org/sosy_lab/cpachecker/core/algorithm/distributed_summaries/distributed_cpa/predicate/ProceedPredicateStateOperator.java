// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "distributedSummaries.predicate")
public class ProceedPredicateStateOperator implements ProceedOperator {

  @Option(
      name = "considerConditionCoverage",
      secure = true,
      description =
          "Check if the new state is already covered by the known postcondition or"
              + " violation-condition states")
  private boolean checkImplicationsOfKnownStates = true;

  private final Solver solver;

  public ProceedPredicateStateOperator(Solver pSolver, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    solver = pSolver;
  }

  @Override
  public DssMessageProcessing processForward(AbstractState pState) {
    return DssMessageProcessing.proceed();
  }

  @Override
  public DssMessageProcessing processBackward(
      AbstractState pState, Collection<AbstractState> pKnownStates)
      throws InterruptedException, SolverException {
    PredicateAbstractState predicateAbstractState = getPredicateState(pState);
    BooleanFormula condition = getFormulaRepresentation(predicateAbstractState);
    if (solver.isUnsat(condition)) {
      // FIXME: Add statistic whether this actually happens EVER.
      return DssMessageProcessing.stop();
    }
    if (checkImplicationsOfKnownStates) {
      List<BooleanFormula> knownConditions =
          getFormulaRepresentations(getPredicateStates(pKnownStates));
      for (BooleanFormula knownCondition : knownConditions) {
        if (solver.implies(condition, knownCondition)) {

          return DssMessageProcessing.stop();
        }
      }
    }
    return DssMessageProcessing.proceed();
  }

  private PredicateAbstractState getPredicateState(AbstractState pState) {
    return Objects.requireNonNull(
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class));
  }

  private List<PredicateAbstractState> getPredicateStates(Collection<AbstractState> pStates) {
    return pStates.stream().map(this::getPredicateState).toList();
  }

  private BooleanFormula getFormulaRepresentation(PredicateAbstractState pPredicateState) {
    if (pPredicateState.isAbstractionState()) {
      return pPredicateState.getAbstractionFormula().asFormula();
    } else {
      return pPredicateState.getPathFormula().getFormula();
    }
  }

  private List<BooleanFormula> getFormulaRepresentations(
      Collection<PredicateAbstractState> pPredicateStates) {
    return pPredicateStates.stream().map(this::getFormulaRepresentation).toList();
  }
}
