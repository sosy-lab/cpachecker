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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.AbstractSetBasedCounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * A {@link CounterexampleFilter} that defines counterexamples as similar,
 * if the interpolants of their "negated paths" contain the same set of predicates.
 * The "negated path" of a counterexample is defined as the prefix of the path
 * until before the last AssumeEdge, and then the negation of that last AssumeEdge.
 *
 * If the negated path is not infeasible, the counterexample is considered
 * relevant (because no interpolants can be computed).
 * The location of the inteprolant predicates along the path is ignored,
 * all predicates are merged into a single set.
 */
public class InterpolantPredicatesCounterexampleFilter extends AbstractSetBasedCounterexampleFilter<Set<AbstractionPredicate>> {

  private final LogManager logger;

  private final PathFormulaManager pfmgr;
  private final FormulaManagerFactory solverFactory;
  private final PredicateAbstractionManager predAbsMgr;

  public InterpolantPredicatesCounterexampleFilter(Configuration pConfig, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);

    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(InterpolantPredicatesCounterexampleFilter.class.getSimpleName() + " needs a PredicateCPA");
    }

    logger = pLogger;
    pfmgr = predicateCpa.getPathFormulaManager();
    solverFactory = predicateCpa.getFormulaManagerFactory();
    predAbsMgr = predicateCpa.getPredicateManager();
  }

  @Override
  protected Optional<Set<AbstractionPredicate>> getCounterexampleRepresentation(CounterexampleInfo counterexample) throws InterruptedException {
    List<CFAEdge> edges = counterexample.getTargetPath().asEdgesList();

    int cutPoint = edges.size() - 1; // Position of last AssumeEdge in path
    for (CFAEdge edge : Lists.reverse(edges)) {
      if (edge instanceof AssumeEdge) {
        break;
      }
      cutPoint--;
    }
    if (cutPoint < 0) {
      // no AssumEdge in path, cannot use this filter
      return Optional.absent();
    }

    AssumeEdge lastAssumeEdge = (AssumeEdge)edges.get(cutPoint);
    List<CFAEdge> prefix = edges.subList(0, cutPoint);

    PathFormula pf = pfmgr.makeEmptyPathFormula();
    List<BooleanFormula> formulas = new ArrayList<>(prefix.size() + 1);

    try {
      for (CFAEdge edge : prefix) {
        pf = pfmgr.makeAnd(pf, edge);
        formulas.add(pf.getFormula());
        pf = pfmgr.makeEmptyPathFormula(pf);
      }
      pf = pfmgr.makeAnd(pf, CFAUtils.getComplimentaryAssumeEdge(lastAssumeEdge));
      formulas.add(pf.getFormula());

    } catch (CPATransferException e) {
      logger.logUserException(Level.WARNING, e, "Failed to filter counterexample");
      return Optional.absent();
    }

    return Optional.fromNullable(getInterpolantPredicates(formulas));
  }

  private <T> Set<AbstractionPredicate> getInterpolantPredicates(List<BooleanFormula> formulas) throws InterruptedException {

    try (@SuppressWarnings("unchecked")
         InterpolatingProverEnvironment<T> itpProver =
           (InterpolatingProverEnvironment<T>) solverFactory.newProverEnvironmentWithInterpolation(false)) {

      List<T> itpGroupIds = new ArrayList<>(formulas.size());
      for (BooleanFormula f : formulas) {
        itpGroupIds.add(itpProver.push(f));
      }

      if (!itpProver.isUnsat()) {
        // Negated path is not infeasible, cannot produce interpolants.
        // No filtering possible.
        return null;
      }

      Set<AbstractionPredicate> predicates = new HashSet<>();
      for (int i = 1; i < itpGroupIds.size(); i++) {
        BooleanFormula itp = itpProver.getInterpolant(itpGroupIds.subList(0, i));
        predicates.addAll(predAbsMgr.extractPredicates(itp));
      }
      return ImmutableSet.copyOf(predicates);
    }
  }
}