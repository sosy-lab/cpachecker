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


public interface CExpressionVisitor<R, X extends Exception> extends CLeftHandSideVisitor<R, X> {

  R visit(CBinaryExpression pIastBinaryExpression) throws X;

  R visit(CCastExpression pIastCastExpression) throws X;

  R visit(CCharLiteralExpression pIastCharLiteralExpression) throws X;

  R visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws X;

  R visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws X;

  R visit(CStringLiteralExpression pIastStringLiteralExpression) throws X;

  R visit(CTypeIdExpression pIastTypeIdExpression) throws X;

  R visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression) throws X;

  R visit(CUnaryExpression pIastUnaryExpression) throws X;

  R visit (CImaginaryLiteralExpression PIastLiteralExpression) throws X;
}
