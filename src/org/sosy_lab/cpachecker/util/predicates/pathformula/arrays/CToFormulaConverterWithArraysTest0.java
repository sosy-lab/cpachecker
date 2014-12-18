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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

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
@SuppressWarnings("unused")
public class CToFormulaConverterWithArraysTest0 extends SolverBasedTest0 {

  private static final CArrayType unlimitedIntArrayType = new CArrayType(false, false, CNumericTypes.INT, null);

  private CToFormulaConverterWithArrays ctfBwd;
  private CToFormulaConverterWithArrays ctfFwd;

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

  private ArrayFormula<IntegerFormula, IntegerFormula> _smt_a;
  private ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, IntegerFormula>> _smt_a2d;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    MachineModel mm = MachineModel.LINUX64;
    mgrv = new FormulaManagerView(factory, config, logger);
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

    ctfFwd = new CToFormulaConverterWithArrays(
        opts,
        mgrv,
        mm,
        Optional.<VariableClassification>absent(),
        logger,
        ShutdownNotifier.create(),
        th,
        AnalysisDirection.FORWARD);
  }

  @Before
  public void setupCfaTestData() throws UnrecognizedCCodeException {

    _smt_a = amgr.makeArray("a@1", NumeralType.IntegerType, NumeralType.IntegerType);
    _smt_a2d = amgr.makeArray("a2d@1", NumeralType.IntegerType, FormulaType.getArrayType(NumeralType.IntegerType, NumeralType.IntegerType));

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
  public void testArrayView1() {
    NumeralFormulaManagerView<IntegerFormula, IntegerFormula> imgv = mgrv.getIntegerFormulaManager();
    ArrayFormulaManagerView amgv = mgrv.getArrayFormulaManager();

    IntegerFormula _i = imgv.makeVariable("i");
    IntegerFormula _1 = imgv.makeNumber(1);
    IntegerFormula _i_plus_1 = imgv.add(_i, _1);

    ArrayFormula<IntegerFormula, IntegerFormula> _b = amgv.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    IntegerFormula _b_at_i_plus_1 = amgv.select(_b, _i_plus_1);

    assertThat(_b_at_i_plus_1.toString()).isEqualTo("(select b (+ i 1))"); // Compatibility to all solvers not guaranteed
  }

  @Test
  public void testArrayView2() {
    NumeralFormulaManagerView<IntegerFormula, IntegerFormula> imgv = mgrv.getIntegerFormulaManager();
    ArrayFormulaManagerView amgv = mgrv.getArrayFormulaManager();

    IntegerFormula _i = imgv.makeVariable("i");

    ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, RationalFormula>> multi
      = amgv.makeArray("multi",
        NumeralType.IntegerType,
        FormulaType.getArrayType(
            NumeralType.IntegerType, NumeralType.RationalType));

    RationalFormula valueInMulti = amgv.select(amgv.select(multi, _i), _i);

    assertThat(valueInMulti.toString()).isEqualTo("(select (select multi i) i)"); // Compatibility to all solvers not guaranteed
  }

  @Test
  public void testArrayView3() {
    NumeralFormulaManagerView<IntegerFormula, IntegerFormula> imgv = mgrv.getIntegerFormulaManager();
    ArrayFormulaManagerView amgv = mgrv.getArrayFormulaManager();

    IntegerFormula _i = imgv.makeVariable("i");

    ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, BitvectorFormula>> multi
      = amgv.makeArray("multi",
        NumeralType.IntegerType,
        FormulaType.getArrayType(
            NumeralType.IntegerType, FormulaType.getBitvectorTypeWithSize(32)));

    BitvectorFormula valueInMulti = amgv.select(amgv.select(multi, _i), _i);

    assertThat(valueInMulti.toString()).isEqualTo("(select (select multi i) i)"); // Compatibility to all solvers not guaranteed
  }

  @Test
  public void testMakePredicate1() throws UnrecognizedCCodeException, InterruptedException {
    // a[2] != 0

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makePredicate(
        _a_at_2_notequal_0.getSecond(),
        _a_at_2_notequal_0.getFirst(),
        "foo", ssa);

    assertThat(mgr.getUnsafeFormulaManager().simplify(result).toString())
    .comparesEqualTo(bmgr.not(
        imgr.equal(
            amgr.select(_smt_a, imgr.makeNumber(2)),
            imgr.makeNumber(0))).toString());

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
      .comparesEqualTo(bmgr.not(
          imgr.equal(
              amgr.select(_smt_a, amgr.select(_smt_a, imgr.makeNumber(2))),
              imgr.makeNumber(0))).toString());

  }

  @Test
  public void testArrayDesignatedList() throws UnrecognizedCCodeException, InterruptedException {
    // int a[1000] = { 1, 3, 5, 7, 9, [1000-5] = 8, 6, 4, 2, 0 };
    //  all other elements should be initialized with ZERO
    //  Solvers support this by allowing to specify a default value for arrays

    // This test is important because it also tests the initialization of arrays with a default value!
  }

  private CInitializerExpression createIntInitExpr(int pValue) {
    return new CInitializerExpression(
        FileLocation.DUMMY,
        CIntegerLiteralExpression.createDummyLiteral(pValue, CNumericTypes.INT));
  }

  @Test
  public void testArrayInitializerList() throws InterruptedException, CPATransferException {
    // int x[] = { 1, 3, 5, 7 } ;

    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _x = makeDeclaration(
        "x",
        new CArrayType(false, false, CNumericTypes.INT, null),
        new CInitializerList(FileLocation.DUMMY,
            Lists.<CInitializer>newArrayList(
                createIntInitExpr(1),
                createIntInitExpr(3),
                createIntInitExpr(5),
                createIntInitExpr(7)
                )));

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makeDeclaration(
        _x.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.getUnsafeFormulaManager().simplify(result).toString()
        .replaceAll("\n", " ").replaceAll("  ", " "))
        .isEqualTo("TODO");
  }

  @Test
  public void testArrayDeclaration1() throws InterruptedException, CPATransferException {
    // int arr[100];

    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _arr = makeDeclaration(
        "arr",
        new CArrayType(
            false,
            false,
            CNumericTypes.INT,
            CIntegerLiteralExpression.createDummyLiteral(100, CNumericTypes.INT)),
            null
            );

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();

    final BooleanFormula resultBwd = ctfBwd.makeDeclaration(
        _arr.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.getUnsafeFormulaManager().simplify(resultBwd).toString())
      .comparesEqualTo("true");

    final BooleanFormula resultFwd = ctfFwd.makeDeclaration(
        _arr.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.getUnsafeFormulaManager().simplify(resultFwd).toString())
      .isEqualTo("The result should be an initialized array"); //TODO
  }

  @Test
  public void testMultiDimensional1() throws InterruptedException, CPATransferException {
    // int arr2d[3][7];

    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _arr2d = makeDeclaration(
        "_arr2d",
        new CArrayType(
            false,
            false,
            new CArrayType(
                false,
                false,
                CNumericTypes.INT,
                CIntegerLiteralExpression.createDummyLiteral(7, CNumericTypes.INT)),
            CIntegerLiteralExpression.createDummyLiteral(3, CNumericTypes.INT)
           ),
          null
        );

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();

    final BooleanFormula resultBwd = ctfBwd.makeDeclaration(
        _arr2d.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.getUnsafeFormulaManager().simplify(resultBwd).toString())
      .isEqualTo("true");

    final BooleanFormula resultFwd = ctfFwd.makeDeclaration(
        _arr2d.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.getUnsafeFormulaManager().simplify(resultFwd).toString())
      .isEqualTo("The result should be an initialized array"); //TODO
  }

  @Test
  public void testMultiDimensional2() throws UnrecognizedCCodeException, InterruptedException {
    // a2d[3][7] == 23;
    // a2d[3][7] = 23;

    final CArrayType arrayWith10 = new CArrayType(
        false,
        false,
        CNumericTypes.INT,
        CIntegerLiteralExpression.createDummyLiteral(10, CNumericTypes.INT));

    final CArrayType typeOf_arr2d = new CArrayType(
        false,
        false,
        arrayWith10,
        CIntegerLiteralExpression.createDummyLiteral(10, CNumericTypes.INT)
       );

    final Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _arr2d
      = makeDeclaration("a2d", typeOf_arr2d, null);

    CArraySubscriptExpression _arr2d_at_3_7 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        new CArraySubscriptExpression(
            FileLocation.DUMMY,
            arrayWith10,
            _arr2d.getThird(),
            CIntegerLiteralExpression.createDummyLiteral(3, CNumericTypes.INT)),
         CIntegerLiteralExpression.createDummyLiteral(7, CNumericTypes.INT)
        );

    Pair<CAssumeEdge, CExpression> _arr2d_at_3_7_equal_23 = makeAssume(expressionBuilder.buildBinaryExpression(
        _arr2d_at_3_7,
        CIntegerLiteralExpression.createDummyLiteral(23, CNumericTypes.INT),
        BinaryOperator.EQUALS));

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makePredicate(
        _arr2d_at_3_7_equal_23.getSecond(),
        _arr2d_at_3_7_equal_23.getFirst(),
        "foo", ssa);

    assertThat(mgr.getUnsafeFormulaManager().simplify(result).toString())
    .comparesEqualTo(
        imgr.equal(
            amgr.select(
                amgr.select(
                    _smt_a2d, imgr.makeNumber(3)),
                    imgr.makeNumber(7)),
            imgr.makeNumber(23)).toString());
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
