/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Canonicalizer;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class DefaultCanonicalizer implements Canonicalizer {

  private static class TransformDoubleNegations extends PatternBasedRule {

    // not not a
    // ====>
    // a

    public TransformDoubleNegations(Solver pSolver, SmtAstMatcher pMatcher) {
      super(pSolver, pMatcher);
    }

    @Override
    protected void setupPatterns() {
      premises.add(new PatternBasedPremise(
          or(
              match("not",
                  match("not",
                      matchAnyWithAnyArgsBind("f"))
          ))));
    }

    @Override
    protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
      BooleanFormula f = (BooleanFormula) Preconditions.checkNotNull(pAssignment.get("f"));

      return Collections.singleton(f);
    }

  }

  private static class TransformNotLessOrEq extends PatternBasedRule {

    // not a <= b
    // a, b: Integers
    // ====>
    // a > b

    public TransformNotLessOrEq(Solver pSolver, SmtAstMatcher pMatcher) {
      super(pSolver, pMatcher);
    }

    @Override
    protected void setupPatterns() {
      premises.add(new PatternBasedPremise(
          or(
              match("not",
                  match("<=",
                      matchNumeralExpressionBind("a"),
                      matchNumeralExpressionBind("b")))
          )));
    }

    @Override
    protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
      IntegerFormula a = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("a"));
      IntegerFormula b = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("b"));

      return Lists.newArrayList(ifm.greaterThan(a, b));
    }

  }

  private static class TranswformNotGreaterThan extends PatternBasedRule {

    // not a > b
    // a, b: Integers
    // ====>
    // a <= b

    public TranswformNotGreaterThan(Solver pSolver, SmtAstMatcher pMatcher) {
      super(pSolver, pMatcher);
    }

    @Override
    protected void setupPatterns() {
      premises.add(new PatternBasedPremise(
          or(
              match("not",
                  match(">",
                      matchNumeralExpressionBind("a"),
                      matchNumeralExpressionBind("b")))
          )));
    }

    @Override
    protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
      IntegerFormula a = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("a"));
      IntegerFormula b = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("b"));

      return Lists.newArrayList(ifm.lessOrEquals(a, b));
    }

  }

  private final List<PatternBasedRule> axioms;

  public DefaultCanonicalizer(Solver pSolver, SmtAstMatcher pSmtAstMatcher) {
    this.axioms = Lists.<PatternBasedRule>newArrayList();

    axioms.add(new TransformDoubleNegations(pSolver, pSmtAstMatcher));
    axioms.add(new TranswformNotGreaterThan(pSolver, pSmtAstMatcher));
    axioms.add(new TransformNotLessOrEq(pSolver, pSmtAstMatcher));
  }

  @Override
  public BooleanFormula canonicalize(BooleanFormula pPredicate) throws SolverException, InterruptedException {
    BooleanFormula result = pPredicate;
    for (PatternBasedRule a : axioms) {
      Set<BooleanFormula> returned = a.apply(result);
      if (returned.size() == 1) {
        result = returned.iterator().next();
      }
    }
    return result;
  }

  @Override
  public Collection<BooleanFormula> canonicalize(Collection<BooleanFormula> pPredicates) throws SolverException, InterruptedException {
    List<BooleanFormula> result = Lists.newArrayListWithCapacity(pPredicates.size());
    for (BooleanFormula p : pPredicates) {
      result.add(canonicalize(p));
    }
    return result;
  }
}
