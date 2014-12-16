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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;

import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EquivalenceRule;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;

public class EquivalenceRuleTest0 extends AbstractRuleTest0 {

  private EquivalenceRule er;
  private IntegerFormula _x;
  private IntegerFormula _e;
  private IntegerFormula _0;
  private IntegerFormula _i;
  private IntegerFormula _al;
  private IntegerFormula _1;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    er = new EquivalenceRule(solver, matcher);

    _i = mgrv.makeVariable(NumeralType.IntegerType, "i");
    _al = mgrv.makeVariable(NumeralType.IntegerType, "al");
    _x = mgrv.makeVariable(NumeralType.IntegerType, "x");
    _e = mgrv.makeVariable(NumeralType.IntegerType, "e");
    _0 = imgr.makeNumber(0);
    _1 = imgr.makeNumber(1);
  }

  @Test
  public void testEquivalence1() throws SolverException, InterruptedException {

    // Formulas for the premise
    BooleanFormula _x_minus_e_GEQ_0 = imgr.greaterOrEquals(imgr.subtract(_x, _e), _0);
    BooleanFormula _minus_x_plus_e_GEQ_0 = imgr.greaterOrEquals(imgr.add(imgr.subtract(_0, _x), _e), _0);

    // The expected conclusion
    BooleanFormula expectedConclusion = imgr.equal(_x, _e);

    Set<BooleanFormula> conclusion = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _x_minus_e_GEQ_0,
            _minus_x_plus_e_GEQ_0
        ));

    assertThat(conclusion).isNotEmpty();
    assertThat(conclusion.iterator().next().toString()).isEqualTo(expectedConclusion.toString());
  }

  @Test
  public void testEquivalence2() throws SolverException, InterruptedException {

    Set<BooleanFormula> result = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            imgr.lessThan(_i, _al),
            imgr.greaterOrEquals(imgr.add(_i, _1), _al)
        ));

    BooleanFormula expectedConclusion = imgr.equal(_al, imgr.add(_i, _1));

    assertThat(result).isNotEmpty();
    assertThat(result.iterator().next().toString()).isEqualTo(expectedConclusion.toString());
  }

}

