/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

public interface JSExpressionVisitor<R, X extends Exception> extends JSLeftHandSideVisitor<R, X> {

  R visit(JSBinaryExpression pBinaryExpression) throws X;

  R visit(JSStringLiteralExpression pStringLiteralExpression) throws X;

  R visit(JSFloatLiteralExpression pLiteral) throws X;

  R visit(JSUnaryExpression pUnaryExpression) throws X;

  R visit(JSIntegerLiteralExpression pIntegerLiteralExpression) throws X;

  R visit(JSBooleanLiteralExpression pBooleanLiteralExpression) throws X;

  R visit(JSNullLiteralExpression pNullLiteralExpression) throws X;

  R visit(JSUndefinedLiteralExpression pUndefinedLiteralExpression) throws X;

  R visit(JSThisExpression pThisExpression) throws X;

  default R visit(JSExpression pExpression) throws X {
    if (pExpression instanceof JSIdExpression) {
      return visit((JSIdExpression) pExpression);
    } else if (pExpression instanceof JSBinaryExpression) {
      return visit((JSBinaryExpression) pExpression);
    } else if (pExpression instanceof JSStringLiteralExpression) {
      return visit((JSStringLiteralExpression) pExpression);
    } else if (pExpression instanceof JSFloatLiteralExpression) {
      return visit((JSFloatLiteralExpression) pExpression);
    } else if (pExpression instanceof JSUnaryExpression) {
      return visit((JSUnaryExpression) pExpression);
    } else if (pExpression instanceof JSIntegerLiteralExpression) {
      return visit((JSIntegerLiteralExpression) pExpression);
    } else if (pExpression instanceof JSBooleanLiteralExpression) {
      return visit((JSBooleanLiteralExpression) pExpression);
    } else if (pExpression instanceof JSNullLiteralExpression) {
      return visit((JSNullLiteralExpression) pExpression);
    } else if (pExpression instanceof JSUndefinedLiteralExpression) {
      return visit((JSUndefinedLiteralExpression) pExpression);
    } else if (pExpression instanceof JSThisExpression) {
      return visit((JSThisExpression) pExpression);
    } else {
      throw new RuntimeException("Not implemented yet");
    }
  }
}
