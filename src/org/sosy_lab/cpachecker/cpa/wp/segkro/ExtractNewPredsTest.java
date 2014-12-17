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
package org.sosy_lab.cpachecker.cpa.wp.segkro;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.wp.segkro.interfaces.Rule;
import org.sosy_lab.cpachecker.cpa.wp.segkro.rules.RulesetFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;

@SuppressWarnings("unused")
public class ExtractNewPredsTest extends SolverBasedTest0 {

  private ExtractNewPreds enp;
  private FormulaManagerView mgrv;

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(factory, config, logger);
    Solver solver = new Solver(mgrv, factory, config, logger);
    List<Rule> rules = Lists.newArrayList();
    rules = RulesetFactory.createRuleset(mgr, solver);
    enp = new ExtractNewPreds(rules);
  }

  @Test @Ignore
  public void test() {
    ArrayFormulaManagerView afm = mgrv.getArrayFormulaManager();
    BooleanFormulaManagerView bfm = mgrv.getBooleanFormulaManager();
    NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifm = mgrv.getIntegerFormulaManager();

    IntegerFormula _i = mgrv.makeVariable(NumeralType.IntegerType, "i");
    IntegerFormula _al = mgrv.makeVariable(NumeralType.IntegerType, "al");
    IntegerFormula _0 = ifm.makeNumber(0);
    IntegerFormula _1 = ifm.makeNumber(1);
    IntegerFormula _i_plus_1 = ifm.add(_i, _1);

    ArrayFormula<IntegerFormula, IntegerFormula> _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    IntegerFormula _b_at_i_plus_1 = afm.select(_b, _i_plus_1);
    BooleanFormula _b_at_i_plus_1_EQUAL_0 = ifm.equal(_b_at_i_plus_1, _0);
    BooleanFormula _b_at_i_plus_1_NOTEQUAL_0 = bfm.not(ifm.equal(_b_at_i_plus_1, _0));

    BooleanFormula _i_LESS_al = ifm.lessThan(_i, _al);
    BooleanFormula _i_plus_1_GEQ_al = ifm.greaterOrEquals(_i_plus_1, _al);



  }

}
