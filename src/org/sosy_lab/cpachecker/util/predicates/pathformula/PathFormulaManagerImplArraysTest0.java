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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.makeDeclaration;

import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Before;
import org.junit.Test;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

import java.util.Optional;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class PathFormulaManagerImplArraysTest0 extends SolverBasedTest0 {

  private static final CArrayType unlimitedIntArrayType = new CArrayType(false, false, CNumericTypes.INT, null);

  private Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _a;

  private CArraySubscriptExpression _a_at_1;
  private CArraySubscriptExpression _a_at_2;
  private CArraySubscriptExpression _a_at_3;
  private CArraySubscriptExpression _a_at_0;

  private CIntegerLiteralExpression _1;
  private CIntegerLiteralExpression _2;
  private CIntegerLiteralExpression _10;
  private CIntegerLiteralExpression _20;
  private CIntegerLiteralExpression _30;
  private CIntegerLiteralExpression _100;

  private PathFormulaManagerImpl pfmgrFwd;
  private PathFormulaManagerImpl pfmgrBwd;
  private CBinaryExpressionBuilder eb;

  private FormulaManagerView mgv;
  private Solver solver;

  private CExpression _0;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    Configuration myConfig = Configuration.builder()
        .copyFrom(config)
        .setOption("cpa.predicate.handlePointerAliasing", "false")
        .setOption("cpa.predicate.handleArrays", "true")
        .build();

    solver = new Solver(factory, config, LogManager.createTestLogManager());
    mgv = solver.getFormulaManager();

    pfmgrFwd =
        new PathFormulaManagerImpl(
            mgv,
            myConfig,
            LogManager.createTestLogManager(),
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.FORWARD);

    pfmgrBwd =
        new PathFormulaManagerImpl(
            mgv,
            myConfig,
            LogManager.createTestLogManager(),
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.BACKWARD);

    eb = new CBinaryExpressionBuilder(MachineModel.LINUX64, logger);

    _a = makeDeclaration("a", unlimitedIntArrayType, null);

    _0 = CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT);
    _1 = CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT);
    _2 = CIntegerLiteralExpression.createDummyLiteral(2, CNumericTypes.INT);
    _10 = CIntegerLiteralExpression.createDummyLiteral(10, CNumericTypes.INT);
    _20 = CIntegerLiteralExpression.createDummyLiteral(20, CNumericTypes.INT);
    _30 = CIntegerLiteralExpression.createDummyLiteral(30, CNumericTypes.INT);
    _100 = CIntegerLiteralExpression.createDummyLiteral(100, CNumericTypes.INT);

    _a_at_0 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        CIntegerLiteralExpression.ZERO);

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

    _a_at_3 = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        CIntegerLiteralExpression.createDummyLiteral(2, CNumericTypes.INT));
  }

  @Test
  public void testForwardArrayPathSat1() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isFalse();
  }

  @Test
  public void testForwardArrayPathUnsat1() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // if (a[0] != 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testForwardArrayPathSat2() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[1] = 2;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
    = TestDataTools.makeAssignment(_a_at_1, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isFalse();
  }

  @Test
  public void testForwardArrayPathUnsat2() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[1] = 2;
    // if (a[0] == 2) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
      = TestDataTools.makeAssignment(_a_at_1, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _2, BinaryOperator.EQUALS));

    PathFormula result = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testForwardArrayPathUnsat3() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[0] = 2;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
      = TestDataTools.makeAssignment(_a_at_0, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testForwardArrayPathSat3() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[0] = 2;
    // if (a[0] != 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
      = TestDataTools.makeAssignment(_a_at_0, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isFalse();
  }

  @Test
  public void testForwardArrayBranchingUnsat1() throws CPATransferException, InterruptedException, SolverException {
    //1: a[0] = 0;
    //2: if (a[1] == 10) {
    //3:    a[2] = 20;
    //4:    a[3] = 30;
    //5:  } else {
    //6:    a[2] = 0;
    //7:    a[3] = 0;
    //   }
    //8: if (a[2] > 100) {
    //     ERROR: ...
    //   }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _0);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));
    Pair<CAssumeEdge, CExpression> _op5
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));

    Pair<CFAEdge, CExpressionAssignmentStatement> _op3
      = TestDataTools.makeAssignment(_a_at_2, _20);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op4
      = TestDataTools.makeAssignment(_a_at_3, _30);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op6
      = TestDataTools.makeAssignment(_a_at_2, _0);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op7
      = TestDataTools.makeAssignment(_a_at_3, _0);

    Pair<CAssumeEdge, CExpression> _op8
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_2, _100, BinaryOperator.GREATER_THAN));

    PathFormula branch1 = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst(),
        _op4.getFirst()
        ));
    PathFormula branch2 = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op5.getFirst(),
        _op6.getFirst(),
        _op7.getFirst()
        ));
    PathFormula result = pfmgrFwd.makeOr(branch1, branch2);
    result = pfmgrFwd.makeAnd(result, _op8.getFirst());

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testForwardArrayBranchingUnsat2() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 0;
    // if (a[1] == 10) {
    //    a[2] = 20;
    //    a[3] = 30;
    // } else {
    //    a[2] = 0;
    // }
    // if (a[2] > 100) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _0);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));
    Pair<CAssumeEdge, CExpression> _op5
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));

    Pair<CFAEdge, CExpressionAssignmentStatement> _op3
      = TestDataTools.makeAssignment(_a_at_2, _20);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op4
      = TestDataTools.makeAssignment(_a_at_3, _30);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op6
      = TestDataTools.makeAssignment(_a_at_2, _0);

    Pair<CAssumeEdge, CExpression> _op8
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_2, _100, BinaryOperator.GREATER_THAN));

    PathFormula branch1 = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst(),
        _op4.getFirst()
        ));
    PathFormula branch2 = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op5.getFirst(),
        _op6.getFirst()
        ));
    PathFormula result = pfmgrFwd.makeOr(branch1, branch2);
    result = pfmgrFwd.makeAnd(result, _op8.getFirst());

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testForwardArrayBranchingUnsat3() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 0;
    // if (a[1] == 10) {
    //    a[2] = 20;
    //    a[3] = 30;
    // }
    // if (a[0] > 10) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _0);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));
    Pair<CAssumeEdge, CExpression> _op5
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));

    Pair<CFAEdge, CExpressionAssignmentStatement> _op3
      = TestDataTools.makeAssignment(_a_at_2, _20);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op4
      = TestDataTools.makeAssignment(_a_at_3, _30);

    Pair<CAssumeEdge, CExpression> _op8
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_2, _100, BinaryOperator.GREATER_THAN));

    PathFormula branch1 = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op2.getFirst(),
        _op3.getFirst(),
        _op4.getFirst()
        ));
    PathFormula branch2 = pfmgrFwd.makeFormulaForPath(ImmutableList.of(
        _op1.getFirst(),
        _op5.getFirst()
        ));

    PathFormula result = pfmgrFwd.makeOr(branch1, branch2);
    result = pfmgrFwd.makeAnd(result, _op8.getFirst());

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  // > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > >
  // B A C K W A R D
  // > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > > >

  @Test
  public void testBackwardArrayPathSat1() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op2.getFirst(),
        _op1.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isFalse();
  }

  @Test
  public void testBackwardArrayPathUnsat1() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // if (a[0] != 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op2.getFirst(),
        _op1.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testBackwardArrayPathSat2() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[1] = 2;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
    = TestDataTools.makeAssignment(_a_at_1, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op3.getFirst(),
        _op2.getFirst(),
        _op1.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isFalse();
  }

  @Test
  public void testBackwardArrayPathUnsat2() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[1] = 2;
    // if (a[0] == 2) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
      = TestDataTools.makeAssignment(_a_at_1, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _2, BinaryOperator.EQUALS));

    PathFormula result = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op3.getFirst(),
        _op2.getFirst(),
        _op1.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testBackwardArrayPathUnsat3() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[0] = 2;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
      = TestDataTools.makeAssignment(_a_at_0, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op3.getFirst(),
        _op2.getFirst(),
        _op1.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testBackwardArrayPathSat3() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 1;
    // a[0] = 2;
    // if (a[0] != 1) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _1);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op2
      = TestDataTools.makeAssignment(_a_at_0, _2);

    Pair<CAssumeEdge, CExpression> _op3
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _1, BinaryOperator.EQUALS));

    PathFormula result = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op3.getFirst(),
        _op2.getFirst(),
        _op1.getFirst()
        ));

    assertThat(solver.isUnsat(result.getFormula())).isFalse();
  }

  @Test
  public void testBackwardArrayBranchingUnsat1() throws CPATransferException, InterruptedException, SolverException {
    //1: a[0] = 0;
    //2: if (a[1] == 10) {
    //3:    a[2] = 20;
    //4:    a[3] = 30;
    //5:  } else {
    //6:    a[2] = 0;
    //7:    a[3] = 0;
    //   }
    //8: if (a[2] > 100) {
    //     ERROR: ...
    //   }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _0);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));
    Pair<CAssumeEdge, CExpression> _op5
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));

    Pair<CFAEdge, CExpressionAssignmentStatement> _op3
      = TestDataTools.makeAssignment(_a_at_2, _20);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op4
      = TestDataTools.makeAssignment(_a_at_3, _30);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op6
      = TestDataTools.makeAssignment(_a_at_2, _0);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op7
      = TestDataTools.makeAssignment(_a_at_3, _0);

    Pair<CAssumeEdge, CExpression> _op8
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_2, _100, BinaryOperator.GREATER_THAN));

    PathFormula branch1 = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op8.getFirst(),
        _op4.getFirst(),
        _op3.getFirst(),
        _op2.getFirst()
        ));
    PathFormula branch2 = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op8.getFirst(),
        _op7.getFirst(),
        _op6.getFirst(),
        _op5.getFirst()
        ));
    PathFormula result = pfmgrBwd.makeOr(branch1, branch2);
    result = pfmgrBwd.makeAnd(result, _op1.getFirst());

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testBackwardArrayBranchingUnsat2() throws CPATransferException, InterruptedException, SolverException {
    //1: a[0] = 0;
    //2: if (a[1] == 10) {
    //3:    a[2] = 20;
    //4:    a[3] = 30;
    //5: } else {
    //6:    a[2] = 0;
    //   }
    //8: if (a[2] > 100) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _0);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));
    Pair<CAssumeEdge, CExpression> _op5
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));

    Pair<CFAEdge, CExpressionAssignmentStatement> _op3
      = TestDataTools.makeAssignment(_a_at_2, _20);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op4
      = TestDataTools.makeAssignment(_a_at_3, _30);

    Pair<CFAEdge, CExpressionAssignmentStatement> _op6
      = TestDataTools.makeAssignment(_a_at_2, _0);

    Pair<CAssumeEdge, CExpression> _op8
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_2, _100, BinaryOperator.GREATER_THAN));

    PathFormula branch1 = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op8.getFirst(),
        _op4.getFirst(),
        _op3.getFirst(),
        _op2.getFirst()
        ));
    PathFormula branch2 = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op8.getFirst(),
        _op6.getFirst(),
        _op5.getFirst()
        ));
    PathFormula result = pfmgrBwd.makeOr(branch1, branch2);
    result = pfmgrBwd.makeAnd(result, _op1.getFirst());

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }

  @Test
  public void testBackwardArrayBranchingUnsat3() throws CPATransferException, InterruptedException, SolverException {
    // a[0] = 0;
    // if (a[1] == 10) {
    //    a[2] = 20;
    //    a[3] = 30;
    // }
    // if (a[0] > 10) {
    //  ERROR: ...
    // }

    Pair<CFAEdge, CExpressionAssignmentStatement> _op1
      = TestDataTools.makeAssignment(_a_at_0, _0);

    Pair<CAssumeEdge, CExpression> _op2
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));
    Pair<CAssumeEdge, CExpression> _op5
      = TestDataTools.makeNegatedAssume(eb.buildBinaryExpression(_a_at_0, _10, BinaryOperator.EQUALS));

    Pair<CFAEdge, CExpressionAssignmentStatement> _op3
      = TestDataTools.makeAssignment(_a_at_2, _20);
    Pair<CFAEdge, CExpressionAssignmentStatement> _op4
      = TestDataTools.makeAssignment(_a_at_3, _30);

    Pair<CAssumeEdge, CExpression> _op8
      = TestDataTools.makeAssume(eb.buildBinaryExpression(_a_at_2, _100, BinaryOperator.GREATER_THAN));

    PathFormula branch1 = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op8.getFirst(),
        _op4.getFirst(),
        _op3.getFirst(),
        _op2.getFirst()
        ));
    PathFormula branch2 = pfmgrBwd.makeFormulaForPath(ImmutableList.of(
        _op8.getFirst(),
        _op5.getFirst()
        ));

    PathFormula result = pfmgrBwd.makeOr(branch1, branch2);
    result = pfmgrBwd.makeAnd(result, _op1.getFirst());

    assertThat(solver.isUnsat(result.getFormula())).isTrue();
  }


}
