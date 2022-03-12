// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.counterexamples;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A {@link CounterexampleFilter} that defines counterexamples as similar, if the unsat core of
 * their "negated paths" is the same. The "negated path" of a counterexample is defined as the
 * prefix of the path until before the last AssumeEdge, and then the negation of that last
 * AssumeEdge.
 *
 * <p>If the negated path is not infeasible, the counterexample is considered relevant (because no
 * interpolants can be computed).
 */
public class UnsatCoreCounterexampleFilter
    extends AbstractNegatedPathCounterexampleFilter<ImmutableList<BooleanFormula>> {

  private final LogManager logger;
  private final Solver solver;

  public UnsatCoreCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);
    logger = pLogger;
    @SuppressWarnings("resource")
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, UnsatCoreCounterexampleFilter.class);
    solver = predicateCpa.getSolver();
  }

  @Override
  protected Optional<ImmutableList<BooleanFormula>> getCounterexampleRepresentation(
      List<BooleanFormula> formulas) throws InterruptedException {

    try (ProverEnvironment thmProver =
        solver.newProverEnvironment(ProverOptions.GENERATE_UNSAT_CORE)) {

      for (BooleanFormula f : formulas) {
        thmProver.push(f);
      }

      if (!thmProver.isUnsat()) {
        // Negated path is not infeasible, cannot produce unsat core.
        // No filtering possible.
        return Optional.empty();
      }

      return Optional.of(ImmutableList.copyOf(thmProver.getUnsatCore()));

    } catch (SolverException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Solving failed on counterexample path, cannot filter this counterexample");
      return Optional.empty();
    }
  }
}
