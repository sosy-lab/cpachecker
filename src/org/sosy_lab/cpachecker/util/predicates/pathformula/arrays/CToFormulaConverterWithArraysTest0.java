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
package org.sosy_lab.cpachecker.util.predicates.pathformula.arrays;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.*;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.base.Optional;

/**
 *
 * CArraySubscriptExpression (is a left-hand-side):
 *  The array subscript operator is used to access a element of an array.
 *
 * Examples:
 *    array[0]
 *    array[i]
 *    array[n-1]
 *
 *  ATTENTION: This can be equally expressed with pointer arithmetic!
 *
 *    int a*
 *      is a valid declaration of an array!
 *
 *  Consider multi-dimensional arrays!
 *
 *  Consider CArrayDesignator!
 *
 */

public class CToFormulaConverterWithArraysTest0 extends SolverBasedTest0 {

  private static final CArrayType unlimitedIntArrayType = new CArrayType(false, false, CNumericTypes.INT, null);

  private CToFormulaConverterWithArrays ctfBwd;

  private CBinaryExpressionBuilder expressionBuilder;
  private FormulaManagerView mgrv;

  private Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _a;
  private Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _b;
  private Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _bl;
  private Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _i;

  private Pair<CFAEdge, CExpressionAssignmentStatement> _i_assign_i_plus_1;
  private Pair<CAssumeEdge, CExpression> _b_at_i_notequal_0;
  private Pair<CAssumeEdge, CExpression> _a_at_i_equal_b_at_i;
  private Pair<CFAEdge, CExpressionAssignmentStatement> _a_at_i_assign_b_at_i;
  private Pair<CAssumeEdge, CExpression> _a_at_2_notequal_0;
  private Pair<CAssumeEdge, CExpression> _i_notequal_0;

  private CArraySubscriptExpression _b_at_i;
  private CArraySubscriptExpression _a_at_i;
  private CArraySubscriptExpression _a_at_1;
  private CArraySubscriptExpression _a_at_2;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    MachineModel mm = MachineModel.LINUX64;
    mgrv = new FormulaManagerView(mgr, config, logger);
    FormulaEncodingOptions opts = new FormulaEncodingOptions(Configuration.defaultConfiguration());
    CtoFormulaTypeHandlerWithArrays th = new CtoFormulaTypeHandlerWithArrays(logger, opts, mm, mgrv);
    expressionBuilder = new CBinaryExpressionBuilder(mm, logger);

    ctfBwd = new CToFormulaConverterWithArrays(
        opts,
        mgrv,
        mm,
        Optional.<VariableClassification>absent(),
        logger,
        ShutdownNotifier.create(),
        th,
        AnalysisDirection.BACKWARD);
  }

  @Before
  public void setupCfaTestData() throws UnrecognizedCCodeException {

    _a = makeDeclaration("a", unlimitedIntArrayType, null);
    _b = makeDeclaration("b", unlimitedIntArrayType, null);
    _bl = makeDeclaration("bl", CNumericTypes.INT, null);
    _i = makeDeclaration("i", CNumericTypes.INT, INT_ZERO_INITIALIZER);

    _b_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _b.getThird(),
        _i.getThird());

    _a_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        _i.getThird());

    _a_at_1 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        CIntegerLiteralExpression.ONE);

    _a_at_2 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        CIntegerLiteralExpression.createDummyLiteral(2, CNumericTypes.INT));

    _i_assign_i_plus_1 = makeAssignment(
        _i.getThird(),
        expressionBuilder.buildBinaryExpression(
            _i.getThird(),
            CIntegerLiteralExpression.ONE,
            BinaryOperator.PLUS));

    _b_at_i_notequal_0 = makeAssume(expressionBuilder.buildBinaryExpression(
        _b_at_i,
        CIntegerLiteralExpression.ZERO,
        BinaryOperator.NOT_EQUALS));

    _a_at_2_notequal_0 = makeAssume(expressionBuilder.buildBinaryExpression(
        _a_at_2,
        CIntegerLiteralExpression.ZERO,
        BinaryOperator.NOT_EQUALS));

    _i_notequal_0 = makeAssume(expressionBuilder.buildBinaryExpression(
        _i.getThird(),
        CIntegerLiteralExpression.ZERO,
        BinaryOperator.NOT_EQUALS));

    _a_at_i_equal_b_at_i = makeAssume(expressionBuilder.buildBinaryExpression(
        _a_at_i,
        _b_at_i,
        BinaryOperator.EQUALS));

    _a_at_i_assign_b_at_i = makeAssignment(_a_at_i, _b_at_i);
  }


  @Test
  public void testMakePredicate1() throws UnrecognizedCCodeException, InterruptedException {
    // a[2] != 0

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makePredicate(
        _a_at_2_notequal_0.getSecond(),
        _a_at_2_notequal_0.getFirst(),
        "foo", ssa);

    assertThat(result.toString())
      .comparesEqualTo("(and (not (= (select a@1 2.0) 0.0)) true)");
  }

  @Test
  public void testMakePredicate2() throws UnrecognizedCCodeException, InterruptedException {
    // a[a[2]] != 0

    CArraySubscriptExpression _a_at__a_at_2 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        _a_at_2);
    Pair<CAssumeEdge, CExpression> _a_at__a_at_2_notequal_0 = makeAssume(expressionBuilder.buildBinaryExpression(
        _a_at__a_at_2,
        CIntegerLiteralExpression.ZERO,
        BinaryOperator.NOT_EQUALS));

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makePredicate(
        _a_at__a_at_2_notequal_0.getSecond(),
        _a_at__a_at_2_notequal_0.getFirst(),
        "foo", ssa);

    assertThat(mgr.getUnsafeFormulaManager().simplify(result).toString())
      .comparesEqualTo("(not (= (select a@1 (select a@1 2.0)) 0.0))");
  }

  @Test
  public void testArrayDesignator1() throws UnrecognizedCCodeException, InterruptedException {
    // int x[] = { 0, 1, 2 } ;
  }

  @Test
  public void testArrayDeclaration1() throws UnrecognizedCCodeException, InterruptedException {
    // int array[100];
  }

  @Test
  public void testMultiDimensional1() throws UnrecognizedCCodeException, InterruptedException {
    // int array2d[3][7];
  }

  @Test
  public void testMultiDimensional2() throws UnrecognizedCCodeException, InterruptedException {
    // array2d[3][7] = 23;
  }

  @Test
  public void testArrayAsPointer1() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[0]
    // and
    //    *array
  }

  @Test
  public void testArrayAsPointer2() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[2]
    // and
    //    *(array + 2)
  }

  @Test
  public void testArrayAsPointer3() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[n - 1]
    // and
    //    *(array + n - 1)
  }

  @Test
  public void testArrayAsPointer4() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[i - 1][j - 1]
    // and
    //    *(*(array + i - 1) + j - 1)
  }

  @Test
  public void testArrayMalloc() throws UnrecognizedCCodeException, InterruptedException {
    // int* a = malloc(100 * sizeof(int));
  }





}
