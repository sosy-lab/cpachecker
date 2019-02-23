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

import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.FloatingPointFormula;

@RunWith(Parameterized.class)
public class StringFormulaManagerTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return new Object[] {Solvers.MATHSAT5, Solvers.Z3};
  }

  @Parameter() public Solvers usedSolver;

  @Override
  protected Solvers solverToUse() {
    return usedSolver;
  }

  @Test
  public void testIdsAreUnique() {
    final int maxFieldNameCount = 10000;
    final StringFormulaManager strMgr = new StringFormulaManager(mgrv, maxFieldNameCount);
    final HashSet<FloatingPointFormula> ids = new HashSet<>();
    strMgr.getIdRange().forEach(ids::add);
    Assert.assertEquals(ids.size(), maxFieldNameCount);
  }

  @Test
  public void isNumberString() {
    Assert.assertTrue(StringFormulaManager.isNumberString("0"));
    Assert.assertTrue(StringFormulaManager.isNumberString("0.1"));
    Assert.assertTrue(StringFormulaManager.isNumberString("0.01"));
    Assert.assertTrue(StringFormulaManager.isNumberString("1"));
    Assert.assertTrue(StringFormulaManager.isNumberString("-1"));
    Assert.assertTrue(StringFormulaManager.isNumberString("-0.01"));

    Assert.assertFalse(StringFormulaManager.isNumberString("-0"));
    Assert.assertFalse(StringFormulaManager.isNumberString("1.0"));
    Assert.assertFalse(StringFormulaManager.isNumberString("-1.0"));
  }
}
