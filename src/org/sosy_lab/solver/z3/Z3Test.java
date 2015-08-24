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
package org.sosy_lab.solver.z3;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.solver.FormulaManagerFactory.Solvers;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.SolverBasedTest0;

/**
 * Testing the custom SSA implementation.
 */
public class Z3Test extends SolverBasedTest0 {

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Test(expected=Exception.class)
  public void testErrorHandling() throws Exception {
    // Will exit(1) without an exception handler.
    //noinspection ConstantConditions,ResultOfMethodCallIgnored
    rmgr.makeNumber("not-a-number");
  }

  @Test
  public void testCongruence() throws Exception {
    IntegerFormula x;
    x = imgr.makeVariable("x");

    try (ProverEnvironment env = mgr.newProverEnvironment(false, false)) {
      //noinspection ResultOfMethodCallIgnored
      env.push(imgr.modularCongruence(x, imgr.makeNumber(0), 2));
      //noinspection ResultOfMethodCallIgnored
      env.push(imgr.equal(x, imgr.makeNumber(1)));
      assertThat(env.isUnsat()).isEqualTo(true);
    }
  }
}