// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.string.utils.JVariableIdentifier;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class JVariableVisitor implements //JExpressionVisitor<JVariableIdentifier, NoException>,
//    JAstNodeVisitor<JVariableIdentifier, NoException>// ,
 JLeftHandSideVisitor<JVariableIdentifier, NoException>
{

  private JType type;

  @Override
  public JVariableIdentifier visit(JArraySubscriptExpression pE)
      throws NoException {
    // TODO Auto-generated method stub
    type = pE.getExpressionType();
    return null;
  }

  @Override
  public JVariableIdentifier visit(JIdExpression pE) throws NoException {
    JSimpleDeclaration decl =pE.getDeclaration();
    return new JVariableIdentifier(decl.getType(),MemoryLocation.forDeclaration(decl));
  }

//  @Override
//  public JVariableIdentifier visit(JCharLiteralExpression pPaCharLiteralExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JStringLiteralExpression pPaStringLiteralExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JBinaryExpression pPaBinaryExpression) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JUnaryExpression pAUnaryExpression) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JFloatLiteralExpression pJFloatLiteralExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JArrayCreationExpression pJArrayCreationExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JArrayInitializer pJArrayInitializer) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JArrayLengthExpression pJArrayLengthExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JVariableRunTimeType pJThisRunTimeType) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JNullLiteralExpression pJNullLiteralExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JEnumConstantExpression pJEnumConstantExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JCastExpression pJCastExpression) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JThisExpression pThisExpression) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JClassLiteralExpression pJClassLiteralExpression)
//      throws NoException {
//    type = pJClassLiteralExpression.getExpressionType();
//    if (HelperMethods.isString(type)) {
//      // return new JVariableIdentifier(type, pJClassLiteralExpression.toASTString(), true);
//      return null;
//    }
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JMethodInvocationExpression pAFunctionCallExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JClassInstanceCreation pJClassInstanceCreation)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JExpressionAssignmentStatement pAExpressionAssignmentStatement)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JExpressionStatement pAExpressionStatement) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier
//      visit(JMethodInvocationAssignmentStatement pAFunctionCallAssignmentStatement)
//          throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JMethodInvocationStatement pAFunctionCallStatement)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JInitializerExpression pJInitializerExpression)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JMethodDeclaration pJMethodDeclaration) throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JParameterDeclaration pJParameterDeclaration)
//      throws NoException {
//    // TODO Auto-generated method stub
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JReturnStatement pJReturnStatement) throws NoException {
//
//    return null;
//  }
//
//  @Override
//  public JVariableIdentifier visit(JVariableDeclaration pJVar) throws NoException {
//    return new JVariableIdentifier(pJVar.getType(), MemoryLocation.forDeclaration(pJVar));
//  }

}
