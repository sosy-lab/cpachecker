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

import static org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternBuilder.*;

import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstMatcher;


public class EliminationRule extends PatternBasedRule {

  public EliminationRule(FormulaManager pFm, FormulaManagerView pFmv, Solver pSolver, SmtAstMatcher pMatcher) {
    super(pFm, pFmv, pSolver, pMatcher);
  }

  @Override
  protected void setupPatterns() {
    IntegerFormula zero = fm.getIntegerFormulaManager().makeNumber(0);

    premises.add(new PatternBasedPremise(
        withDefaultBinding("c1",  zero,
          or(
              match(">=",
                  match("+",
                      match("*",
                          matchNullaryBind("c1"),
                          matchAnyBind("eX")),
                      matchAnyBind("e1")),
                  matchNullary("0")),
              match(">=",
                  matchAnyBind("e1"),
                  matchNullary("0"))))));

    premises.add(new PatternBasedPremise(
        withDefaultBinding("c2",  zero,
          or(
              match(">=",
                  match("+",
                      match("*",
                          match("-",
                              matchNullary("0"),
                              matchNullaryBind("c2")),
                          matchAnyBind("eX")),
                      matchAnyBind("e2")),
                  matchNullary("0")),
              match(">=",
                  matchAnyBind("e2"),
                  matchNullary("0"))))));
  }


}
