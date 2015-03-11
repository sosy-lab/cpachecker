/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolatingProverWithAssumptionsWrapper;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class SolverFormulaWithAssignmentsTest extends SolverBasedTest0 {

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

  /** Generate a prover environment depending on the parameter above.
   * @param <T>
   * @throws InvalidConfigurationException */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <T> InterpolatingProverWithAssumptionsWrapper<T> newEnvironmentForTest() throws InvalidConfigurationException {
    FormulaManagerView formulaView = new FormulaManagerView(factory, config, TestLogManager.getInstance());
    return new InterpolatingProverWithAssumptionsWrapper(mgr.newProverEnvironmentWithInterpolation(false), formulaView);
  }

  @Test
  public <T> void basicAssumptionsTest() throws SolverException, InterruptedException, InvalidConfigurationException {
    IntegerFormula v1 = imgr.makeVariable("v1");
    IntegerFormula v2 = imgr.makeVariable("v2");

    BooleanFormula suffix1 = bmgr.makeVariable("suffix1");
    BooleanFormula suffix2 = bmgr.makeVariable("suffix2");
    BooleanFormula suffix3 = bmgr.makeVariable("suffix3");

    BooleanFormula term1 = bmgr.or(bmgr.and(imgr.equal(v1, imgr.makeNumber(BigDecimal.ONE)),
                                            bmgr.not(imgr.equal(v1, v2))),
                                   suffix1);
    BooleanFormula term2 = bmgr.or(imgr.equal(v2, imgr.makeNumber(BigDecimal.ONE)),
                                   suffix2);
    BooleanFormula term3 = bmgr.or(bmgr.not(imgr.equal(v1, imgr.makeNumber(BigDecimal.ONE))),
                                   suffix3);

    InterpolatingProverWithAssumptionsWrapper<T> env = newEnvironmentForTest();


    T firstPartForInterpolant = env.push(term1);
    env.push(term2);
    env.push(term3);

    assertThat(env.isUnsatWithAssumptions(Lists.newArrayList(bmgr.not(suffix1),
                                                             bmgr.not(suffix2),
                                                             suffix3)))
               .isTrue();
    assertThat(env.getInterpolant(Collections.singletonList(firstPartForInterpolant)).toString())
              .doesNotContain("suffix");

    env.clearAssumptions();

    assertThat(env.isUnsatWithAssumptions(Lists.newArrayList(bmgr.not(suffix1),
                                                             bmgr.not(suffix3),
                                                             suffix2)))
               .isTrue();
    assertThat(env.getInterpolant(Collections.singletonList(firstPartForInterpolant)).toString())
              .doesNotContain("suffix");
    env.clearAssumptions();
  }
}
