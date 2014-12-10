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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.Cartesian;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Premise;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstMatchResult;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstMatcher;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public abstract class PatternBasedRule extends AbstractRule {

  protected final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm;
  protected final QuantifiedFormulaManagerView qfm;
  protected final ArrayFormulaManagerView afm;
  protected final BooleanFormulaManagerView bfm;

  public PatternBasedRule(FormulaManager pFm, FormulaManagerView pFmv, Solver pSolver, SmtAstMatcher pMatcher) {
    super(pFm, pFmv, pSolver, pMatcher);

    bfm = pFmv.getBooleanFormulaManager();
    ifm = pFmv.getIntegerFormulaManager();
    qfm = pFmv.getQuantifiedFormulaManager();
    afm = pFmv.getArrayFormulaManager();

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
  public Set<BooleanFormula> apply(BooleanFormula pInput) {
    return apply(fmv.extractAtoms(pInput, false, false));
  }

  @Override
  public Set<BooleanFormula> apply(Collection<BooleanFormula> pConjunctiveInputPredicates) {
    // Check premises
    for (Premise p: getPremises()) {
      assert p instanceof PatternBasedPremise;
      PatternBasedPremise pp = (PatternBasedPremise) p;

      // matcher.perform(pp.getPatternSelection(), pF)
    }
    return null;
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
  public Set<BooleanFormula> applyWithInputRelatingPremises(List<BooleanFormula> pConjunctiveInputPredicates) throws SolverException, InterruptedException {
    Preconditions.checkArgument(pConjunctiveInputPredicates.size() == getPremises().size());

    final Set<BooleanFormula> result = Sets.newHashSet();

    // Check premises -------------------
    boolean allPremisesMatch = true;
    Multimap<String, Formula> matchingBindings = HashMultimap.create();
    Iterator<BooleanFormula> it = pConjunctiveInputPredicates.iterator();

    for (Premise p: getPremises()) {
      assert p instanceof PatternBasedPremise;
      final PatternBasedPremise pp = (PatternBasedPremise) p;
      final BooleanFormula predicate = it.next();

      final SmtAstMatchResult matchingResult = matcher.perform(
          pp.getPatternSelection(),
          predicate,
          Optional.of(matchingBindings));

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
      if (!satisfiesConstraints(tuple)) {
        continue;
      }

      // Now we can instantiate the conclusion...
      result.addAll(deriveConclusion(tuple));
    }

    return result;
  }

}
