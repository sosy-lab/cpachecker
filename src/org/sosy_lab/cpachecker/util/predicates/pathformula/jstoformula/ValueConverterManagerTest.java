/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
public class ValueConverterManagerTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return new Object[] {Solvers.MATHSAT5, Solvers.Z3};
  }

  @Parameter() public Solvers usedSolver;

  @Override
  protected Solvers solverToUse() {
    return usedSolver;
  }

  private ValueConverterManager valConvMgr;

  @Before
  public void init() throws InvalidConfigurationException {
    initSolver();
    initCPAcheckerSolver();
    final ObjectIdFormulaManager objIdMgr = new ObjectIdFormulaManager(mgrv);
    final TypeTags typeTags = new TypeTags(imgrv);
    valConvMgr =
        new ValueConverterManager(
            new TypedVariableValues(mgrv.getFunctionFormulaManager()),
            typeTags,
            new TypedValueManager(mgrv, typeTags, objIdMgr.getNullObjectId()),
            new StringFormulaManager(mgrv, 15),
            mgrv);
  }

  private void assertToInt32(final double pFrom, final int pTo, final boolean pEqual)
      throws SolverException, InterruptedException {
    @SuppressWarnings("ConstantConditions")
    final BooleanFormula formula =
        bvmgr.equal(
            valConvMgr.toInt32(
                fpmgr.makeNumber(pFrom, FormulaType.getDoublePrecisionFloatingPointType())),
            bvmgr.makeBitvector(32, pTo));
    if (pEqual) {
      assertThatFormula(formula).isSatisfiable();
      assertThatFormula(bmgr.not(formula)).isUnsatisfiable();
    } else {
      assertThatFormula(formula).isUnsatisfiable();
      assertThatFormula(bmgr.not(formula)).isSatisfiable();
    }
  }

  @Test
  public void toInt32() throws SolverException, InterruptedException {
    // int32 to int32
    assertToInt32(-1, -1, true);
    assertToInt32(0, 0, true);
    assertToInt32(2147483647, 2147483647, true);
    assertToInt32(-2147483648, -2147483648, true);

    assertToInt32(-1, -2, false);
    assertToInt32(0, 1, false);
    assertToInt32(2147483647, -2147483648, false);
    assertToInt32(-2147483648, 2147483647, false);

    // double to int32
    assertToInt32(-1.9, -1, true);
    assertToInt32(0.1, 0, true);
    assertToInt32(2147483647.999, 2147483647, true);
    assertToInt32(-2147483648.999, -2147483648, true);

    assertToInt32(-1.9, -2, false);
    assertToInt32(0.1, 1, false);
    assertToInt32(2147483647.999, -2147483648, false);
    assertToInt32(-2147483648.999, 2147483647, false);

    // integer out of 32-bit range are converted with overflow
    assertToInt32(2147483648.0, -2147483648, true);
    assertToInt32(6442450944.0, -2147483648, true);
    assertToInt32(2147483649.0, -2147483647, true);
    assertToInt32(-2147483649.0, 2147483647, true);
    assertToInt32(-6442450945.0, 2147483647, true);
    assertToInt32(-2147483650.0, 2147483646, true);
    assertToInt32(Double.MAX_VALUE, 0, true);
    assertToInt32(-Double.MAX_VALUE, 0, true);

    assertToInt32(2147483648.0, 2147483647, false);
    assertToInt32(6442450944.0, 2147483647, false);
    assertToInt32(2147483649.0, 2147483647, false);
    assertToInt32(-2147483649.0, 0, false);
    assertToInt32(-6442450945.0, 0, false);
    assertToInt32(-2147483650.0, 0, false);
    assertToInt32(Double.MAX_VALUE, 2147483647, false);
    assertToInt32(-Double.MAX_VALUE, -2147483648, false);

    // special values
    assertToInt32(Double.POSITIVE_INFINITY, 0, true);
    assertToInt32(Double.NEGATIVE_INFINITY, 0, true);
    assertToInt32(Double.NaN, 0, true);
    assertToInt32(-0.0, 0, true);
  }
}
