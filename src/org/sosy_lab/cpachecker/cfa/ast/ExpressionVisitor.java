/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;

public interface ExpressionVisitor<R, X extends Exception> {

  R visit(IASTArraySubscriptExpression pIastArraySubscriptExpression) throws X;

  R visit(IASTBinaryExpression pIastBinaryExpression) throws X;

  R visit(IASTCastExpression pIastCastExpression) throws X;

  R visit(IASTFieldReference pIastFieldReference) throws X;

  R visit(IASTIdExpression pIastIdExpression) throws X;

  R visit(IASTCharLiteralExpression pIastCharLiteralExpression) throws X;

  R visit(IASTFloatLiteralExpression pIastFloatLiteralExpression) throws X;

  R visit(IASTIntegerLiteralExpression pIastIntegerLiteralExpression) throws X;

  R visit(IASTStringLiteralExpression pIastStringLiteralExpression) throws X;

  R visit(IASTTypeIdExpression pIastTypeIdExpression) throws X;

  R visit(IASTUnaryExpression pIastUnaryExpression) throws X;

}
