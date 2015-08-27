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

import static org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternBuilder.*;

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * Implementation of the "SUB" inference rule:
 *    Substitution of equalities.
 */
public class SubstitutionRule extends PatternBasedRule {

  public SubstitutionRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    //  (= (select b (+ i 1)) 0)
    //  (= al (+ i 1))
    //
    //  e can be either ... or ...:
    //    al
    //    (+ i 1)
    //  x can be either ... or ...:
    //    al
    //    (+ i 1)

    // consider some axioms:
    //    i < n   <==>   i <= n-1
    //  not i >= n   <==>   i <= n-1

    premises.add(new PatternBasedPremise(
          GenericPatterns.f_of_x_matcher("f",
              and(
                  matchNumeralExpressionBind("x")),
              and(
                  matchAnyWithAnyArgsBind("y"))
        )));

    premises.add(new PatternBasedPremise(or(
        matchBind(">=", "eq",
            matchNumeralExpressionBind("x"),
            matchAnyWithAnyArgsBind("e")),
        matchBind("<=", "eq",
            matchNumeralExpressionBind("x"),
            matchAnyWithAnyArgsBind("e")),
        matchBind("=", "eq",
            matchNumeralExpressionBind("x"),
            matchAnyWithAnyArgsBind("e")),
        match("not",
            matchBind(">=", "lt",
                matchNumeralExpressionBind("x"),
                matchNumeralExpressionBind("e")))
    )));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment)
      throws SolverException, InterruptedException {

    final BooleanFormula f = (BooleanFormula) Preconditions.checkNotNull(pAssignment.get("f"));
    final Formula eq = pAssignment.get("eq");
    final Formula lt = pAssignment.get("lt");

    Preconditions.checkNotNull(pAssignment.get("e"));
    Preconditions.checkNotNull(pAssignment.get("x"));
    Preconditions.checkNotNull(pAssignment.get("y"));

    final Formula parentOfX = pAssignment.get("x.parent");
    if (parentOfX == null) {
      return false;
    }

    if (lt == null && eq == null) { return false; }
    if (lt != null && f.equals(lt)) { return false; }
    if (eq != null && f.equals(eq)) { return false; }
    if (eq != null && solver.isUnsat(bfm.and(f, (BooleanFormula)eq))) { return false; }

    return true;
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {

    final BooleanFormula f = (BooleanFormula) Preconditions.checkNotNull(pAssignment.get("f"));
    final Formula x = Preconditions.checkNotNull(pAssignment.get("x"));
    final Formula parentOfX = Preconditions.checkNotNull(pAssignment.get("x.parent"));

    Map<Formula, Formula> transformation = Maps.newHashMap();
    final Formula e = Preconditions.checkNotNull(pAssignment.get("e"));
    transformation.put(x, e);

    final Formula parentOfXPrime = matcher.substitute(parentOfX, transformation);

    Map<Formula, Formula> transformation2 = Maps.newHashMap();
    transformation2.put(parentOfX, parentOfXPrime);

    final BooleanFormula fPrime = matcher.substitute(f, transformation2);

    return Lists.newArrayList(fPrime);
  }

}
