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
package org.sosy_lab.cpachecker.core.algorithm.precondition;

import static org.mockito.Mockito.*;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.*;

import org.junit.Before;
import org.junit.Ignore;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@Ignore
@SuppressWarnings("unused")
public class PreconditionHelperTest extends SolverBasedTest0 {

  private PreconditionHelper helper;
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
    helper = new PreconditionHelper(mgrv, config, logger, ShutdownNotifier
        .create(), cfa);

    // Test CFA elements...
    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, TestLogManager.getInstance());
    CArrayType unlimitedIntArrayType = new CArrayType(false, false, CNumericTypes.INT, null);

    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _i_decl = makeDeclaration("i", CNumericTypes.INT, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _al_decl = makeDeclaration("al", CNumericTypes.INT, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _a = makeDeclaration("a", unlimitedIntArrayType, null);
    Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> _b = makeDeclaration("b", unlimitedIntArrayType, null);
    Triple<CDeclarationEdge, CFunctionDeclaration, CFunctionType> _funct_decl = makeFunctionDeclaration("copy", CVoidType.VOID,
        Lists.<CParameterDeclaration>newArrayList(
            new CParameterDeclaration(FileLocation.DUMMY, unlimitedIntArrayType, "a"),
            new CParameterDeclaration(FileLocation.DUMMY, unlimitedIntArrayType, "b")
            ));

    CIdExpression _i = _i_decl.getThird();
    CIdExpression _al = makeVariable("al", CNumericTypes.INT);

    CIntegerLiteralExpression _0 = CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT);
    CIntegerLiteralExpression _1 = CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT);

    CArraySubscriptExpression _b_at_i = new CArraySubscriptExpression(
        FileLocation.DUMMY,
        unlimitedIntArrayType,
        _b.getThird(),
        _i);

    _label_error = TestDataTools.makeBlankEdge("ERROR");
    _assume_i_geq_al = makeAssume(expressionBuilder.buildBinaryExpression(_i, _al, BinaryOperator.GREATER_EQUAL)).getFirst();
    _assume_b_at_i_neq_0 = makeAssume(expressionBuilder.buildBinaryExpression(_b_at_i, _0, BinaryOperator.NOT_EQUALS)).getFirst();
    _while = TestDataTools.makeBlankEdge("while");
    _stmt_i_assign_0 = makeAssignment(_i, _0).getFirst();
    _stmt_declare_i = _i_decl.getFirst();
    _dummy = makeBlankEdge("dummy");
    _function_declaration= _funct_decl.getFirst();
    _stmt_al_assign_0 = makeAssignment(_al, _0).getFirst();
  }

//  @Test
//  public void testGetPreconditionOfPath1() throws CPATransferException, SolverException, InterruptedException {
//    //  line 8: N9 -{Label: ERROR}-> N10
//    //  line 7: N6 -{[i >= al]}-> N9
//    //  line 6: N5 -{[(b[i]) != 0]}-> N6
//    //  lines 6-12: N4 -{while}-> N5
//    //  line 5: N3 -{i = 0;}-> N4
//    //  line 4: N2 -{int i;}-> N3
//    //  none: N19 -{Function start dummy edge}-> N2
//    //  lines 3-13: N18 -{void copy(int a[], int b[]);}-> N19
//    //  line 2: N17 -{int al = 0;}-> N18
//    //  line 1: N16 -{void __VERIFIER_error();}-> N17
//    //  none: N1 -{INIT GLOBAL VARS}-> N16
//
//    ARGPath pathMock = mock(ARGPath.class);
//    when(pathMock.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _label_error,
//        _assume_i_geq_al,
//        _assume_b_at_i_neq_0,
//        _while,
//        _stmt_i_assign_0,
//        _stmt_declare_i,
//        _dummy,
//        _function_declaration
//        ));
//
//    // (and (>= 0 al) (not (= (select b 0) 0)))
//    BooleanFormula expected_pc_at_entry = bmgr.and(
//        imgr.greaterOrEquals(
//            imgr.makeNumber(0),
//            imgr.makeVariable("al")),
//        bmgr.not(imgr.equal(
//            amgr.select(
//                amgr.makeArray("b", FormulaType.IntegerType, FormulaType.IntegerType),
//                imgr.makeNumber(0)),
//            imgr.makeNumber(0))));
//
//    BooleanFormula pc_at_entry = helper.getPreconditionOfPath(pathMock, Optional.<CFANode>absent());
//    assertThat(pc_at_entry.toString()).isEqualTo(expected_pc_at_entry.toString());
//
//    // (and (>= i al) (not (= (select b i) 0)))
//    BooleanFormula expected_pc_without_i_assign_0 = bmgr.and(
//        imgr.greaterOrEquals(
//            imgr.makeVariable("i"),
//            imgr.makeVariable("al")),
//        bmgr.not(imgr.equal(
//            amgr.select(
//                amgr.makeArray("b", FormulaType.IntegerType, FormulaType.IntegerType),
//                imgr.makeVariable("i")),
//            imgr.makeNumber(0))));
//
//    BooleanFormula pc_without_i_assign_0 = helper.getPreconditionOfPath(pathMock, Optional.of(_while.getSuccessor()));
//    assertThat(pc_without_i_assign_0.toString()).isEqualTo(expected_pc_without_i_assign_0.toString());
//  }
//
//  @Test
//  public void testGetPreconditionOfPath2() throws CPATransferException, SolverException, InterruptedException {
//    ARGPath pathMock = mock(ARGPath.class);
//    when(pathMock.asEdgesList()).thenReturn(Lists.<CFAEdge>newArrayList(
//        _assume_i_geq_al,
//        _stmt_i_assign_0,
//        _stmt_declare_i
//        ));
//
//    BooleanFormula pc = helper.getPreconditionOfPath(pathMock, Optional.<CFANode>absent());
//
//    assertThat(pc).isNotNull();
//    assertThat(pc.toString()).isEqualTo(
//              imgr.greaterOrEquals(imgr.makeNumber(0), imgr.makeVariable("al")
//            ).toString());
//  }


}
