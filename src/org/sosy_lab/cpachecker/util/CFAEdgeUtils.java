// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;

/** A collection of utility methods for accessing data on {@link CFAEdge}s. */
public final class CFAEdgeUtils {

  public static final Type getLeftHandType(CFAEdge pEdge) {
    if (pEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
      if (declarationEdge.getDeclaration() instanceof AVariableDeclaration) {
        AVariableDeclaration variableDeclaration =
            (AVariableDeclaration) declarationEdge.getDeclaration();
        return variableDeclaration.getType();
      }
    } else {
      ALeftHandSide lhs = getLeftHandSide(pEdge);
      if (lhs instanceof AIdExpression) {
        return ((AIdExpression) lhs).getDeclaration().getType();
      }
    }
    return null;
  }

  public static final String getLeftHandVariable(CFAEdge pEdge) {
    if (pEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
      if (declarationEdge.getDeclaration() instanceof AVariableDeclaration) {
        AVariableDeclaration variableDeclaration =
            (AVariableDeclaration) declarationEdge.getDeclaration();
        return variableDeclaration.getQualifiedName();
      }
    } else {
      ALeftHandSide lhs = getLeftHandSide(pEdge);
      if (lhs instanceof AIdExpression) {
        return ((AIdExpression) lhs).getDeclaration().getQualifiedName();
      }
    }
    return null;
  }

  public static final ALeftHandSide getLeftHandSide(CFAEdge pEdge) {
    if (pEdge instanceof AStatementEdge) {
      AStatementEdge statementEdge = (AStatementEdge) pEdge;
      if (statementEdge.getStatement() instanceof AAssignment) {
        AAssignment assignment = (AAssignment) statementEdge.getStatement();
        return assignment.getLeftHandSide();
      }
    } else if (pEdge instanceof FunctionCallEdge) {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pEdge;
      AFunctionCall functionCall = functionCallEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement assignment =
            (AFunctionCallAssignmentStatement) functionCall;
        return assignment.getLeftHandSide();
      }
    }
    return null;
  }

  public static final CRightHandSide getRightHandSide(CFAEdge pEdge) {
    if (pEdge instanceof CDeclarationEdge) {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration) {
        CVariableDeclaration variableDeclaration =
            (CVariableDeclaration) declarationEdge.getDeclaration();
        CInitializer initializer = variableDeclaration.getInitializer();
        if (initializer instanceof CInitializerExpression) {
          return ((CInitializerExpression) initializer).getExpression();
        }
      }
    } else if (pEdge instanceof CStatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statementEdge.getStatement();
        return assignment.getRightHandSide();
      }
    } else if (pEdge instanceof CFunctionCallEdge) {
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pEdge;
      CFunctionCall functionCall = functionCallEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement assignment =
            (CFunctionCallAssignmentStatement) functionCall;
        return assignment.getRightHandSide();
      }
    }
    return null;
  }
}
