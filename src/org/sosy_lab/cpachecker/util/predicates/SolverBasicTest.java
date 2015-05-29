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

import static com.google.common.truth.TruthJUnit.assume;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;

@RunWith(Parameterized.class)
public class SolverBasicTest extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Solvers[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Test
  public void formulaEqualsAndHashCode() {
    UninterpretedFunctionDeclaration<IntegerFormula> f_b =
        fmgr.declareUninterpretedFunction("f_b", FormulaType.IntegerType, FormulaType.IntegerType);

    new EqualsTester()
      .addEqualityGroup(bmgr.makeBoolean(true))
      .addEqualityGroup(bmgr.makeBoolean(false))
      .addEqualityGroup(bmgr.makeVariable("bool_a"))
      .addEqualityGroup(imgr.makeVariable("int_a"))

      // Way of creating numbers should not make a difference.
      .addEqualityGroup(imgr.makeNumber(0.0), imgr.makeNumber(0L), imgr.makeNumber(BigInteger.ZERO), imgr.makeNumber(BigDecimal.ZERO), imgr.makeNumber("0"))
      .addEqualityGroup(imgr.makeNumber(1.0), imgr.makeNumber(1L), imgr.makeNumber(BigInteger.ONE), imgr.makeNumber(BigDecimal.ONE), imgr.makeNumber("1"))

      // The same formula when created twice should compare equal.
      .addEqualityGroup(bmgr.makeVariable("bool_b"),
                        bmgr.makeVariable("bool_b"))
      .addEqualityGroup(bmgr.and(bmgr.makeVariable("bool_a"), bmgr.makeVariable("bool_b")),
                        bmgr.and(bmgr.makeVariable("bool_a"), bmgr.makeVariable("bool_b")))
      .addEqualityGroup(imgr.equal(imgr.makeNumber(0), imgr.makeVariable("int_a")),
                        imgr.equal(imgr.makeNumber(0), imgr.makeVariable("int_a")))

      // Not all solvers normalize formulas, thus the following does not hold
      // (and it is not strictly required to hold):
      //.addEqualityGroup(bmgr.and(bmgr.makeVariable("bool_c"), bmgr.makeVariable("bool_d")),
      //                  bmgr.and(bmgr.makeVariable("bool_d"), bmgr.makeVariable("bool_c")))
      //.addEqualityGroup(imgr.equal(imgr.makeNumber(0), imgr.makeVariable("int_b")),
      //                  imgr.equal(imgr.makeVariable("int_b"), imgr.makeNumber(0)))

      // UninterpretedFunctionDeclarations should not compare equal to Formulas,
      // but declaring one twice needs to return the same UIF.
      .addEqualityGroup(fmgr.declareUninterpretedFunction("f_a", FormulaType.IntegerType, FormulaType.IntegerType),
                        fmgr.declareUninterpretedFunction("f_a", FormulaType.IntegerType, FormulaType.IntegerType))

      .addEqualityGroup(f_b)
      .addEqualityGroup(fmgr.callUninterpretedFunction(f_b, ImmutableList.of(imgr.makeNumber(0))))
      .addEqualityGroup(fmgr.callUninterpretedFunction(f_b, ImmutableList.of(imgr.makeNumber(1))),
                        fmgr.callUninterpretedFunction(f_b, ImmutableList.of(imgr.makeNumber(1))))

      .testEquals();
  }
}
