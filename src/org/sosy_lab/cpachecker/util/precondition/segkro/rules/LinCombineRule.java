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
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class LinCombineRule extends PatternBasedRule {

  private final IntegerFormula zero;
  private final IntegerFormula one;


  public LinCombineRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);

    zero = ifm.makeNumber(0);
    one = ifm.makeNumber(1);
  }

  @Override
  protected void setupPatterns() {
    // b > e
    //     e >= a
    // ------------>
    //     b > a

    // b >= e
    //      e >= a
    // ------------>
    //     b >= a

    // b >= e
    //      e > a
    // ------------>
    //     b > a

    // b - e > 0
    //     e - a >= 0
    // ------------>
    //     b > a

    //ATTENTION: We assume that we operate on canonicalized predicates!!

    premises.add(new PatternBasedPremise(
        or (
          matchBind(">=", "geq1", // e >= a
              matchAnyWithAnyArgsBind("e"),
              matchAnyWithAnyArgsBind("a")),

          matchBind(">=", "geq1", // e - a >= 0
              and(
                  GenericPatterns.substraction("e", "a"),
                  matchAnyWithAnyArgsBind("zero"))),

          match(">", // e > a
              matchAnyWithAnyArgsBind("e"),
              matchAnyWithAnyArgsBind("a")),

          match(">",  // e - a >= 0
              and(
                  GenericPatterns.substraction("e", "a"),
                  matchAnyWithAnyArgsBind("zero")))

          )));

    premises.add(new PatternBasedPremise(
        or (
          match(">",  // b > e
              matchAnyWithAnyArgsBind("b"),
              matchAnyWithAnyArgsBind("e")),

          match(">", // b - e > 0
              and(
                  GenericPatterns.substraction("b", "e"),
                  matchAnyWithAnyArgsBind("zero"))),

          matchBind(">=", "geq2", // b >= e - 1
              matchAnyWithAnyArgsBind("b"),
              matchAnyWithAnyArgsBind("eMinusOne"))

          )));
  }

  protected boolean checkIfAvailable(Map<String, Formula> pAssignment,
      String pVar, IntegerFormula pIsEqualTo)
          throws SolverException, InterruptedException {

    final Formula f = pAssignment.get(pVar);
    if (f == null) {
      return true;
    }

    if (!(f instanceof IntegerFormula)) {
      return false;
    }

    final IntegerFormula z = (IntegerFormula) f;

    return solver.isUnsat(bfm.not(ifm.equal(z, pIsEqualTo)));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment)
      throws SolverException, InterruptedException {

    final Formula a = Preconditions.checkNotNull(pAssignment.get("a"));
    final Formula b = Preconditions.checkNotNull(pAssignment.get("b"));

    if (!(pAssignment.get("e") instanceof IntegerFormula)) {
      return false;
    }

    final IntegerFormula e = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("e"));

    if (a.equals(b)) {
      return false;
    }

    if (!checkIfAvailable(pAssignment, "zero", zero)) {
      return false;
    }

    if (!checkIfAvailable(pAssignment, "one", one)) {
      return false;
    }

    if (!checkIfAvailable(pAssignment, "eMinusOne", ifm.subtract(e, one))) {
      return false;
    }

    return true;
  }


  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    final IntegerFormula a = (IntegerFormula) pAssignment.get("a");
    final IntegerFormula b = (IntegerFormula) pAssignment.get("b");

    if (pAssignment.containsKey("geq1")
     && pAssignment.containsKey("geq2")) {

      return Lists.newArrayList(
          ifm.lessOrEquals(a, b));
    }

    return Lists.newArrayList(
        ifm.lessThan(a, b));
  }


}
