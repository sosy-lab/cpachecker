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
import org.sosy_lab.solver.Solver;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

import com.google.common.collect.Lists;


public class ExistentialRule extends PatternBasedRule {

  public ExistentialRule(Solver pSolver, SmtAstMatcher pMatcher) {
    super(pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    // ATTENTION:
    //  This definition is different from the Seghir/Kroenig paper!

    premises.add(new PatternBasedPremise(or(
        matchBind("not", "nf",
            GenericPatterns.array_at_index_matcher("fx", "i", PropositionType.POSITIVE))
        )));

    premises.add(new PatternBasedPremise(or(
        GenericPatterns.array_at_index_matcher("f", "j", PropositionType.POSITIVE)
        )));
  }

  @Override
  protected boolean satisfiesConstraints(Map<String, Formula> pAssignment) throws SolverException, InterruptedException {
    final Formula i = pAssignment.get("i");
    final Formula j = pAssignment.get("j");

    assert i instanceof IntegerFormula;
    assert j instanceof IntegerFormula;

    BooleanFormula lt = ifm.lessThan((IntegerFormula) i, (IntegerFormula) j);

    return !solver.isUnsat(lt);
  }

  @Override
  protected Collection<BooleanFormula> deriveConclusion(Map<String, Formula> pAssignment) {

    // There might be more than one valid (symbolic) assignment; this method gets called for every possible assignment

    final BooleanFormula f = (BooleanFormula) pAssignment.get("f");
    final BooleanFormula nf = (BooleanFormula) pAssignment.get("nf");
    final IntegerFormula i = (IntegerFormula) pAssignment.get("i");
    final Formula parentOfI = pAssignment.get(parentOf("i"));
    final IntegerFormula j = (IntegerFormula) pAssignment.get("j");
    final Formula parentOfJ = pAssignment.get(parentOf("j"));

    final IntegerFormula x = ifm.makeVariable("x");

    final BooleanFormula nfPrime = (BooleanFormula) substituteInParent(parentOfI, i, x, nf);
    final BooleanFormula fPrime = (BooleanFormula) substituteInParent(parentOfJ, j, x, f);

    return Lists.newArrayList(
        qfm.exists(x, i, j, fPrime),
        qfm.exists(x, i, j, nfPrime)
      );
  }

}
