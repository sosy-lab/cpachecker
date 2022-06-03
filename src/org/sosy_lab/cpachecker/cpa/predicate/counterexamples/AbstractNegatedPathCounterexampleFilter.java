// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.counterexamples;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.ForOverride;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.AbstractSetBasedCounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * An abstract {@link CounterexampleFilter} implementation for filters that use an SMT solver to
 * generate a representation of the "negated path" of the counterexample. The "negated path" of a
 * counterexample is defined as the prefix of the path until before the last AssumeEdge, and then
 * the negation of that last AssumeEdge.
 */
abstract class AbstractNegatedPathCounterexampleFilter<T>
    extends AbstractSetBasedCounterexampleFilter<T> {

  private final LogManager logger;

  private final PathFormulaManager pfmgr;

  protected AbstractNegatedPathCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);

    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(
            pCpa, PredicateCPA.class, InterpolantPredicatesCounterexampleFilter.class);
    logger = pLogger;
    pfmgr = predicateCpa.getPathFormulaManager();
  }

  @Override
  protected Optional<T> getCounterexampleRepresentation(CounterexampleInfo counterexample)
      throws InterruptedException {
    List<CFAEdge> edges = counterexample.getTargetPath().getInnerEdges();

    int cutPoint = edges.size() - 1; // Position of last AssumeEdge in path
    for (CFAEdge edge : Lists.reverse(edges)) {
      if (edge instanceof AssumeEdge) {
        break;
      }
      cutPoint--;
    }
    if (cutPoint < 0) {
      // no AssumEdge in path, cannot use this filter
      return Optional.empty();
    }

    AssumeEdge lastAssumeEdge = (AssumeEdge) edges.get(cutPoint);
    List<CFAEdge> prefix = edges.subList(0, cutPoint);

    PathFormula pf = pfmgr.makeEmptyPathFormula();
    List<BooleanFormula> formulas = new ArrayList<>(prefix.size() + 1);

    try {
      for (CFAEdge edge : prefix) {
        pf = pfmgr.makeAnd(pf, edge);
        formulas.add(pf.getFormula());
        pf = pfmgr.makeEmptyPathFormulaWithContextFrom(pf);
      }
      pf = pfmgr.makeAnd(pf, CFAUtils.getComplimentaryAssumeEdge(lastAssumeEdge));
      formulas.add(pf.getFormula());

    } catch (CPATransferException e) {
      logger.logUserException(Level.WARNING, e, "Failed to filter counterexample");
      return Optional.empty();
    }

    return getCounterexampleRepresentation(formulas);
  }

  /**
   * This method needs to produce an immutable representation of each counterexample. If this filter
   * does not manage to produce a meaningful representation of the current path, it may return
   * {@link Optional#empty()}. In this case, the counterexample is considered relevant. This method
   * is given as input a list of {@link BooleanFormula}s that represent the "negated path" of this
   * counterexample.
   *
   * @param negatedPath A list of formulas, guaranteed to be not null.
   * @return An immutable representation of the counterexample, needs to have proper implementations
   *     of {@link Object#equals(Object)} and {@link Object#hashCode()}, or {@link
   *     Optional#empty()}.
   */
  @ForOverride
  protected abstract Optional<T> getCounterexampleRepresentation(List<BooleanFormula> negatedPath)
      throws InterruptedException;
}
