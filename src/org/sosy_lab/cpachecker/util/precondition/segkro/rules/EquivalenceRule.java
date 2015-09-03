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
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.collect.Lists;

/**
 * Implementation of the "EQ" inference rule:
 *    Equality of arithmetic expressions; simplification.
 */
public class EquivalenceRule extends PatternBasedRule {

  public EquivalenceRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    premises.add(new PatternBasedPremise(
        or(
          match("<", // i < x
              matchAnyWithAnyArgsBind("i"),
              matchNumeralVariableBind("x")
              ),
          match(">=", // x - e >= 0
              and (
                  GenericPatterns.substraction(
                      matchNullaryBind("x"),
                      matchAnyWithAnyArgsBind("e")),
                  matchNullary("0"))
          ))));

    premises.add(new PatternBasedPremise(
        or(
          match(">=", // i + 1 >= x
              matchBind("+", "e",
                  matchAnyWithAnyArgsBind("i"),
                  matchNullary("1")),
              matchNumeralVariableBind("x")
              ),
          match(">=", // (-x) + e >= 0
              and (
                GenericPatterns.substraction(
                    matchAnyWithAnyArgsBind("e"),
                    matchNullaryBind("x")),
                matchNullary("0")))
          )));

  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment)
      throws SolverException, InterruptedException {
    return true;
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    final IntegerFormula e = (IntegerFormula) pAssignment.get("e");
    final IntegerFormula x = (IntegerFormula) pAssignment.get("x");

    return Lists.newArrayList(
        ifm.equal(x, e));
  }
}
