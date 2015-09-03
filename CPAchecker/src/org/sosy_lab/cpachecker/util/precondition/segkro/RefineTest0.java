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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import static org.mockito.Mockito.*;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.*;

import org.junit.Before;
import org.mockito.Mockito;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@SuppressWarnings("unused")
public class RefineTest0 extends SolverBasedTest0 {

  private Refine refine;

  private FormulaManagerView mgrv;

  private CFAEdge _label_error;
  private CFAEdge _assume_i_geq_al;
  private CFAEdge _assume_b_at_i_neq_0;
  private CFAEdge _while;
  private CFAEdge _stmt_i_assign_0;
  private CFAEdge _stmt_declare_i;
  private CFAEdge _dummy;
  private CFAEdge _function_declaration;
  private CFAEdge _stmt_al_assign_0;
  private CAssumeEdge _assume_not_b_at_i_neq_0;
  private CDeclarationEdge _stmt_declare_al;
  private CFAEdge _stmt_a_at_i_assign_b_at_i;
  private CFAEdge _stmt_i_assign_i_plus_1;

  private CAssumeEdge _assume_p_neq_1;
  private CAssumeEdge _assume_p_eq_1;
  private CAssumeEdge _assume_x_leq_1000;
  private CAssumeEdge _assume_x_gt_1000;
  private CAssumeEdge _assume_x_gt_50;
  private CAssumeEdge _assume_x_leq_50;
  private CExpression _50;
  private CAssumeEdge _assume_x_geq_al;

  private CFANode _loc_1;
  private CFANode _loc_2;
  private CFANode _loc_3;
  private CFANode _loc_err;
  private CFANode _loc_end;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() throws InvalidConfigurationException {
    ConfigurationBuilder result = super.createTestConfigBuilder();
    result.setOption("cpa.predicate.handlePointerAliasing", "false");
    result.setOption("cpa.predicate.handleArrays", "true");

    return result;
  }

  @Before
  public void setUp() throws Exception {
    CFA cfa = mock(CFA.class);
    when(cfa.getMachineModel()).thenReturn(MachineModel.LINUX64);
    when(cfa.getVarClassification()).thenReturn(Optional.<VariableClassification>absent());

    mgrv = new FormulaManagerView(factory, config, TestLogManager.getInstance());
    Solver solver = new Solver(mgrv, factory, config, TestLogManager.getInstance());

    RuleEngine ruleEngine = new RuleEngine(logger, solver);
    ExtractNewPreds enp = new ExtractNewPreds(solver, ruleEngine);
    InterpolationWithCandidates ipc = new MinCorePrio(logger, Mockito.mock(CFA.class), solver);
    RegionManager regionManager = new BDDManagerFactory(config, logger).createRegionManager();
    AbstractionManager amgr = new AbstractionManager(regionManager, mgrv, config, logger, solver);
    refine = new Refine(config, logger, ShutdownNotifier.create(), cfa, solver, amgr, enp, ipc);

    // Test CFA elements...
    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, TestLogManager.getInstance());
    CArrayType unlimitedIntArrayType = new CArrayType(false, false, CNumericTypes.INT, null);

    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _i_decl = makeDeclaration("i", CNumericTypes.INT, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _x_decl = makeDeclaration("x", CNumericTypes.INT, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _p_decl = makeDeclaration("p", CNumericTypes.INT, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _al_decl = makeDeclaration("al", CNumericTypes.INT, INT_ZERO_INITIALIZER);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _a = makeDeclaration("a", unlimitedIntArrayType, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _b = makeDeclaration("b", unlimitedIntArrayType, null);
    Triple<CDeclarationEdge, CFunctionDeclaration, CFunctionType> _funct_decl = makeFunctionDeclaration("copy", CVoidType.VOID,
        Lists.<CParameterDeclaration>newArrayList(
            new CParameterDeclaration(FileLocation.DUMMY, unlimitedIntArrayType, "a"),
            new CParameterDeclaration(FileLocation.DUMMY, unlimitedIntArrayType, "b")
            ));

    CIdExpression _i = _i_decl.getThird();
    CIdExpression _p = _p_decl.getThird();
    CIdExpression _x = _x_decl.getThird();
    CIdExpression _al = makeVariable("al", CNumericTypes.INT);

    CIntegerLiteralExpression _0 = CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT);
    CIntegerLiteralExpression _1 = CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT);
    CIntegerLiteralExpression _50 = CIntegerLiteralExpression.createDummyLiteral(50, CNumericTypes.INT);
    CIntegerLiteralExpression _1000 = CIntegerLiteralExpression.createDummyLiteral(1000, CNumericTypes.INT);

    CBinaryExpression _i_plus_1 = expressionBuilder.buildBinaryExpression(_i, _1, BinaryOperator.PLUS);

    CArraySubscriptExpression _b_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _b.getThird(),
        _i);

    CArraySubscriptExpression _a_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _a.getThird(),
        _i);

    _while = TestDataTools.makeBlankEdge("while");
    _dummy = makeBlankEdge("dummy");
    _label_error = TestDataTools.makeBlankEdge("ERROR");

    _assume_i_geq_al = makeAssume(expressionBuilder.buildBinaryExpression(_i, _al, BinaryOperator.GREATER_EQUAL)).getFirst();
    _assume_b_at_i_neq_0 = makeAssume(expressionBuilder.buildBinaryExpression(_b_at_i, _0, BinaryOperator.NOT_EQUALS)).getFirst();
    _assume_not_b_at_i_neq_0 = makeNegatedAssume(expressionBuilder.buildBinaryExpression(_b_at_i, _0, BinaryOperator.NOT_EQUALS)).getFirst();

    _loc_1 = new CFANode("1");
    _loc_2 = new CFANode("2");
    _loc_3 = new CFANode("3");
    _loc_err = new CFANode("err");
    _loc_end = new CFANode("end");

    _assume_x_gt_1000 = makeAssume(expressionBuilder.buildBinaryExpression(_x, _1000, BinaryOperator.GREATER_THAN), _loc_2, _loc_err).getFirst();
    _assume_x_leq_1000 = makeAssume(expressionBuilder.buildBinaryExpression(_x, _1000, BinaryOperator.LESS_EQUAL), _loc_2, _loc_end).getFirst();
    _assume_x_gt_50 = makeAssume(expressionBuilder.buildBinaryExpression(_x, _50, BinaryOperator.GREATER_THAN), _loc_3, _loc_err).getFirst();
    _assume_x_leq_50 = makeAssume(expressionBuilder.buildBinaryExpression(_x, _50, BinaryOperator.LESS_EQUAL), _loc_3, _loc_end).getFirst();
    _assume_p_neq_1 = makeAssume(expressionBuilder.buildBinaryExpression(_p, _1, BinaryOperator.NOT_EQUALS), _loc_1, _loc_2).getFirst();
    _assume_p_eq_1 = makeAssume(expressionBuilder.buildBinaryExpression(_p, _1, BinaryOperator.EQUALS), _loc_1, _loc_3).getFirst();
    _assume_p_neq_1 = makeAssume(expressionBuilder.buildBinaryExpression(_p, _1, BinaryOperator.NOT_EQUALS), _loc_1, _loc_2).getFirst();
    _assume_p_eq_1 = makeAssume(expressionBuilder.buildBinaryExpression(_p, _1, BinaryOperator.EQUALS), _loc_1, _loc_3).getFirst();

    _stmt_a_at_i_assign_b_at_i = makeAssignment(_a_at_i, _b_at_i).getFirst();
    _stmt_i_assign_0 = makeAssignment(_i, _0).getFirst();
    _stmt_i_assign_i_plus_1 = makeAssignment(_i, _i_plus_1).getFirst();
    _stmt_al_assign_0 = makeAssignment(_al, _0).getFirst();

    _stmt_declare_i = _i_decl.getFirst();
    _stmt_declare_al = _al_decl.getFirst();
    _function_declaration= _funct_decl.getFirst();
  }

//  @Test
//  public void testRefineCase1() throws CPATransferException, SolverException, InterruptedException {
//
//    ARGPath traceError = mock(ARGPath.class);
//    when(traceError.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _label_error,
//        _assume_i_geq_al,
//        _assume_b_at_i_neq_0,
//        _while,
//        _stmt_i_assign_0,
//        _stmt_declare_i,
//        _stmt_declare_al
//        ));
//
//    ARGPath traceSafe = mock(ARGPath.class);
//    when(traceSafe.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_i_geq_al,
//        _stmt_i_assign_0,
//        _stmt_declare_i,
//        _stmt_declare_al
//        ));
//
//    PredicatePrecision result = refine.refine(
//        traceError, traceSafe,
//        traceSafe.getFirstPositionWith(_stmt_declare_i.getPredecessor()));
//
//    assertThat(result).isNotNull();
//    assertThat(result.getGlobalPredicates()).isNotEmpty();
//  }
//
//  @Test
//  public void testRefineCase2() throws CPATransferException, SolverException, InterruptedException {
//
//    ARGPath traceError = mock(ARGPath.class);
//    when(traceError.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _label_error,
//        _assume_i_geq_al,
//        _assume_b_at_i_neq_0,
//        _stmt_i_assign_i_plus_1,
//        _stmt_a_at_i_assign_b_at_i,
//        _assume_i_geq_al,
//        _assume_b_at_i_neq_0,
//        _while,
//        _stmt_i_assign_0,
//        _stmt_declare_i,
//        _stmt_declare_al
//        ));
//
//    ARGPath traceSafe = mock(ARGPath.class);
//    when(traceSafe.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_not_b_at_i_neq_0,
//        _stmt_i_assign_i_plus_1,
//        _stmt_a_at_i_assign_b_at_i,
//        _assume_i_geq_al,
//        _assume_b_at_i_neq_0,
//        _stmt_i_assign_0,
//        _stmt_declare_i,
//        _stmt_declare_al
//        ));
//
//    PredicatePrecision result = refine.refine(
//        traceError, traceSafe,
//        traceSafe.getFirstPositionWith(_stmt_declare_i.getPredecessor()));
//
//    assertThat(result).isNotNull();
//    assertThat(result.getGlobalPredicates()).isNotEmpty();
//  }
//
//  @Test
//  public void testRefineCase3() throws CPATransferException, SolverException, InterruptedException {
//
//    ARGPath traceError = mock(ARGPath.class);
//    when(traceError.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _stmt_i_assign_i_plus_1,
//        _assume_b_at_i_neq_0,
//        _stmt_declare_i
//        ));
//
//    ARGPath traceSafe = mock(ARGPath.class);
//    when(traceSafe.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _stmt_i_assign_0,
//        _assume_not_b_at_i_neq_0,
//        _stmt_declare_i
//        ));
//
//    PredicatePrecision result = refine.refine(
//        traceError, traceSafe,
//        traceSafe.getFirstPositionWith(_stmt_declare_i.getPredecessor()));
//
//    assertThat(result).isNotNull();
//    assertThat(result.getGlobalPredicates()).isNotEmpty();
//  }
//
//
//  @Test
//  public void testInterpolateCase1() throws SolverException, InterruptedException {
//
//    // (= (select |copy::b| 0) 0)
//    // (and (>= 0 al) (not (= (select |copy::b| 0) 0)))
//
//    ArrayFormula<IntegerFormula, IntegerFormula> _b
//      = amgr.makeArray("b", FormulaType.IntegerType, FormulaType.IntegerType);
//
//    BooleanFormula preconditionA = imgr.equal(
//        amgr.select(_b, imgr.makeNumber(0)), imgr.makeNumber(0));
//
//    BooleanFormula pPreconditionB = bmgr.and(Lists.newArrayList(
//        imgr.greaterOrEquals(imgr.makeNumber(0), imgr.makeVariable("al")),
//        bmgr.not(imgr.equal(amgr.select(_b, imgr.makeNumber(0)), imgr.makeNumber(0)))));
//
//    BooleanFormula result = refine.interpolate(preconditionA, pPreconditionB, null);
//
//    assertThat(result.toString()).isEqualTo(preconditionA.toString());
//  }
//
//  @Test
//  public void testWpDebug2Pair_12() throws SolverException, InterruptedException, CPATransferException {
//
//    ARGPath traceError = mock(ARGPath.class);
//    when(traceError.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_x_gt_1000,
//        _assume_p_neq_1
//        ));
//
//    ARGPath traceSafe = mock(ARGPath.class);
//    when(traceSafe.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_x_leq_1000,
//        _assume_p_neq_1
//        ));
//
//    PredicatePrecision result = refine.refine(traceError, traceSafe, traceSafe.getFirstPositionWith(_loc_1));
//
//    assertThat(result).isNotNull();
//    assertThat(result.getGlobalPredicates()).isNotEmpty();
//  }
//
//  @Test
//  public void testWpDebug2Pair_23() throws SolverException, InterruptedException, CPATransferException {
//
//    ARGPath traceError = mock(ARGPath.class);
//    when(traceError.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_x_gt_50,
//        _assume_p_eq_1
//        ));
//
//    ARGPath traceSafe = mock(ARGPath.class);
//    when(traceSafe.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_x_leq_1000,
//        _assume_p_neq_1
//        ));
//
//    PredicatePrecision result = refine.refine(traceError, traceSafe, traceSafe.getFirstPositionWith(_loc_1));
//
//    assertThat(result).isNotNull();
//    assertThat(result.getGlobalPredicates()).isNotEmpty();
//  }
//
//  @Test
//  public void testPathReversal() {
//    ARGPath traceError = mock(ARGPath.class);
//    when(traceError.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_x_gt_50,
//        _assume_p_eq_1
//        ));
//
//    List<ReversedEdge> result = refine.getReversedTrace(traceError);
//
//    assertThat(_assume_p_eq_1.getSuccessor()).isEqualTo(_assume_x_gt_50.getPredecessor());
//    assertThat(_assume_x_gt_50.getSuccessor()).isEqualTo(_loc_err);
//    assertThat(_assume_p_eq_1.getPredecessor()).isEqualTo(_loc_1);
//
//    assertThat(result).isNotNull();
//    assertThat(result.get(0).getPredecessor()).isEqualTo(_assume_p_eq_1.getSuccessor());
//    assertThat(result.get(0).getSuccessor()).isEqualTo(_assume_p_eq_1.getPredecessor());
//    assertThat(result.get(1).getPredecessor()).isEqualTo(result.get(0).getSuccessor());
//  }
//

}
