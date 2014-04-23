/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate.counterexamples;

import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A {@link CounterexampleFilter} that defines counterexamples as similar,
 * if the unsat core of their "negated paths" is the same.
 * The "negated path" of a counterexample is defined as the prefix of the path
 * until before the last AssumeEdge, and then the negation of that last AssumeEdge.
 *
 * If the negated path is not infeasible, the counterexample is considered
 * relevant (because no interpolants can be computed).
 */
public class UnsatCoreCounterexampleFilter extends AbstractNegatedPathCounterexampleFilter<ImmutableList<BooleanFormula>> {

  private final FormulaManagerFactory solverFactory;

  public UnsatCoreCounterexampleFilter(Configuration pConfig, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);

    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(UnsatCoreCounterexampleFilter.class.getSimpleName() + " needs a PredicateCPA");
    }

    solverFactory = predicateCpa.getFormulaManagerFactory();
  }

  @Override
  protected Optional<ImmutableList<BooleanFormula>> getCounterexampleRepresentation(List<BooleanFormula> formulas) throws InterruptedException {

    try (ProverEnvironment thmProver = solverFactory.newProverEnvironment(false, true)) {

      for (BooleanFormula f : formulas) {
        thmProver.push(f);
      }

      if (!thmProver.isUnsat()) {
        // Negated path is not infeasible, cannot produce unsat core.
        // No filtering possible.
        return Optional.absent();
      }

      return Optional.of(ImmutableList.copyOf(thmProver.getUnsatCore()));
    }
  }
}