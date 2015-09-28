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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.Cartesian;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Premise;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatchResult;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public abstract class PatternBasedRule extends AbstractRule {

  protected final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;
  protected final QuantifiedFormulaManagerView qfm;
  protected final ArrayFormulaManagerView afm;
  protected final BooleanFormulaManagerView bfm;
  protected final FormulaManagerView fmv;

  final StatTimer constraintCheckTimer = new StatTimer(StatKind.SUM, "Constraint checking");
  final StatTimer conclusionTimer = new StatTimer(StatKind.SUM, "Concluding");
  final StatTimer matchingTimer = new StatTimer(StatKind.SUM, "Matching");
  final StatTimer overallTimer = new StatTimer(StatKind.SUM, "Overall");
  final StatTimer conclusionValidationTimer = new StatTimer(StatKind.SUM, "Validation");

  public PatternBasedRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);

    fmv = pSolver.getFormulaManager();
    bfm = fmv.getBooleanFormulaManager();
    ifm = fmv.getIntegerFormulaManager();
    qfm = fmv.getQuantifiedFormulaManager();
    afm = fmv.getArrayFormulaManager();

    setupPatterns();
  }

  protected abstract void setupPatterns();

  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment) throws SolverException, InterruptedException {
    return true;
  }

  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    return Collections.emptyList();
  }

  @Override
  public Set<BooleanFormula> apply(BooleanFormula pInput) throws SolverException, InterruptedException {
    return apply(fmv.extractLiterals(pInput), HashMultimap.<String, Formula>create());
  }

  protected Formula substituteInParent(
      final Formula pParent,
      final Formula pToSubstitute,
      final Formula pSubstituteBy,
      final Formula pReplaceParentIn) {

    Map<Formula, Formula> transformation = Maps.newHashMap();
    transformation.put(pToSubstitute, pSubstituteBy);

    final Formula parentPrime = matcher.substitute(pParent, transformation);

    Map<Formula, Formula> transformation2 = Maps.newHashMap();
    transformation2.put(pParent, parentPrime);

    return matcher.substitute(pReplaceParentIn, transformation2);
  }

  @Override
  public Set<BooleanFormula> apply(
      final Collection<BooleanFormula> pConjunctiveInputPredicates,
      final Multimap<String, Formula> pMatchingBindings)
          throws SolverException, InterruptedException {

    HashSet<BooleanFormula> result = Sets.newHashSet();

    final int premiseCount = getPremises().size();
    Verify.verify(premiseCount != 0);

    final List<Collection<BooleanFormula>> dimensions = new ArrayList<>(premiseCount);
    for (int i=0; i<premiseCount; i++) {
      dimensions.add(pConjunctiveInputPredicates);
    }

    for (List<BooleanFormula> tuple: Cartesian.product(dimensions)) {
      final Set<BooleanFormula> inferred = applyWithInputRelatingPremises(tuple, pMatchingBindings);
      result.addAll(inferred);
    }

    return result;
  }

  private Collection<Map<String, Formula>> getAllAssignments(Multimap<String, Formula> pFromBindings) {
    final Set<String> boundVariables = pFromBindings.keySet();
    final List<Collection<Formula>> dimensions = new ArrayList<>(boundVariables.size());
    for (String var: boundVariables) {
      dimensions.add(pFromBindings.get(var));
    }

    Collection<Map<String, Formula>> result = Lists.newArrayList();
    for (List<Formula> x: Cartesian.product(dimensions)) {
      final Map<String, Formula> tuple = Maps.newHashMap();
      final Iterator<Formula> xIt = x.iterator();
      for (String var: boundVariables) {
        Formula f = xIt.next();
        tuple.put(var, f);
      }
      result.add(tuple);
    }

    return result;
  }

  @Override
  public Set<BooleanFormula> applyWithInputRelatingPremises(List<BooleanFormula> pConjunctiveInputPredicates)
      throws SolverException, InterruptedException {

    return applyWithInputRelatingPremises(pConjunctiveInputPredicates, HashMultimap.<String, Formula>create());
  }

  public Set<BooleanFormula> applyWithInputRelatingPremises(
      final Collection<BooleanFormula> pConjunctiveInputPredicates,
      final Multimap<String, Formula> pMatchingBindings)
          throws SolverException, InterruptedException {

    overallTimer.start();
    try {

      Preconditions.checkArgument(pConjunctiveInputPredicates.size() == getPremises().size());

      final Set<BooleanFormula> result = Sets.newHashSet();

      // Check premises -------------------
      boolean allPremisesMatch = true;
      final Multimap<String, Formula> matchingBindings = HashMultimap.create(pMatchingBindings);
      Iterator<BooleanFormula> it = pConjunctiveInputPredicates.iterator();

      for (Premise p: getPremises()) {
        assert p instanceof PatternBasedPremise;
        final PatternBasedPremise pp = (PatternBasedPremise) p;
        final BooleanFormula predicate = it.next();

        matchingTimer.start();
        final SmtAstMatchResult matchingResult = matcher.perform(
            pp.getPatternSelection(),
            null,
            predicate,
            Optional.of(matchingBindings));
        matchingTimer.stop();

        if (!matchingResult.matches()) {
          allPremisesMatch = false;
          break;
        }

        matchingResult.appendBindingsTo(matchingBindings);
      }

      // Derive conclusion ------------------
      if (!allPremisesMatch) {
        return Collections.emptySet();
      }

      for (Map<String, Formula> tuple: getAllAssignments(matchingBindings)) {
        // Check whether the tuple satisfies the constraints...
        constraintCheckTimer.start();
        final boolean constraintsSatisfied = satisfiesConstraints(tuple);
        constraintCheckTimer.stop();
        if (!constraintsSatisfied) {
          continue;
        }

        // Now we can instantiate the conclusion...
        conclusionTimer.start();
        result.addAll(deriveConclusion(tuple));
        conclusionTimer.stop();
      }


      // The conclusion must be a valid over-approximation
      conclusionValidationTimer.start();
      boolean isValidOverapproximation = isValidConclusion(pConjunctiveInputPredicates, result);
      conclusionValidationTimer.stop();

      if (!isValidOverapproximation) {
        return Collections.emptySet();
      }

      return result;

    } finally {
      overallTimer.stop();
    }
  }

  protected boolean isValidConclusion(
      final Collection<BooleanFormula> pConjunctiveInputPredicates,
      final Set<BooleanFormula> pResult)
          throws SolverException, InterruptedException {

    return solver.isUnsat(bfm.not(bfm.implication(
        bfm.and(ImmutableList.copyOf(pConjunctiveInputPredicates)),
        bfm.and(ImmutableList.copyOf(pResult)))));
  }

}
