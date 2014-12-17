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

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.collect.Lists;


public class ExtendRightRule extends PatternBasedRule {

  public ExtendRightRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    // (exists ((x Int)) (and (not (= (select b x) 0)) (>= x i) (<= x j)))

    premises.add(new PatternBasedPremise(
        or(
          matchExistsQuant(
              and(
                GenericPatterns.f_of_x("f", quantified("x")),
                match(">=",
                    matchAnyWithAnyArgsBind(quantified("x")),
                    matchAnyWithAnyArgsBind("i")),
                match("<=",
                    matchAnyWithAnyArgsBind(quantified("x")),
                    matchAnyWithAnyArgsBind("j"))))
        )));

    premises.add(new PatternBasedPremise(or(
        match("<=",
            matchAnyWithAnyArgsBind("j"),
            matchAnyWithAnyArgsBind("k")))));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment) throws SolverException, InterruptedException {
    return true;
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    final BooleanFormula f = (BooleanFormula) pAssignment.get("f");
    final IntegerFormula i = (IntegerFormula) pAssignment.get("i");
    final IntegerFormula k = (IntegerFormula) pAssignment.get("k");
    final IntegerFormula x = (IntegerFormula) pAssignment.get(quantified("x"));

    final BooleanFormula xConstraint =  bfm.and(
        ifm.greaterOrEquals(x, i),
        ifm.lessOrEquals(x, k));

    return Lists.newArrayList(
        qfm.exists(Lists.newArrayList(x), bfm.and(f, xConstraint)));
  }
}
