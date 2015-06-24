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

import static com.google.common.truth.Truth.*;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class SolverTheoriesTest extends SolverBasedTest0 {

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

  @Test
  public void intTest1() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula num = imgr.makeNumber(2);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);
    assert_().about(BooleanFormula()).that(f).isSatisfiable();
  }

  @Test
  public void intTest2() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_b");
    IntegerFormula num = imgr.makeNumber(1);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);
    assert_().about(BooleanFormula()).that(f).isUnsatisfiable();
  }

  @Test
  public void realTest() throws Exception {
    requireRationals();

    RationalFormula a = rmgr.makeVariable("int_c");
    RationalFormula num = rmgr.makeNumber(1);

    BooleanFormula f = rmgr.equal(rmgr.add(a, a), num);
    assert_().about(BooleanFormula()).that(f).isSatisfiable();
  }

  @Test
  public void test_BitvectorIsZeroAfterShiftLeft() throws Exception {
    requireBitvectors();

    BitvectorFormula one = bvmgr.makeBitvector(32, 1);

    // unsigned char
    BitvectorFormula a = bvmgr.makeVariable(8, "char_a");
    BitvectorFormula b = bvmgr.makeVariable(8, "char_b");
    BitvectorFormula rightOp = bvmgr.makeBitvector(32, 7);

    // 'cast' a to unsigned int
    a = bvmgr.extend(a, 32 - 8, false);
    b = bvmgr.extend(b, 32 - 8, false);
    a = bvmgr.or(a, one);
    b = bvmgr.or(b, one);
    a = bvmgr.extract(a, 7, 0, true);
    b = bvmgr.extract(b, 7, 0, true);
    a = bvmgr.extend(a, 32 - 8, false);
    b = bvmgr.extend(b, 32 - 8, false);

    a = bvmgr.shiftLeft(a, rightOp);
    b = bvmgr.shiftLeft(b, rightOp);
    a = bvmgr.extract(a, 7, 0, true);
    b = bvmgr.extract(b, 7, 0, true);
    BooleanFormula f = bmgr.not(bvmgr.equal(a, b));

    assert_().about(BooleanFormula()).that(f).isUnsatisfiable();
  }

  @Test
  public void testUfWithBoolType() throws SolverException, InterruptedException {
    UninterpretedFunctionDeclaration<BooleanFormula> uf = fmgr.declareUninterpretedFunction("fun_ib", FormulaType.BooleanType, FormulaType.IntegerType);
    BooleanFormula uf0 = fmgr.callUninterpretedFunction(uf, ImmutableList.of(imgr.makeNumber(0)));
    BooleanFormula uf1 = fmgr.callUninterpretedFunction(uf, ImmutableList.of(imgr.makeNumber(1)));
    BooleanFormula uf2 = fmgr.callUninterpretedFunction(uf, ImmutableList.of(imgr.makeNumber(2)));

    BooleanFormula f01 = bmgr.xor(uf0, uf1);
    BooleanFormula f02 = bmgr.xor(uf0, uf2);
    BooleanFormula f12 = bmgr.xor(uf1, uf2);
    assert_().about(BooleanFormula()).that(f01).isSatisfiable();
    assert_().about(BooleanFormula()).that(f02).isSatisfiable();
    assert_().about(BooleanFormula()).that(f12).isSatisfiable();

    BooleanFormula f = bmgr.and(ImmutableList.of(f01, f02, f12));
    assert_().about(BooleanFormula()).that(f).isUnsatisfiable();
  }

  @Test @Ignore
  public void testUfWithBoolArg() throws SolverException, InterruptedException {
    // Not all SMT solvers support UFs with boolean arguments.
    // We can simulate this with "uf(ite(p,0,1))", but currently we do not need this.
    // Thus this test is disabled and the following is enabled.

    UninterpretedFunctionDeclaration<IntegerFormula> uf = fmgr.declareUninterpretedFunction("fun_bi", FormulaType.IntegerType, FormulaType.BooleanType);
    IntegerFormula ufTrue = fmgr.callUninterpretedFunction(uf, ImmutableList.of(bmgr.makeBoolean(true)));
    IntegerFormula ufFalse = fmgr.callUninterpretedFunction(uf, ImmutableList.of(bmgr.makeBoolean(false)));

    BooleanFormula f = bmgr.not(imgr.equal(ufTrue, ufFalse));
    assertThat(f.toString()).isEmpty();
    assert_().about(BooleanFormula()).that(f).isSatisfiable();
  }

  @Test(expected=IllegalArgumentException.class)
  public void testUfWithBoolArg_unsupported() throws SolverException, InterruptedException {
    fmgr.declareUninterpretedFunction("fun_bi", FormulaType.IntegerType, FormulaType.BooleanType);
  }


  @Test
  public void quantifierEliminationTest1() throws Exception {
    requireQuantifiers();

    IntegerFormula var_B = imgr.makeVariable("b");
    IntegerFormula var_C = imgr.makeVariable("c");
    IntegerFormula num_2 = imgr.makeNumber(2);
    IntegerFormula num_1000 = imgr.makeNumber(1000);
    BooleanFormula eq_c_2 = imgr.equal(var_C, num_2);
    IntegerFormula minus_b_c = imgr.subtract(var_B, var_C);
    BooleanFormula gt_bMinusC_1000 = imgr.greaterThan(minus_b_c, num_1000);
    BooleanFormula and_cEq2_bMinusCgt1000 = bmgr.and(eq_c_2, gt_bMinusC_1000);

    BooleanFormula f = qmgr.exists(Lists.<Formula>newArrayList(var_C), and_cEq2_bMinusCgt1000);
    BooleanFormula result = qmgr.eliminateQuantifiers(f);
    assertThat(result.toString()).doesNotContain("exists");
    assertThat(result.toString()).doesNotContain("c");

    BooleanFormula expected = imgr.greaterOrEquals(var_B, imgr.makeNumber(1003));
    assert_().about(BooleanFormula()).that(result).isEquivalentTo(expected);
  }

  @Test @Ignore
  public void quantifierEliminationTest2() throws Exception {
    requireQuantifiers();

    IntegerFormula i1 = imgr.makeVariable("i@1");
    IntegerFormula j1 = imgr.makeVariable("j@1");
    IntegerFormula j2 = imgr.makeVariable("j@2");
    IntegerFormula a1 = imgr.makeVariable("a@1");

    IntegerFormula _1 = imgr.makeNumber(1);
    IntegerFormula _minus1 = imgr.makeNumber(-1);

    IntegerFormula _1_plus_a1 = imgr.add(_1, a1);
    BooleanFormula not_j1_eq_minus1 = bmgr.not(imgr.equal(j1, _minus1));
    BooleanFormula i1_eq_1_plus_a1 = imgr.equal(i1, _1_plus_a1);

    IntegerFormula j2_plus_a1 = imgr.add(j2, a1);
    BooleanFormula j1_eq_j2_plus_a1 = imgr.equal(j1, j2_plus_a1);

    BooleanFormula fm = bmgr.and(Lists.newArrayList(
            i1_eq_1_plus_a1,
            not_j1_eq_minus1,
            j1_eq_j2_plus_a1));

    BooleanFormula q = qmgr.exists(Lists.<Formula>newArrayList(j1), fm);
    BooleanFormula result = qmgr.eliminateQuantifiers(q);
    assertThat(result.toString()).doesNotContain("exists");
    assertThat(result.toString()).doesNotContain("j@1");

    BooleanFormula expected = bmgr.not(imgr.equal(i1, j2));
    assert_().about(BooleanFormula()).that(result).isEquivalentTo(expected);
  }

  @Test
  public void testGetFormulaType() {
    BooleanFormula _boolVar = bmgr.makeVariable("boolVar");
    assertThat(mgr.getFormulaType(_boolVar)).isEqualTo(FormulaType.BooleanType);

    IntegerFormula _intVar = imgr.makeNumber(1);
    assertThat(mgr.getFormulaType(_intVar)).isEqualTo(FormulaType.IntegerType);

    requireArrays();
    ArrayFormula<IntegerFormula, IntegerFormula> _arrayVar = amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    assertThat(mgr.getFormulaType(_arrayVar))
      .isInstanceOf(FormulaType.ArrayFormulaType.class);
  }

  @Test
  public void testMakeIntArray() {
    requireArrays();

    IntegerFormula _i = imgr.makeVariable("i");
    IntegerFormula _1 = imgr.makeNumber(1);
    IntegerFormula _i_plus_1 = imgr.add(_i, _1);

    ArrayFormula<IntegerFormula, IntegerFormula> _b = amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    IntegerFormula _b_at_i_plus_1 = amgr.select(_b, _i_plus_1);

    assertThat(_b_at_i_plus_1.toString()).isEqualTo("(select b (+ i 1))"); // Compatibility to all solvers not guaranteed
  }

  @Test
  public void testMakeBitVectorArray() {
    requireArrays();

    BitvectorFormula _i = mgr.getBitvectorFormulaManager().makeVariable(64, "i");
    ArrayFormula<BitvectorFormula, BitvectorFormula> _b = amgr.makeArray("b", FormulaType.getBitvectorTypeWithSize(64), FormulaType.getBitvectorTypeWithSize(32));
    BitvectorFormula _b_at_i = amgr.select(_b, _i);

    assertThat(_b_at_i.toString()).isEqualTo("(select b i)"); // Compatibility to all solvers not guaranteed
  }
}
