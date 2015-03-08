/*
 * CPAchecker is a tool for configurable software verification.
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

import static com.google.common.truth.Truth.assert_;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

/**
 * Tests for arithmetic operations in {@link BooleanFormula}s
 */
@RunWith(Parameterized.class)
public class SolverArithmeticOperationsTest extends SolverBasedTest0 {

  @Parameterized.Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return FormulaManagerFactory.Solvers.values();
  }

  @Parameterized.Parameter(0)
  public FormulaManagerFactory.Solvers solver;

  @Override
  protected FormulaManagerFactory.Solvers solverToUse() {
    return solver;
  }

  @Test
  public void testUnsat_isZeroAfterShiftLeft() throws Exception {
    BitvectorFormulaManager bvfmgr = mgr.getBitvectorFormulaManager();
    BitvectorFormula one = bvfmgr.makeBitvector(32, 1);

    // unsigned char
    BitvectorFormula a = bvfmgr.makeVariable(8, "char_a");
    BitvectorFormula b = bvfmgr.makeVariable(8, "char_b");
    BitvectorFormula rightOp = bvfmgr.makeBitvector(32, 7);

    // 'cast' a to unsigned int
    a = bvfmgr.extend(a, 32 - 8, false);
    b = bvfmgr.extend(b, 32 - 8, false);
    a = bvfmgr.or(a, one);
    b = bvfmgr.or(b, one);
    a = bvfmgr.extract(a, 7, 0);
    b = bvfmgr.extract(b, 7, 0);
    a = bvfmgr.extend(a, 32 - 8, false);
    b = bvfmgr.extend(b, 32 - 8, false);

    a = bvfmgr.shiftLeft(a, rightOp);
    b = bvfmgr.shiftLeft(b, rightOp);
    a = bvfmgr.extract(a, 7, 0);
    b = bvfmgr.extract(b, 7, 0);
    BooleanFormula f = bmgr.not(bvfmgr.equal(a, b));

    assert_().about(BooleanFormula()).that(f).isUnsatisfiable();
  }

}
