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
import static org.sosy_lab.cpachecker.util.test.TestDataTools.INT_ZERO_INITIALIZER;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.makeAssignment;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.makeAssume;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.makeDeclaration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.NumeralType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
@SuppressFBWarnings({
  "NP_NONNULL_PARAM_VIOLATION",
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"
})
@RunWith(Parameterized.class)
public class CToFormulaConverterWithArraysTest extends SolverBasedTest0 {

  @Parameters(name="{0} {1}")
  public static List<Object[]> getAllSolvers() {
    List<Object[]> parameters = new ArrayList<>();
    for (Solvers solver : Solvers.values()) {
      for (MachineModel machineModel : MachineModel.values()) {
        parameters.add(new Object[]{solver, machineModel});
      }
    }
    return parameters;
  }

  @Parameter(0)
  public Solvers solver;

  @Parameter(1)
  public MachineModel machineModel;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Before
  public void allTestsRequireArrays() {
    super.requireArrays();
  }

  @VisibleForTesting
  private static class CToFormulaConverterWithArraysUnderTest
      extends CToFormulaConverterWithArrays {
    public CToFormulaConverterWithArraysUnderTest(
        FormulaEncodingOptions pOptions,
        FormulaManagerView pFmgr,
        MachineModel pMachineModel,
        Optional<VariableClassification> pVariableClassification,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier,
        CtoFormulaTypeHandler pTypeHandler,
        AnalysisDirection pDirection) {
      super(pOptions, pFmgr, pMachineModel, pVariableClassification, pLogger,
          pShutdownNotifier, pTypeHandler, pDirection);
    }

    @Override
    protected BooleanFormula makeDeclaration(
        CDeclarationEdge pEdge,
        String pFunction,
        SSAMapBuilder pSsa,
        PointerTargetSetBuilder pPts,
        Constraints pConstraints,
        ErrorConditions pErrorConditions)
        throws UnrecognizedCCodeException, InterruptedException {
      return super.makeDeclaration(pEdge, pFunction, pSsa, pPts, pConstraints,
          pErrorConditions);
    }

    @Override
    protected BooleanFormula makeAssignment(
        CLeftHandSide pLhs,
        CLeftHandSide pLhsForChecking,
        CRightHandSide pRhs,
        CFAEdge pEdge,
        String pFunction,
        SSAMapBuilder pSsa,
        PointerTargetSetBuilder pPts,
        Constraints pConstraints,
        ErrorConditions pErrorConditions)
        throws UnrecognizedCCodeException, InterruptedException {
      return super.makeAssignment(pLhs, pLhsForChecking, pRhs, pEdge, pFunction,
          pSsa, pPts, pConstraints, pErrorConditions);
    }
  }

  private static final CArrayType unlimitedIntArrayType = new CArrayType(
      false, false, CNumericTypes.INT, null);

  private CToFormulaConverterWithArraysUnderTest ctfBwd;
  private CToFormulaConverterWithArraysUnderTest ctfFwd;

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
  private Pair<CFAEdge, CExpressionAssignmentStatement> _a_assign_0_at_2;
  private Pair<CAssumeEdge, CExpression> _i_notequal_0;

  private CArraySubscriptExpression _b_at_i;
  private CArraySubscriptExpression _a_at_i;
  private CArraySubscriptExpression _a_at_1;
  private CArraySubscriptExpression _a_at_2;

  private ArrayFormula<IntegerFormula, IntegerFormula> _smt_a_ssa1;
  private ArrayFormula<IntegerFormula, IntegerFormula> _smt_b_ssa1;
  private ArrayFormula<IntegerFormula, IntegerFormula> _smt_a_ssa2;
  private ArrayFormula<IntegerFormula,
      ArrayFormula<IntegerFormula, IntegerFormula>> _smt_a2d;

  @Before
  public void setUp() throws Exception {
    requireRationals();
    mgrv = new FormulaManagerView(context.getFormulaManager(),
        config, logger);

    FormulaEncodingOptions opts = new FormulaEncodingOptions(
        Configuration.defaultConfiguration());
    CtoFormulaTypeHandlerWithArrays th = new CtoFormulaTypeHandlerWithArrays(
        logger, machineModel);
    expressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);

    ctfBwd =
        new CToFormulaConverterWithArraysUnderTest(
            opts,
            mgrv,
            machineModel,
            Optional.empty(),
            logger,
            ShutdownNotifier.createDummy(),
            th,
            AnalysisDirection.BACKWARD);

    ctfFwd =
        new CToFormulaConverterWithArraysUnderTest(
            opts,
            mgrv,
            machineModel,
            Optional.empty(),
            logger,
            ShutdownNotifier.createDummy(),
            th,
            AnalysisDirection.FORWARD);
  }

  @Before
  public void setupCfaTestData() throws UnrecognizedCCodeException {

    _smt_b_ssa1 = amgr.makeArray("b@1", NumeralType.IntegerType,
        NumeralType.IntegerType);
    _smt_a_ssa1 = amgr.makeArray("a@1", NumeralType.IntegerType,
        NumeralType.IntegerType);
    _smt_a_ssa2 = amgr.makeArray("a@2", NumeralType.IntegerType,
        NumeralType.IntegerType);
    _smt_a2d = amgr.makeArray("a2d@1", NumeralType.IntegerType,
        FormulaType.getArrayType(NumeralType.IntegerType,
            NumeralType.IntegerType));

    _a = makeDeclaration("a", unlimitedIntArrayType, null);
    _b = makeDeclaration("b", unlimitedIntArrayType, null);
    _bl = makeDeclaration("bl", CNumericTypes.INT, null);
    _i = makeDeclaration("i", CNumericTypes.INT, INT_ZERO_INITIALIZER);

    _b_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        _b.getThird(),
        _i.getThird());

    _a_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        _a.getThird(),
        _i.getThird());

    _a_at_1 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        _a.getThird(),
        CIntegerLiteralExpression.ONE);

    _a_at_2 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
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

    _a_assign_0_at_2 = makeAssignment(_a_at_2, CIntegerLiteralExpression.ZERO);

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
  public void testSimpleArrayAssume()
      throws UnrecognizedCCodeException, InterruptedException {
    // a[2] != 0

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makePredicate(
        _a_at_2_notequal_0.getSecond(),
        _a_at_2_notequal_0.getFirst(),
        "foo", ssa);

    assertThat(mgr.simplify(result))
    .isEqualTo(bmgr.not(
        imgr.equal(
            amgr.select(_smt_a_ssa1, imgr.makeNumber(2)),
            imgr.makeNumber(0))));
  }

  @Test
  public void testSimpleArrayAssign()
      throws UnrecognizedCCodeException, InterruptedException {
    // a[2] = 1;
    // ----->
    // (= a@2 (store a@1 2 1))

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    ssa = ssa.setIndex("a", unlimitedIntArrayType , 1);

    Pair<CFAEdge, CExpressionAssignmentStatement> assign = TestDataTools.makeAssignment(
        _a_assign_0_at_2.getSecond().getLeftHandSide(),
        CIntegerLiteralExpression.ONE);

    BooleanFormula result = ctfFwd.makeAssignment(
        assign.getSecond().getLeftHandSide(),
        assign.getSecond().getLeftHandSide(),
        assign.getSecond().getRightHandSide(),
        assign.getFirst(),
        "foo", ssa, null, null, null);

    assertThat(mgr.simplify(result))
        .isEqualTo(amgr.equivalence(
            _smt_a_ssa2, amgr.store(
                _smt_a_ssa1, imgr.makeNumber(2), imgr.makeNumber(1))));
  }

  @Test
  public void testSimpleArrayAssignBackward()
      throws UnrecognizedCCodeException, InterruptedException {
    // a[2] = 1;
    // ----->
    // (= a@1 (store a@2 2 1))

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    ssa = ssa.setIndex("a", unlimitedIntArrayType , 1);

    Pair<CFAEdge, CExpressionAssignmentStatement> assign = TestDataTools.makeAssignment(
        _a_assign_0_at_2.getSecond().getLeftHandSide(),
        CIntegerLiteralExpression.ONE);

    BooleanFormula result = ctfBwd.makeAssignment(
        assign.getSecond().getLeftHandSide(),
        assign.getSecond().getLeftHandSide(),
        assign.getSecond().getRightHandSide(),
        assign.getFirst(),
        "foo", ssa, null, null, null);

    assertThat(mgr.simplify(result))
      .isEqualTo(amgr.equivalence(
          _smt_a_ssa1, amgr.store(
              _smt_a_ssa2, imgr.makeNumber(2), imgr.makeNumber(1))));
  }

  @Test
  public void testSimpleRhsArrayAssign()
      throws UnrecognizedCCodeException, InterruptedException {
    // i = a[2];

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    Pair<CFAEdge, CExpressionAssignmentStatement> op = TestDataTools.makeAssignment(
        _i.getThird(), _a_at_2);

    BooleanFormula result = ctfBwd.makeAssignment(
        op.getSecond().getLeftHandSide(),
        op.getSecond().getLeftHandSide(),
        op.getSecond().getRightHandSide(),
        op.getFirst(),
        "foo", ssa, null, null, null);

    assertThat(mgr.simplify(result))
        .isEqualTo(imgr.equal(imgr.makeVariable("i@1"), amgr.select(
            _smt_a_ssa1, imgr.makeNumber(2))));

  }

  @Test
  public void testNestedArrayAssign()
      throws UnrecognizedCCodeException, InterruptedException {
    // a[a[2]] = 1;
    // ----->
    // (= a@2 (store a@1 (select a@1 2) 1))

    CArraySubscriptExpression _a_at__a_at_2 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        _a_at_2);

    Pair<CFAEdge, CExpressionAssignmentStatement> op = TestDataTools.makeAssignment(
        _a_at__a_at_2,
        CIntegerLiteralExpression.ONE);

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfFwd.makeAssignment(
        op.getSecond().getLeftHandSide(),
        op.getSecond().getLeftHandSide(),
        op.getSecond().getRightHandSide(),
        op.getFirst(),
        "foo", ssa, null, null, null);

    assertThat(mgr.simplify(result))
        .isEqualTo(amgr.equivalence(
            _smt_a_ssa2, amgr.store(
                _smt_a_ssa1, amgr.select(
                    _smt_a_ssa1, imgr.makeNumber(2)),
                imgr.makeNumber(1))));
  }

  @Test
  public void testNestedArrayAssume()
      throws UnrecognizedCCodeException, InterruptedException {
    // a[a[2]] != 0

    CArraySubscriptExpression _a_at__a_at_2 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        _a.getThird(),
        _a_at_2);
    Pair<CAssumeEdge, CExpression> _a_at__a_at_2_notequal_0 = makeAssume(
        expressionBuilder.buildBinaryExpression(
            _a_at__a_at_2,
            CIntegerLiteralExpression.ZERO,
            BinaryOperator.NOT_EQUALS));

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfBwd.makePredicate(
        _a_at__a_at_2_notequal_0.getSecond(),
        _a_at__a_at_2_notequal_0.getFirst(),
        "foo", ssa);

    assertThat(mgr.simplify(result))
        .isEqualTo(bmgr.not(imgr.equal(
            amgr.select(_smt_a_ssa1, amgr.select(_smt_a_ssa1, imgr.makeNumber(2))),
            imgr.makeNumber(0))));
  }

  @Test
  @Ignore
  public void testArrayDesignatedList() throws UnrecognizedCCodeException, InterruptedException {
    /*
     * java.lang.AssertionError: The subject was expected to be false, but was true
     */
    // int a[1000] = { 1, 3, 5, 7, 9, [1000-5] = 8, 6, 4, 2, 0 };
    //  all other elements should be initialized with ZERO
    //  Solvers support this by allowing to specify a default value for arrays

    // This test is important because it also tests the initialization of arrays with a default value!
    assertThat(true).isFalse();
  }

  private CInitializerExpression createIntInitExpr(int pValue) {
    return new CInitializerExpression(
        FileLocation.DUMMY,
        CIntegerLiteralExpression.createDummyLiteral(pValue, CNumericTypes.INT));
  }

  @Test
  @Ignore
  public void testArrayInitializerList() throws InterruptedException, CPATransferException {
    /*
     * java.lang.IllegalArgumentException: Not supported interface
     */
    // int x[] = { 1, 3, 5, 7 } ;

    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _x =
        makeDeclaration(
            "x",
            new CArrayType(false, false, CNumericTypes.INT, null),
            new CInitializerList(
                FileLocation.DUMMY,
                ImmutableList.of(
                    createIntInitExpr(1),
                    createIntInitExpr(3),
                    createIntInitExpr(5),
                    createIntInitExpr(7))));

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfFwd.makeDeclaration(
        _x.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.simplify(result).toString()
        .replaceAll("\n", " ").replaceAll("  ", " "))
        .isEqualTo("TODO");
  }

  @Test
  public void testArrayDeclaration1()
      throws InterruptedException, CPATransferException {
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

    assertThat(mgr.simplify(resultBwd))
      .isEqualTo(bmgr.makeTrue());

    final BooleanFormula resultFwd = ctfFwd.makeDeclaration(
        _arr.getFirst(), "foo", ssa, null, null, null);

    assertThat(mgr.simplify(resultFwd))
      .isEqualTo(bmgr.makeTrue());
  }

  @Test
  @Ignore
  public void testArrayAssignment() throws UnrecognizedCCodeException, InterruptedException {
    /*
     * java.lang.AssertionError: Not true that <true> is equal to <(= a@1 b@1)>
     */
    // int a[];
    // int b[];
    // a = b;

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    Pair<CFAEdge, CExpressionAssignmentStatement> _assing = TestDataTools.makeAssignment(_a.getThird(), _b.getThird());

    BooleanFormula result = ctfFwd.makeAssignment(
        _assing.getSecond().getLeftHandSide(),
        _assing.getSecond().getLeftHandSide(),
        _assing.getSecond().getRightHandSide(),
        _assing.getFirst(),
        "foo", ssa, null, null, null);

    // TODO: Aliasing not handled!!!!!!!!!!
    assertThat(result).isEqualTo(amgr.equivalence(_smt_a_ssa1, _smt_b_ssa1));
  }

  @Test
  public void testMultiDimensional1() throws InterruptedException, CPATransferException {
    // TODO This just tests if the view creates the string we expect. It's not
    // real test, as we have different views for different solvers.
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

    final BooleanFormula resultFwd = ctfFwd.makeDeclaration(
        _arr2d.getFirst(), "foo", ssa, null, null, null);

    if (solver == Solvers.MATHSAT5) {
      assertThat(mgr.simplify(resultBwd).toString())
          .isEqualTo("`true`");

      assertThat(mgr.simplify(resultFwd).toString())
          .isEqualTo("`true`");
    } else {
      assertThat(mgr.simplify(resultBwd).toString())
          .isEqualTo("true");

      assertThat(mgr.simplify(resultFwd).toString())
          .isEqualTo("true");
    }
  }

  @Test
  @Ignore
  public void testMultiDimensionalAssign() throws UnrecognizedCCodeException, InterruptedException {
    // a2d[3][7] = 23;

    final CArrayType arrayWith10 = new CArrayType(
        false, false, CNumericTypes.INT, CIntegerLiteralExpression
            .createDummyLiteral(10, CNumericTypes.INT));
    final CArrayType typeOf_arr2d = new CArrayType(
        false, false, arrayWith10, CIntegerLiteralExpression
            .createDummyLiteral(10, CNumericTypes.INT));
    final Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _arr2d
        = makeDeclaration("a2d", typeOf_arr2d, null);
    CArraySubscriptExpression _arr2d_at_3_7 = new CArraySubscriptExpression(
        FileLocation.DUMMY, CNumericTypes.INT,
        new CArraySubscriptExpression(
            FileLocation.DUMMY, arrayWith10, _arr2d.getThird(),
            CIntegerLiteralExpression.createDummyLiteral(3, CNumericTypes.INT)),
        CIntegerLiteralExpression.createDummyLiteral(7, CNumericTypes.INT));

    Pair<CFAEdge, CExpressionAssignmentStatement> op = TestDataTools.makeAssignment(
        _arr2d_at_3_7,
        CIntegerLiteralExpression.createDummyLiteral(23, CNumericTypes.INT));

    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula result = ctfFwd.makeAssignment(
        op.getSecond().getLeftHandSide(),
        op.getSecond().getLeftHandSide(),
        op.getSecond().getRightHandSide(),
        op.getFirst(),
        "foo", ssa, null, null, null);

    assertThat(mgr.simplify(result))
        .isEqualTo(
            amgr.store(_smt_a2d, imgr.makeNumber(3),
                amgr.store(amgr.select(_smt_a2d, imgr.makeNumber(3)),
                    imgr.makeNumber(7), imgr.makeNumber(23))));
  }

  @Test
  public void testMultiDimensionalAssume() throws UnrecognizedCCodeException, InterruptedException {
    // a2d[3][7] == 23;

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

    assertThat(mgr.simplify(result))
    .isEqualTo(
        imgr.equal(
            amgr.select(
                amgr.select(
                    _smt_a2d, imgr.makeNumber(3)),
                    imgr.makeNumber(7)),
            imgr.makeNumber(23)));
  }

  @Test
  @Ignore
  public void testArrayAsPointer1() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[0]
    // and
    //    *array
  }

  @Test
  @Ignore
  public void testArrayAsPointer2() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[2]
    // and
    //    *(array + 2)
  }

  @Test
  @Ignore
  public void testArrayAsPointer3() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[n - 1]
    // and
    //    *(array + n - 1)
  }

  @Test
  @Ignore
  public void testArrayAsPointer4() throws UnrecognizedCCodeException, InterruptedException {
    // Equivalence of
    //    array[i - 1][j - 1]
    // and
    //    *(*(array + i - 1) + j - 1)
  }

  @Test
  @Ignore
  public void testArrayMalloc() throws UnrecognizedCCodeException, InterruptedException {
    // int* a = malloc(100 * sizeof(int));
  }

}
