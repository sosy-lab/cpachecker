// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Refines an acyclic block's exit precision directly from its exact formulas.
 *
 * <p>For an exit formula F and successor condition V with F AND V unsatisfiable, interpolation
 * yields F => I and I AND V => false. Adding I to the exit precision makes the next summary retain
 * this separation. Keep I as a compound predicate so Cartesian abstraction can retain the
 * separation; splitting it into atoms requires Boolean abstraction to reconstruct correlations.
 *
 * <p>Exploration contains no intermediate predicate abstractions. Its formulas and feasible targets
 * therefore remain valid when boundary predicates change. Outgoing summaries still need a fresh
 * abstraction with the current precision, and the caller must enforce the global error/ELSE gate.
 */
final class DssPredicateBoundaryRefinement {
  private final PredicateCPA cpa;
  private final CFANode exitLocation;
  private final DssPredicatePrecisionRefinement precisionRefinement;

  DssPredicateBoundaryRefinement(
      PredicateCPA pCpa, CFANode pExitLocation, Configuration pConfiguration)
      throws InvalidConfigurationException {
    cpa = pCpa;
    exitLocation = pExitLocation;
    precisionRefinement = new DssPredicatePrecisionRefinement(pConfiguration);
  }

  private PathFormula exactExitFormula(PredicateAbstractState predicate) {
    if (predicate.isAbstractionState()) {
      // A target can itself be at the block exit. Its SAT-only abstraction stores the checked
      // formula, while an ordinary exit remains a non-abstraction state.
      return predicate.getAbstractionFormula().getBlockFormula();
    }
    PathFormula path = predicate.getPathFormula();
    return path.withFormula(
        cpa.getSolver()
            .getFormulaManager()
            .getBooleanFormulaManager()
            .and(predicate.getAbstractionFormula().asInstantiatedFormula(), path.getFormula()));
  }

  private static <T> Optional<BooleanFormula> interpolant(
      InterpolatingProverEnvironment<T> prover, BooleanFormula prefix, BooleanFormula condition)
      throws SolverException, InterruptedException {
    T group = prover.push(prefix);
    prover.push(condition);
    return prover.isUnsat()
        ? Optional.of(prover.getInterpolant(ImmutableList.of(group)))
        : Optional.empty();
  }

  Precision refine(
      Collection<ARGState> exits, Collection<AbstractState> conditions, Precision precision)
      throws CPAException, InterruptedException {
    if (conditions.isEmpty()) {
      return precision;
    }
    var fmgr = cpa.getSolver().getFormulaManager();
    PredicatePrecision predicates =
        requireNonNull(Precisions.extractPrecisionByType(precision, PredicatePrecision.class));
    var learned = ImmutableListMultimap.<LocationInstance, AbstractionPredicate>builder();
    try {
      for (ARGState exit : exits) {
        var exitPredicate =
            requireNonNull(AbstractStates.extractStateByType(exit, PredicateAbstractState.class));
        PathFormula path = exactExitFormula(exitPredicate);
        int instance =
            exitPredicate.getAbstractionLocationsOnPath().getOrDefault(exitLocation, 0)
                + (exitPredicate.isAbstractionState() ? 0 : 1);
        for (AbstractState condition : conditions) {
          var predicate =
              requireNonNull(
                  AbstractStates.extractStateByType(condition, PredicateAbstractState.class));
          // getViolationCondition renames private variables on every use. They must not become
          // accidentally shared with the prefix or with another successor condition.
          BooleanFormula violation =
              fmgr.instantiate(predicate.getViolationCondition(fmgr), path.getSsa());
          try (var prover = cpa.getSolver().newProverEnvironmentWithInterpolation()) {
            Optional<BooleanFormula> result = interpolant(prover, path.getFormula(), violation);
            if (result.isEmpty()) {
              continue;
            }
            BooleanFormula itp = result.orElseThrow();
            if (fmgr.getBooleanFormulaManager().isTrue(itp)
                || !fmgr.instantiate(fmgr.uninstantiate(itp), path.getSsa()).equals(itp)) {
              // Only current exit values may become boundary predicates. An interpolant using
              // intermediate SSA values cannot be transported as a location predicate.
              continue;
            }
            learned.put(
                new LocationInstance(exitLocation, instance),
                cpa.getPredicateManager().getPredicateFor(itp));
          }
        }
      }
    } catch (SolverException e) {
      throw new CPAException("Exact boundary interpolation failed", e);
    }
    PredicatePrecision refined = precisionRefinement.addPredicates(predicates, learned.build());
    return Precisions.replaceByType(precision, refined, PredicatePrecision.class::isInstance);
  }

  ImmutableList<StateAndPrecision> abstractSummaries(
      Collection<StateAndPrecision> summaries, Precision precision)
      throws CPAException, InterruptedException {
    PredicatePrecision predicates =
        requireNonNull(Precisions.extractPrecisionByType(precision, PredicatePrecision.class));
    var result = ImmutableList.<StateAndPrecision>builder();
    try {
      for (StateAndPrecision summary : summaries) {
        ARGState state = (ARGState) summary.state();
        var predicate =
            requireNonNull(AbstractStates.extractStateByType(state, PredicateAbstractState.class));
        PathFormula path = exactExitFormula(predicate);
        CFANode location = requireNonNull(AbstractStates.extractLocation(state));
        checkState(location.equals(exitLocation), "Summary outside the block exit");
        var locations = predicate.getAbstractionLocationsOnPath();
        int instance =
            locations.getOrDefault(location, 0) + (predicate.isAbstractionState() ? 0 : 1);
        var abstraction =
            cpa.getPredicateManager()
                .buildAbstraction(
                    location,
                    AbstractStates.extractOptionalCallstackWraper(state),
                    path.getFormula(),
                    path,
                    predicates.getPredicates(location, instance));
        if (abstraction.isFalse()) {
          continue;
        }
        var replacement =
            PredicateAbstractState.mkAbstractionState(
                cpa.getPathFormulaManager().makeEmptyPathFormulaWithContextFrom(path),
                abstraction,
                locations.putAndCopy(location, instance));
        var components = new ArrayList<AbstractState>();
        for (AbstractState component :
            ((CompositeState) state.getWrappedState()).getWrappedStates()) {
          components.add(component == predicate ? replacement : component);
        }
        // Publishing needs an abstract exit state, while the cached exploration and its witness
        // keep the original exact ARG untouched.
        result.add(
            new StateAndPrecision(new ARGState(new CompositeState(components), null), precision));
      }
    } catch (SolverException e) {
      throw new CPAException("Exact exit abstraction failed", e);
    }
    return result.build();
  }
}
