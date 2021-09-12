// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class ArrayAbstractionUtils {

  private ArrayAbstractionUtils() {}

  static CIdExpression createCIdExpression(CType pType, MemoryLocation pMemoryLocation) {

    String variableName = pMemoryLocation.getIdentifier();
    String qualifiedName = pMemoryLocation.getExtendedQualifiedName();

    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            pType,
            variableName,
            variableName,
            qualifiedName,
            null);

    return new CIdExpression(FileLocation.DUMMY, variableDeclaration);
  }

  static MemoryLocation getMemoryLocation(CIdExpression pCIdExpression) {

    String qualifiedName = pCIdExpression.getDeclaration().getQualifiedName();

    return MemoryLocation.fromQualifiedName(qualifiedName);
  }

  static CAssumeEdge createAssumeEdge(CExpression pCondition, boolean pTruthAssumption) {
    return new CAssumeEdge(
        "",
        FileLocation.DUMMY,
        CFANode.newDummyCFANode("dummy-predecessor"),
        CFANode.newDummyCFANode("dummy-successor"),
        pCondition,
        pTruthAssumption);
  }

  static CVariableDeclaration createVariableDeclarationWithType(
      CVariableDeclaration pOriginalVariableDeclaration, CType pNewType) {

    return new CVariableDeclaration(
        pOriginalVariableDeclaration.getFileLocation(),
        pOriginalVariableDeclaration.isGlobal(),
        pOriginalVariableDeclaration.getCStorageClass(),
        pNewType,
        pOriginalVariableDeclaration.getName(),
        pOriginalVariableDeclaration.getOrigName(),
        pOriginalVariableDeclaration.getQualifiedName(),
        null);
  }

  static CVariableDeclaration createNonArrayVariableDeclaration(
      CVariableDeclaration pArrayVariableDeclaration) {
    CType type = pArrayVariableDeclaration.getType();

    if (type instanceof CArrayType) {
      CType newType = ((CArrayType) type).getType();
      return createVariableDeclarationWithType(pArrayVariableDeclaration, newType);
    }

    return pArrayVariableDeclaration;
  }

  static CStatementEdge createAssignEdge(CLeftHandSide pLhs, CIdExpression pRhs) {

    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);

    return new CStatementEdge(
        "",
        assignmentStatement,
        FileLocation.DUMMY,
        CFANode.newDummyCFANode("dummy-predecessor"),
        CFANode.newDummyCFANode("dummy-successor"));
  }
}
