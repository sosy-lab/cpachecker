// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

/** Testing the custom SSA implementation. */
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@RunWith(Parameterized.class)
public class PathFormulaManagerImplTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverUnderTest;

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  private PathFormulaManager pfmgrFwd;
  private PathFormulaManager pfmgrBwd;

  private CDeclarationEdge x_decl;
  private CStatementEdge assignment;

  private static final CType variableType = CNumericTypes.INT;
  private static final FormulaType<?> formulaType = FormulaType.getBitvectorTypeWithSize(32);

  @Before
  public void setup() throws Exception {
    Configuration configBackwards =
        Configuration.builder()
            .copyFrom(config)
            .setOption(
                "cpa.predicate.handlePointerAliasing",
                "false") // not yet supported by the backwards analysis
            .build();

    pfmgrFwd =
        new PathFormulaManagerImpl(
            mgrv,
            config,
            logger,
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.FORWARD);

    pfmgrBwd =
        new PathFormulaManagerImpl(
            mgrv,
            configBackwards,
            logger,
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.BACKWARD);

    createEdges();
  }

  /** Create edges for declaring <code>x</code> and for <code>x = x + 1</code>. */
  private void createEdges() throws UnrecognizedCodeException {
    CBinaryExpressionBuilder expressionBuilder =
        new CBinaryExpressionBuilder(MachineModel.LINUX32, LogManager.createTestLogManager());

    String fName = "main";
    CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.BOOL);
    CFunctionDeclaration fdef =
        new CFunctionDeclaration(
            FileLocation.DUMMY, functionType, fName, ImmutableList.of(), ImmutableSet.of());
    FunctionEntryNode entryNode =
        new CFunctionEntryNode(
            FileLocation.DUMMY, fdef, new FunctionExitNode(fdef), Optional.empty());

    CFANode a = new CFANode(fdef);
    CFANode b = new CFANode(fdef);

    CFAEdge init = new BlankEdge("", FileLocation.DUMMY, entryNode, a, "init");
    entryNode.addLeavingEdge(init);
    a.addEnteringEdge(init);

    // Declaration of the variable "X".
    // Equivalent to "int x = 0".
    CVariableDeclaration xDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            CNumericTypes.INT,
            "x",
            "x",
            "x",
            new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));

    x_decl = new CDeclarationEdge("int x = 0", FileLocation.DUMMY, a, b, xDeclaration);

    // x + 1
    CExpression rhs =
        expressionBuilder.buildBinaryExpression(
            new CIdExpression(FileLocation.DUMMY, CNumericTypes.INT, "x", xDeclaration),
            CIntegerLiteralExpression.ONE, // expression B.
            CBinaryExpression.BinaryOperator.PLUS);

    assignment =
        new CStatementEdge(
            "x := x + 1",
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY,
                new CIdExpression(FileLocation.DUMMY, CNumericTypes.INT, "x", xDeclaration),
                rhs),
            FileLocation.DUMMY,
            a,
            b);
  }

  @Test
  public void testCustomSSAIdx() throws Exception {
    int customIdx = 1337;
    SSAMap ssaMap = SSAMap.emptySSAMap().withDefault(customIdx);
    PathFormula emptyWithCustomSSA =
        pfmgrFwd.makeEmptyPathFormulaWithContext(ssaMap, PointerTargetSet.emptyPointerTargetSet());
    PathFormula p = pfmgrFwd.makeAnd(emptyWithCustomSSA, assignment);

    // The SSA index should be incremented by one (= DEFAULT_INCREMENT) by the edge "x := x + 1".
    assertThat(p.getSsa().getIndex("x"))
        .isEqualTo(customIdx + FreshValueProvider.DEFAULT_INCREMENT);
  }

  @Test
  public void testAssignmentSSABackward() throws Exception {
    PathFormula pf = makePathFormulaWithCustomIndex(pfmgrBwd, "x", CNumericTypes.INT, 10);

    pf = pfmgrBwd.makeAnd(pf, assignment);

    BooleanFormula expected =
        mgrv.makeEqual(
            mgrv.makeVariable(formulaType, "x", 10),
            mgrv.makePlus(
                mgrv.makeVariable(formulaType, "x", 11), mgrv.makeNumber(formulaType, 1)));
    assertThatFormula(pf.getFormula()).isEquivalentTo(expected);
  }

  @Test
  public void testDeclarationSSABackward() throws Exception {
    PathFormula pf = makePathFormulaWithCustomIndex(pfmgrBwd, "x", CNumericTypes.INT, 10);

    pf = pfmgrBwd.makeAnd(pf, x_decl);

    // The SSA index must be computed without gaps!!
    assertThat(pf.getSsa().getIndex("x")).isEqualTo(11);
  }

  @Test
  public void testDeclarationSSAForward() throws Exception {
    PathFormula pf = makePathFormulaWithCustomIndex(pfmgrFwd, "x", CNumericTypes.INT, 10);

    pf = pfmgrFwd.makeAnd(pf, x_decl);

    assertThat(pf.getSsa().getIndex("x")).isEqualTo(11);
  }

  @Test
  public void testAssignmentSSAForward() throws Exception {
    PathFormula pf = makePathFormulaWithCustomIndex(pfmgrFwd, "x", CNumericTypes.INT, 10);

    pf = pfmgrFwd.makeAnd(pf, assignment);

    BooleanFormula expected =
        mgrv.makeEqual(
            mgrv.makeVariable(formulaType, "x", 11),
            mgrv.makePlus(
                mgrv.makeVariable(formulaType, "x", 10), mgrv.makeNumber(formulaType, 1)));
    assertThatFormula(pf.getFormula()).isEquivalentTo(expected);
  }

  private PathFormula makePathFormulaWithCustomIndex(
      PathFormulaManager pPfmgr, String pVar, CType pType, int pIndex) {
    SSAMap ssaMap = SSAMap.emptySSAMap().builder().setIndex(pVar, pType, pIndex).build();
    return pPfmgr.makeEmptyPathFormulaWithContext(ssaMap, PointerTargetSet.emptyPointerTargetSet());
  }

  @Test
  public void testEmpty() throws SolverException, InterruptedException {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula expected =
        new PathFormula(
            mgrv.getBooleanFormulaManager().makeTrue(),
            SSAMap.emptySSAMap(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    assertEquals(expected, empty);
  }

  private PathFormula makePathFormulaWithVariable(String var, int index) {
    BooleanFormula f =
        mgrv.makeEqual(mgrv.makeVariable(formulaType, var, index), mgrv.makeNumber(formulaType, 0));

    SSAMap s = SSAMap.emptySSAMap().builder().setIndex(var, variableType, index).build();

    return new PathFormula(f, s, PointerTargetSet.emptyPointerTargetSet(), 1);
  }

  private BooleanFormula makeVariableEquality(String var, int index1, int index2) {

    return mgrv.makeEqual(
        mgrv.makeVariable(formulaType, var, index2), mgrv.makeVariable(formulaType, var, index1));
  }

  // The following tests test the disjunction of the Formulas
  // as well as the merge of the SSAMaps within makeOr().

  @Test
  public void testMakeOrBothEmpty() throws Exception {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula result = pfmgrFwd.makeOr(empty, empty);

    assertEquals(empty, result);
  }

  @Test
  public void testMakeOrLeftEmpty() throws Exception {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula pf = makePathFormulaWithVariable("a", 2);

    PathFormula result = pfmgrFwd.makeOr(empty, pf);

    PathFormula expected =
        new PathFormula(
            mgrv.makeOr(makeVariableEquality("a", 1, 2), pf.getFormula()),
            pf.getSsa(),
            pf.getPointerTargetSet(),
            1);

    assertEquals(expected, result);
  }

  @Test
  public void testMakeOrRightEmpty() throws Exception {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula pf = makePathFormulaWithVariable("a", 2);

    PathFormula result = pfmgrFwd.makeOr(pf, empty);

    PathFormula expected =
        new PathFormula(
            mgrv.makeOr(pf.getFormula(), makeVariableEquality("a", 1, 2)),
            pf.getSsa(),
            pf.getPointerTargetSet(),
            1);

    assertEquals(expected, result);
  }

  @Test
  public void testMakeOr() throws Exception {
    PathFormula pf1 = makePathFormulaWithVariable("a", 2);
    PathFormula pf2 = makePathFormulaWithVariable("a", 3);

    PathFormula result = pfmgrFwd.makeOr(pf1, pf2);

    BooleanFormula left = mgrv.makeAnd(pf1.getFormula(), makeVariableEquality("a", 2, 3));
    BooleanFormula right = pf2.getFormula();

    PathFormula expected =
        new PathFormula(
            mgrv.makeOr(left, right), pf2.getSsa(), PointerTargetSet.emptyPointerTargetSet(), 1);

    assertEquals(expected, result);
  }

  @Test
  public void testMakeOrCommutative() throws Exception {
    PathFormula pf1 = makePathFormulaWithVariable("a", 2);
    PathFormula pf2 = makePathFormulaWithVariable("b", 3);

    PathFormula resultA = pfmgrFwd.makeOr(pf1, pf2);
    PathFormula resultB = pfmgrFwd.makeOr(pf2, pf1);

    assertThatFormula(resultA.getFormula()).isEquivalentTo(resultB.getFormula());
  }

  private void assertEquals(PathFormula expected, PathFormula result)
      throws SolverException, InterruptedException {
    assertThatFormula(result.getFormula()).isEquivalentTo(expected.getFormula());
    assertThat(result.getLength()).isEqualTo(expected.getLength());
    assertThat(result.getSsa()).isEqualTo(result.getSsa());
    assertThat(result.getPointerTargetSet()).isEqualTo(expected.getPointerTargetSet());
  }
}
