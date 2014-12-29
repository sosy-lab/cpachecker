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

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class UniversalizeRule extends PatternBasedRule {

  public UniversalizeRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    // (not (bar (+ a c)))
    // (not (= (select b (+ i 1)) 0))
    // (bar (+ a c))
    // (= (select b (+ i 1)) 0)
    // (= 0 (select b (+ i 1)))

    // TODO: Nested arrays...
    // TODO: Handling of deeper nestings (implementation of matchInSubtree)

    premises.add(new PatternBasedPremise(
        GenericPatterns.array_at_index_subtree_matcher("a", "i")
          ));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment) throws SolverException, InterruptedException {
    return true;
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {
    final BooleanFormula f = (BooleanFormula) Preconditions.checkNotNull(pAssignment.get("a"));
    final IntegerFormula i = (IntegerFormula) Preconditions.checkNotNull(pAssignment.get("i"));

    assert f != null;
    assert i != null;

    final IntegerFormula x = ifm.makeVariable("x");
    final BooleanFormula xConstraint =  ifm.equal(x, i);

    Map<Formula, Formula> transformation = Maps.newHashMap();
    transformation.put(i, x);

    final BooleanFormula fPrime = matcher.substitute(f, transformation);

    return Lists.newArrayList(
        qfm.forall(Lists.newArrayList(x), bfm.and(fPrime, xConstraint)));
  }
}
