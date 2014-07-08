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

import java.math.BigInteger;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Testing the custom SSA implementation.
 */
public class PathFormulaManagerImplTest {

  public Triple<CFAEdge, CFAEdge, MutableCFA> createCFA() {

    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(
        MachineModel.LINUX32, TestLogManager.getInstance()
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
    CSimpleDeclaration xDeclaration = new CVariableDeclaration(
        FileLocation.DUMMY,
        false,
        CStorageClass.AUTO,
        intType(),
        "x",
        "x",
        "x",
        new CInitializerExpression(
            FileLocation.DUMMY,
            intConstant(BigInteger.ZERO)
        )
    );

    // x + 1
    CExpression rhs = expressionBuilder.buildBinaryExpression(
        new CIdExpression(
            FileLocation.DUMMY,
            intType(),
            "x",
            xDeclaration
        ),
        intConstant(BigInteger.ONE), // expression B.
        CBinaryExpression.BinaryOperator.PLUS
    );

    CFAEdge a_to_b = new CStatementEdge(
        "x := x + 1",

        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            new CIdExpression(
                FileLocation.DUMMY,
                intType(),
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
            intType(),
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

  CExpression intConstant(BigInteger constant) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, intType(), constant
    );
  }

  FunctionEntryNode dummyFunction(String name) {
    CFunctionType functionType = new CFunctionType(false, false, boolType(),
        Collections.<CType>emptyList(), false);

    FunctionEntryNode main = new FunctionEntryNode(
        FileLocation.DUMMY,
        name,
        new FunctionExitNode(name),
        new CFunctionDeclaration(
            FileLocation.DUMMY, functionType, name,
            Collections.<CParameterDeclaration>emptyList()
        ),
        Collections.<String>emptyList()
    );

    return main;
  }

  private CType boolType() {
    return new CSimpleType(
        false, false, CBasicType.BOOL,
        false, false, false, false,
        false, false, false);
  }

  private CType intType() {
    // NOTE: is it OK for all of them to be false?
    // anything else which needs to be done?
    return new CSimpleType(
        false, false, CBasicType.INT,
        false, false, false, false,
        false, false, false);
  }

  PathFormulaManager getPathFormulaManager(CFA cfa) throws InvalidConfigurationException {
    FormulaManagerFactory formulaManagerFactory = new FormulaManagerFactory(
        Configuration.defaultConfiguration(),
        TestLogManager.getInstance(), ShutdownNotifier.create());

    FormulaManager formulaManager = formulaManagerFactory.getFormulaManager();
    FormulaManagerView formulaManagerView = new FormulaManagerView(formulaManager,
        Configuration.defaultConfiguration(), TestLogManager.getInstance());

    return new PathFormulaManagerImpl(
        formulaManagerView,
        Configuration.defaultConfiguration(),
        TestLogManager.getInstance(),
        ShutdownNotifier.create(),
        cfa
    );
  }

  @Test
  public void testCustomSSAIdx() throws Exception {
    Triple<CFAEdge, CFAEdge, MutableCFA> data = createCFA();
    CFA cfa = data.getThird();
    CFAEdge a_to_b = data.getFirst();

    PathFormulaManager pathFormulaManager = getPathFormulaManager(cfa);

    int customIdx = 1337;
    PathFormula p = makePathFormulaWithCustomIdx(
        a_to_b, customIdx, pathFormulaManager);

    // The SSA index should be incremented by one by the edge "x := x + 1".
    Assert.assertEquals(customIdx + 1, p.getSsa().getIndex("x"));
  }

  /**
   * Creates a {@link PathFormula} with SSA indexing starting
   * from the specified value.
   * Useful for more fine-grained control over SSA indexes.
   */
  private static PathFormula makePathFormulaWithCustomIdx(CFAEdge edge, int ssaIdx,
      PathFormulaManager pathFormulaManager) throws CPATransferException, InterruptedException {
    PathFormula empty = pathFormulaManager.makeEmptyPathFormula();
    PathFormula emptyWithCustomSSA = pathFormulaManager.makeNewPathFormula(
        empty,
        SSAMap.emptySSAMap().withDefault(ssaIdx));

    return pathFormulaManager.makeAnd(emptyWithCustomSSA, edge);
  }
}
