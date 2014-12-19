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
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

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

    premises.add(new PatternBasedPremise(
          GenericPatterns.f_of_x_expression("f", "x")
        ));

    premises.add(new PatternBasedPremise(or(
        match("=",
            matchAnyWithAnyArgsBind("x"),
            matchAnyWithAnyArgsBind("e")),
        match("=",
            matchAnyWithAnyArgsBind("e"),
            matchAnyWithAnyArgsBind("x"))
    )));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment)
      throws SolverException, InterruptedException {
    return true;
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    final BooleanFormula f = (BooleanFormula) pAssignment.get("f");
    final Formula x = pAssignment.get("x");
    final Formula e = pAssignment.get("e");

    Map<Formula, Formula> transformation = Maps.newHashMap();
    transformation.put(x, e);

    final BooleanFormula fPrime = matcher.substitute(f, transformation);

    return Lists.newArrayList(fPrime);
  }

}
