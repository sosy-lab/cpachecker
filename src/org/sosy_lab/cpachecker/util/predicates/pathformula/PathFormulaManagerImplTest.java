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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
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
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.NumeralFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.test.SolverBasedTest0;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Testing the custom SSA implementation.
 */
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class PathFormulaManagerImplTest extends SolverBasedTest0 {

  @SuppressWarnings("hiding")
  private FormulaManagerView fmgr;
  private PathFormulaManager pfmgrFwd;
  private PathFormulaManager pfmgrBwd;

  private CDeclarationEdge x_decl;

  @Before
  public void setup() throws Exception {
    Configuration configBackwards = Configuration.builder()
        .copyFrom(config)
        .setOption("cpa.predicate.handlePointerAliasing", "false") // not yet supported by the backwards analysis
        .build();

    fmgr = new FormulaManagerView(
        context.getFormulaManager(), config, LogManager.createTestLogManager());

    pfmgrFwd =
        new PathFormulaManagerImpl(
            fmgr,
            config,
            LogManager.createTestLogManager(),
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.FORWARD);

    pfmgrBwd =
        new PathFormulaManagerImpl(
            fmgr,
            configBackwards,
            LogManager.createTestLogManager(),
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.BACKWARD);
  }

  private Triple<CFAEdge, CFAEdge, MutableCFA> createCFA() throws UnrecognizedCCodeException {

    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(
        MachineModel.LINUX32, LogManager.createTestLogManager()
    );

    String fName = "main";
    SortedMap<String, FunctionEntryNode> functions = new TreeMap<>();

    FunctionEntryNode entryNode = dummyFunction(fName);

    // Edge 1: "x' = x + 1".
    // Edge 2: "x <= 10"
    CFANode a = new CFANode(fName);
    CFANode b = new CFANode(fName);

    CFAEdge init = new BlankEdge("", FileLocation.DUMMY, entryNode, a, "init");
    entryNode.addLeavingEdge(init);
    a.addEnteringEdge(init);

    // Declaration of the variable "X".
    // Equivalent to "int x = 0".
    CVariableDeclaration xDeclaration = new CVariableDeclaration(
        FileLocation.DUMMY,
        false,
        CStorageClass.AUTO,
        CNumericTypes.INT,
        "x",
        "x",
        "x",
        new CInitializerExpression(
            FileLocation.DUMMY,
            CIntegerLiteralExpression.ZERO
        )
    );

    x_decl = new CDeclarationEdge(
        "int x = 0",
        FileLocation.DUMMY,
        a,
        b,
        xDeclaration
        );

    // x + 1
    CExpression rhs = expressionBuilder.buildBinaryExpression(
        new CIdExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            "x",
            xDeclaration
        ),
        CIntegerLiteralExpression.ONE, // expression B.
        CBinaryExpression.BinaryOperator.PLUS
    );

    CFAEdge a_to_b = new CStatementEdge(
        "x := x + 1",

        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            new CIdExpression(
                FileLocation.DUMMY,
                CNumericTypes.INT,
                "x",
                xDeclaration
            ),
            rhs
        ),
        FileLocation.DUMMY,
        a,
        b
    );

    // x <= 10.
    CExpression guard = expressionBuilder.buildBinaryExpression(
        new CIdExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            "x",
            xDeclaration
        ),
        intConstant(BigInteger.TEN),
        CBinaryExpression.BinaryOperator.LESS_THAN
    );

    // OK and here we want a guard edge, right?..
    CFAEdge b_to_a = new CStatementEdge(
        "x <= 10"  ,
        new CExpressionStatement(
            FileLocation.DUMMY, guard
        ),
        FileLocation.DUMMY,
        b, a
    );

    SortedSetMultimap<String, CFANode> nodes = TreeMultimap.create();
    nodes.put("main", a);
    nodes.put("main", b);

    functions.put("main", entryNode);

    MutableCFA cfa = new MutableCFA(
        MachineModel.LINUX32,
        functions,
        nodes,
        entryNode,
        Language.C
    );
    return Triple.of(a_to_b, b_to_a, cfa);
  }

  private CExpression intConstant(BigInteger constant) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, CNumericTypes.INT, constant
    );
  }

  private FunctionEntryNode dummyFunction(String name) {
    CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.BOOL);

    FunctionEntryNode main = new CFunctionEntryNode(
        FileLocation.DUMMY,
        new CFunctionDeclaration(
            FileLocation.DUMMY, functionType, name,
            Collections.<CParameterDeclaration>emptyList()
        ),
        new FunctionExitNode(name),
        Optional.empty()
    );

    return main;
  }

  @Test
  public void testCustomSSAIdx() throws Exception {
    Triple<CFAEdge, CFAEdge, MutableCFA> data = createCFA();
    CFAEdge a_to_b = data.getFirst();

    int customIdx = 1337;
    PathFormula p = makePathFormulaWithCustomIdx(a_to_b, customIdx);

    // The SSA index should be incremented by one (= DEFAULT_INCREMENT) by the edge "x := x + 1".
    Assert.assertEquals(customIdx + FreshValueProvider.DEFAULT_INCREMENT, p.getSsa().getIndex("x"));
  }

  @Test
  public void testAssignmentSSABackward() throws Exception {
    Triple<CFAEdge, CFAEdge, MutableCFA> data = createCFA();
    CFAEdge a_to_b = data.getFirst();

    PathFormula pf = pfmgrBwd.makeEmptyPathFormula();
    pf = pfmgrBwd.makeNewPathFormula(pf,
        pf.getSsa().builder()
        .setIndex("x", CNumericTypes.INT, 10)
        .build());

    pf = pfmgrBwd.makeAnd(pf, a_to_b);

    Assert.assertEquals("(= x@10 (+ x@11 1))", pf.toString());
  }

  @Test
  public void testDeclarationSSABackward() throws Exception {
    createCFA();

    PathFormula pf = pfmgrBwd.makeEmptyPathFormula();
    pf = pfmgrBwd.makeNewPathFormula(pf,
        pf.getSsa().builder()
        .setIndex("x", CNumericTypes.INT, 10)
        .build());

    pf = pfmgrBwd.makeAnd(pf, x_decl);

    // The SSA index must be computed without gaps!!
    Assert.assertEquals(11, pf.getSsa().getIndex("x"));
  }

  @Test
  public void testDeclarationSSAForward() throws Exception {
    createCFA();

    PathFormula pf = pfmgrFwd.makeEmptyPathFormula();
    pf = pfmgrFwd.makeNewPathFormula(pf,
        pf.getSsa().builder()
        .setIndex("x", CNumericTypes.INT, 10)
        .build());

    pf = pfmgrFwd.makeAnd(pf, x_decl);

    Assert.assertEquals(11, pf.getSsa().getIndex("x"));
  }

  @Test
  public void testAssignmentSSAForward() throws Exception {
    Triple<CFAEdge, CFAEdge, MutableCFA> data = createCFA();
    CFAEdge a_to_b = data.getFirst();

    PathFormula pf = pfmgrFwd.makeEmptyPathFormula();
    pf = pfmgrFwd.makeNewPathFormula(pf,
        pf.getSsa().builder()
        .setIndex("x", CNumericTypes.INT, 10)
        .build());

    pf = pfmgrFwd.makeAnd(pf, a_to_b);

    Assert.assertEquals("(= x@11 (+ x@10 1))", pf.toString());
  }


  /**
   * Creates a {@link PathFormula} with SSA indexing starting
   * from the specified value.
   * Useful for more fine-grained control over SSA indexes.
   */
  private PathFormula makePathFormulaWithCustomIdx(CFAEdge edge, int ssaIdx)
      throws CPATransferException, InterruptedException {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula emptyWithCustomSSA = pfmgrFwd.makeNewPathFormula(
        empty,
        SSAMap.emptySSAMap().withDefault(ssaIdx));

    return pfmgrFwd.makeAnd(emptyWithCustomSSA, edge);
  }

  @Test
  public void testEmpty() {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula expected = new PathFormula(
        fmgr.getBooleanFormulaManager().makeTrue(),
        SSAMap.emptySSAMap(),
        PointerTargetSet.emptyPointerTargetSet(),
        0);
    assertEquals(expected, empty);
  }

  private PathFormula makePathFormulaWithVariable(String var, int index) {
    NumeralFormulaManagerView<NumeralFormula, RationalFormula> rfmgr =
        fmgr.getRationalFormulaManager();

    BooleanFormula f = rfmgr.equal(rfmgr.makeVariable(var, index), rfmgr.makeNumber(0));

    SSAMap s = SSAMap.emptySSAMap().builder().setIndex(var, CNumericTypes.DOUBLE, index).build();

    return new PathFormula(f, s, PointerTargetSet.emptyPointerTargetSet(), 1);
  }

  private BooleanFormula makeVariableEquality(String var, int index1, int index2) {
    NumeralFormulaManagerView<NumeralFormula, RationalFormula> rfmgr =
        fmgr.getRationalFormulaManager();

    return rfmgr.equal(rfmgr.makeVariable(var, index2), rfmgr.makeVariable(var, index1));
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

    PathFormula expected = new PathFormula(
        fmgr.makeOr(makeVariableEquality("a", 1, 2), pf.getFormula()),
        pf.getSsa(), pf.getPointerTargetSet(), 1);

    assertEquals(expected, result);
  }

  @Test
  public void testMakeOrRightEmpty() throws Exception {
    PathFormula empty = pfmgrFwd.makeEmptyPathFormula();
    PathFormula pf = makePathFormulaWithVariable("a", 2);

    PathFormula result = pfmgrFwd.makeOr(pf, empty);

    PathFormula expected = new PathFormula(
        fmgr.makeOr(pf.getFormula(), makeVariableEquality("a", 1, 2)),
        pf.getSsa(), pf.getPointerTargetSet(), 1);

    assertEquals(expected, result);
  }

  @Test
  public void testMakeOr() throws Exception {
    PathFormula pf1 = makePathFormulaWithVariable("a", 2);
    PathFormula pf2 = makePathFormulaWithVariable("a", 3);

    PathFormula result = pfmgrFwd.makeOr(pf1, pf2);

    BooleanFormula left = fmgr.makeAnd(pf1.getFormula(), makeVariableEquality("a", 2, 3));
    BooleanFormula right = pf2.getFormula();

    PathFormula expected = new PathFormula(fmgr.makeOr(left, right),
        pf2.getSsa(), PointerTargetSet.emptyPointerTargetSet(), 1);

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
}