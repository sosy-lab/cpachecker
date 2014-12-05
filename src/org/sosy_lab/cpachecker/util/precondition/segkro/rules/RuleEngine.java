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

import java.util.List;

import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Concluding;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Rule;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class RuleEngine implements Concluding {

  private List<Rule> rules;

  public RuleEngine(FormulaManager pFm, Solver pSolver) {
    rules = Lists.newArrayList();

    rules.add(new EliminationRule(pFm, pSolver));
    rules.add(new EquivalenceRule(pFm, pSolver));
    rules.add(new UniverifyRule(pFm, pSolver));
    rules.add(new SubstitutionRule(pFm, pSolver));
    rules.add(new LinkRule(pFm, pSolver));
    rules.add(new ExistentialRule(pFm, pSolver));
    rules.add(new ExtendLeftRule(pFm, pSolver));
    rules.add(new ExtendRightRule(pFm, pSolver));
  }

  public ImmutableList<Rule> getRules() {
    return ImmutableList.copyOf(rules);
  }

  @Override
  public BooleanFormula concludeFromAtoms(List<BooleanFormula> pAtomPredicates) {
    return null;
  }

  /**
   *
   * @param pAtomPredicates
   * @param pRule
   * @return    List of atomic predicates
   */
  public List<BooleanFormula> concludeWithSingleRule(List<BooleanFormula> pAtomPredicates, Rule pRule) {
    return null;
  }

}
