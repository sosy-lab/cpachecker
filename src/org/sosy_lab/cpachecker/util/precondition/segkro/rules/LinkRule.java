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
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.GenericPatterns.PropositionType;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtQuantificationPattern.QuantifierType;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import com.google.common.collect.ImmutableSet;


public class LinkRule extends PatternBasedRule {

  public LinkRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    premises.add(new PatternBasedPremise(
        or(
          GenericPatterns.range_predicate_matcher("forall",
              QuantifierType.FORALL,
              "f",
              "i", "j",
              GenericPatterns.array_at_index_matcher("f", quantified("var1"), PropositionType.ALL))

    )));

    premises.add(new PatternBasedPremise(
        or(
          GenericPatterns.range_predicate_matcher("forallRight",
              QuantifierType.FORALL,
              "f",
              "iPlusOneEq", "k",
              GenericPatterns.array_at_index_matcher("f", quantified("var2"), PropositionType.ALL))
    )));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment) throws SolverException, InterruptedException {
    final IntegerFormula i = (IntegerFormula) pAssignment.get("i");
    final IntegerFormula iPlusOneEq = (IntegerFormula) pAssignment.get("iPlusOneEq");

    return solver.isUnsat(bfm.not(ifm.equal(ifm.add(i, ifm.makeNumber(1)), iPlusOneEq)));
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    final BooleanFormula f1 = (BooleanFormula) pAssignment.get("f");
    final IntegerFormula j = (IntegerFormula) pAssignment.get("j");
    final IntegerFormula k = (IntegerFormula) pAssignment.get("k");

    final IntegerFormula xBound = (IntegerFormula) pAssignment.get(quantified("var1"));
    final Formula xBoundParent = pAssignment.get(parentOf(quantified("var1")));
    final IntegerFormula xNew = ifm.makeVariable("x");
    final BooleanFormula fNew = (BooleanFormula) substituteInParent(xBoundParent, xBound, xNew, f1);

    return ImmutableSet.of(qfm.forall(xNew, j, k, fNew));

//    if (pAssignment.containsKey("forall")) {
//      return Lists.newArrayList(qmgr.forall(Lists.newArrayList(xNew), bfm.and(fNew, xConstraint)));
//    } else {
//      return Lists.newArrayList(qmgr.exists(Lists.newArrayList(xNew), bfm.and(fNew, xConstraint)));
//    }

  }

}
