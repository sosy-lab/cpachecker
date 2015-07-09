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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment.AllSatCallback;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class SolverAllSatTest extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  private ProverEnvironment env;

  @Before
  public void setupEnvironment() {
    env = mgr.newProverEnvironment(false, false);
  }

  @After
  public void closeEnvironment() {
    env.close();
  }

  private static final String EXPECTED_RESULT = "AllSatTest_unsat";

  private static class TestAllSatCallback implements AllSatCallback<String> {

    private final List<List<BooleanFormula>> models = new ArrayList<>();

    @Override
    public void apply(List<BooleanFormula> pModel) {
      models.add(ImmutableList.copyOf(pModel));
    }

    @Override
    public String getResult() throws InterruptedException {
      return EXPECTED_RESULT;
    }
  }

  @Test
  public void allSatTest_unsat() throws SolverException, InterruptedException {
    IntegerFormula a = imgr.makeVariable("i");
    IntegerFormula n1 = imgr.makeNumber(1);
    IntegerFormula n2 = imgr.makeNumber(2);

    BooleanFormula cond1 = imgr.equal(a, n1);
    BooleanFormula cond2 = imgr.equal(a, n2);

    BooleanFormula v1 = bmgr.makeVariable("b1");
    BooleanFormula v2 = bmgr.makeVariable("b2");

    env.push(cond1);
    env.push(cond2);

    env.push(bmgr.equivalence(v1, cond1));
    env.push(bmgr.equivalence(v2, cond2));


    TestAllSatCallback callback = new TestAllSatCallback() {
          @Override
          public void apply(List<BooleanFormula> pModel) {
            fail("Formula is unsat, but all-sat callback called with model " + pModel);
          }
        };

    assertThat(env.allSat(callback, ImmutableList.of(v1, v2)))
              .isEqualTo(EXPECTED_RESULT);
  }

  @Test
  public void allSatTest_xor() throws SolverException, InterruptedException {
    IntegerFormula a = imgr.makeVariable("i");
    IntegerFormula n1 = imgr.makeNumber(1);
    IntegerFormula n2 = imgr.makeNumber(2);

    BooleanFormula cond1 = imgr.equal(a, n1);
    BooleanFormula cond2 = imgr.equal(a, n2);

    BooleanFormula v1 = bmgr.makeVariable("b1");
    BooleanFormula v2 = bmgr.makeVariable("b2");

    env.push(bmgr.xor(cond1, cond2));

    env.push(bmgr.equivalence(v1, cond1));
    env.push(bmgr.equivalence(v2, cond2));

    TestAllSatCallback callback = new TestAllSatCallback();

    assertThat(env.allSat(callback, ImmutableList.of(v1, v2)))
              .isEqualTo(EXPECTED_RESULT);

    assertThat(callback.models).containsExactly(
        ImmutableList.of(v1, bmgr.not(v2)),
        ImmutableList.of(bmgr.not(v1), v2));
  }
}
