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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;


public interface CAstNodeVisitor<R, X extends Exception> {

  R visit(CArrayDesignator pNode) throws X;
  R visit(CArrayRangeDesignator pNode) throws X;
  R visit(CFieldDesignator pNode) throws X;
  R visit(CInitializerList pNode) throws X;
  R visit(CReturnStatement pNode) throws X;
  R visit(CDesignatedInitializer pNode) throws X;
  R visit(CInitializerExpression pNode) throws X;
  R visit(CFunctionCallExpression pNode) throws X;
  R visit(CBinaryExpression pNode) throws X;
  R visit(CCastExpression pNode) throws X;
  R visit(CTypeIdExpression pNode) throws X;
  R visit(CTypeIdInitializerExpression pNode) throws X;
  R visit(CUnaryExpression pNode) throws X;
  R visit(CArraySubscriptExpression pNode) throws X;
  R visit(CComplexCastExpression pNode) throws X;
  R visit(CFieldReference pNode) throws X;
  R visit(CIdExpression pNode) throws X;
  R visit(CPointerExpression pNode) throws X;
  R visit(CCharLiteralExpression pNode) throws X;
  R visit(CFloatLiteralExpression pNode) throws X;
  R visit(CImaginaryLiteralExpression pNode) throws X;
  R visit(CIntegerLiteralExpression pNode) throws X;
  R visit(CStringLiteralExpression pNode) throws X;
  R visit(CParameterDeclaration pNode) throws X;
  R visit(CFunctionDeclaration pNode) throws X;
  R visit(CComplexTypeDeclaration pNode) throws X;
  R visit(CTypeDefDeclaration pNode) throws X;
  R visit(CVariableDeclaration pNode) throws X;
  R visit(CExpressionAssignmentStatement pNode) throws X;
  R visit(CExpressionStatement pNode) throws X;
  R visit(CFunctionCallAssignmentStatement pNode) throws X;
  R visit(CFunctionCallStatement pNode) throws X;
  R visit(CEnumerator pCEnumerator);

}
