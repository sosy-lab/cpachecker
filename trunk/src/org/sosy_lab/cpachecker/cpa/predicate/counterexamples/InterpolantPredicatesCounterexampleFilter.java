// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.counterexamples;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A {@link CounterexampleFilter} that defines counterexamples as similar, if the interpolants of
 * their "negated paths" contain the same set of predicates. The "negated path" of a counterexample
 * is defined as the prefix of the path until before the last AssumeEdge, and then the negation of
 * that last AssumeEdge.
 *
 * <p>If the negated path is not infeasible, the counterexample is considered relevant (because no
 * interpolants can be computed). The location of the inteprolant predicates along the path is
 * ignored, all predicates are merged into a single set.
 */
public class InterpolantPredicatesCounterexampleFilter
    extends AbstractNegatedPathCounterexampleFilter<ImmutableSet<AbstractionPredicate>> {

  private final LogManager logger;

  private final Solver solver;
  private final PredicateAbstractionManager predAbsMgr;

  public InterpolantPredicatesCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);
    logger = pLogger;

    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(
            pCpa, PredicateCPA.class, InterpolantPredicatesCounterexampleFilter.class);
    solver = predicateCpa.getSolver();
    predAbsMgr = predicateCpa.getPredicateManager();
  }

  @Override
  protected Optional<ImmutableSet<AbstractionPredicate>> getCounterexampleRepresentation(
      List<BooleanFormula> pFormulas) throws InterruptedException {
    return getCounterexampleRepresentation0(pFormulas);
  }

  private <T> Optional<ImmutableSet<AbstractionPredicate>> getCounterexampleRepresentation0(
      List<BooleanFormula> formulas) throws InterruptedException {

    try (@SuppressWarnings("unchecked")
        InterpolatingProverEnvironment<T> itpProver =
            (InterpolatingProverEnvironment<T>) solver.newProverEnvironmentWithInterpolation()) {

      List<T> itpGroupIds = new ArrayList<>(formulas.size());
      for (BooleanFormula f : formulas) {
        itpGroupIds.add(itpProver.push(f));
      }

      if (!itpProver.isUnsat()) {
        // Negated path is not infeasible, cannot produce interpolants.
        // No filtering possible.
        return Optional.empty();
      }

      Set<AbstractionPredicate> predicates = new HashSet<>();
      for (int i = 1; i < itpGroupIds.size(); i++) {
        BooleanFormula itp = itpProver.getInterpolant(itpGroupIds.subList(0, i));
        predicates.addAll(predAbsMgr.getPredicatesForAtomsOf(itp));
      }
      return Optional.of(ImmutableSet.copyOf(predicates));
    } catch (SolverException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Interpolation failed on counterexample path, cannot filter this counterexample");
      return Optional.empty();
    }
  }
}
