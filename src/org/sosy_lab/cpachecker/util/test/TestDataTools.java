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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.collect.ImmutableList;

public class TestDataTools {

  public static final CFANode DUMMY_CFA_NODE = new CFANode("DUMMY");

  public static final CInitializer INT_ZERO_INITIALIZER = new CInitializerExpression(
      FileLocation.DUMMY,
      CIntegerLiteralExpression.ZERO);

  public static Triple<CDeclarationEdge, CFunctionDeclaration, CFunctionType> makeFunctionDeclaration(
      String pFunctionName,
      CType pFunctionReturnType,
      List<CParameterDeclaration> pParameters) {

    CFunctionType functionType = new CFunctionType(
        false, false, checkNotNull(pFunctionReturnType), ImmutableList.<CType>of(), false);
    CFunctionDeclaration fd = new CFunctionDeclaration(
        FileLocation.DUMMY, functionType, pFunctionName, pParameters);
    CDeclarationEdge declEdge = new CDeclarationEdge(
        "", FileLocation.DUMMY, DUMMY_CFA_NODE, DUMMY_CFA_NODE, fd);

    return Triple.of(declEdge, fd, functionType);
  }

  public static Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> makeDeclaration(
      String varName, CType varType, @Nullable CInitializer initializer) {
    final FileLocation loc = FileLocation.DUMMY;
    final CVariableDeclaration decl = new CVariableDeclaration(
        loc, true, CStorageClass.AUTO, varType, varName, varName, varName, initializer);

    return Triple.of(
        new CDeclarationEdge(
            String.format("%s %s", "dummy", varName),
            FileLocation.DUMMY,
            DUMMY_CFA_NODE,
            DUMMY_CFA_NODE,
            decl),
        decl,
        new CIdExpression(
            FileLocation.DUMMY,
            decl));
  }

  public static CFAEdge makeBlankEdge(String pDescription) {
    return new BlankEdge("", FileLocation.DUMMY, DUMMY_CFA_NODE, DUMMY_CFA_NODE, pDescription);
  }

  public static Pair<CFAEdge, CExpressionAssignmentStatement> makeAssignment(CLeftHandSide pLhs, CExpression pRhs) {
    CExpressionAssignmentStatement stmt = new CExpressionAssignmentStatement(
        FileLocation.DUMMY,
        pLhs,
        pRhs);

    CFAEdge edge = new CStatementEdge(
        "dummy := rhs",
        stmt,
        FileLocation.DUMMY,
        DUMMY_CFA_NODE,
        DUMMY_CFA_NODE);

    return Pair.of(edge, stmt);
  }

  public static CIdExpression makeVariable(String varName, CSimpleType varType) {
    FileLocation loc = FileLocation.DUMMY;
    CVariableDeclaration decl = new CVariableDeclaration(
        loc, true, CStorageClass.AUTO, varType, varName, varName, varName, null);

    return new CIdExpression(loc, decl);
  }

  public static Pair<CAssumeEdge, CExpression> makeAssume(CExpression pAssumeExr) {
    CAssumeEdge assumeEdge = new CAssumeEdge(
        "dummyassume",
        FileLocation.DUMMY,
        DUMMY_CFA_NODE,
        DUMMY_CFA_NODE,
        pAssumeExr,
        true);

    return Pair.of(assumeEdge, pAssumeExr);
  }

}
