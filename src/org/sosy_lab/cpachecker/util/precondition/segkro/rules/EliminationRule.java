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
import java.util.Set;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class EliminationRule extends PatternBasedRule {

  public EliminationRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    premises.add(new PatternBasedPremise(
          or(
              match(">=", // c1*e + e1 >= 0
                  match("+",
                      match("*",
                          matchNullaryBind("c1"),
                          matchAnyWithAnyArgsBind("eX")),
                      matchAnyWithAnyArgsBind("e1")),
                  matchNullary("0"))
                )));

    premises.add(new PatternBasedPremise(
          or(
              match(">=", // -c2*e + e2 >= 0
                  and (
                    GenericPatterns.substraction(
                        matchAnyWithAnyArgsBind("e2"),
                        match("*",
                            matchNullaryBind("c2"),
                            matchAnyWithAnyArgsBind("eX"))),
                    matchNullary("0"))),
              match(">=", // -c2*e + e2 >= 0
                  match("+",
                      match("*",
                          match("-",
                              matchNullary("0"),
                              matchNullaryBind("c2")),
                          matchAnyWithAnyArgsBind("eX")),
                      matchAnyWithAnyArgsBind("e2")),
                  matchNullary("0"))
              )));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment)
      throws SolverException, InterruptedException {

    final IntegerFormula zero = fmv.getIntegerFormulaManager().makeNumber(0);
    final IntegerFormula c1 = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("c1"));
    final IntegerFormula c2 = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("c2"));

    return !solver.isUnsat(
        bfm.and(
            ifm.greaterThan(c1, zero),
            ifm.greaterThan(c2, zero)));
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {

    final IntegerFormula zero = fmv.getIntegerFormulaManager().makeNumber(0);
    final IntegerFormula c1 = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("c1"));
    final IntegerFormula c2 = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("c2"));
    final IntegerFormula e1 = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("e1"));
    final IntegerFormula e2 = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("e2"));

    return Lists.newArrayList(
        ifm.greaterOrEquals(
            ifm.add(
                ifm.multiply(c2, e1),
                ifm.multiply(c1, e2)),
            zero));
  }

  @Override
  protected boolean isValidConclusion(Collection<BooleanFormula> pConjunctiveInputPredicates,
      Set<BooleanFormula> pResult) throws SolverException, InterruptedException {

    return true;
  }


}
