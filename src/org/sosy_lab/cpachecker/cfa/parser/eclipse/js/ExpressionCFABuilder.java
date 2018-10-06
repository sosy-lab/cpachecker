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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteral;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;

class ExpressionCFABuilder implements ExpressionAppendable {

  private AssignmentAppendable assignmentAppendable;
  private BooleanLiteralConverter booleanLiteralConverter;
  private ConditionalExpressionAppendable conditionalExpressionAppendable;
  private FieldAccessAppendable fieldAccessAppendable;
  private FunctionExpressionAppendable functionExpressionAppendable;
  private FunctionInvocationAppendable functionInvocationAppendable;
  private InfixExpressionAppendable infixExpressionAppendable;
  private NullLiteralConverter nullLiteralConverter;
  private NumberLiteralConverter numberLiteralConverter;
  private ObjectLiteralAppendable objectLiteralAppendable;
  private ParenthesizedExpressionAppendable parenthesizedExpressionAppendable;
  private PrefixExpressionAppendable prefixExpressionAppendable;
  private PostfixExpressionAppendable postfixExpressionAppendable;
  private SimpleNameResolver simpleNameResolver;
  private StringLiteralConverter stringLiteralConverter;
  private UndefinedLiteralConverter undefinedLiteralConverter;
  private VariableDeclarationExpressionAppendable variableDeclarationExpressionAppendable;

  void setAssignmentAppendable(final AssignmentAppendable pAssignmentAppendable) {
    assignmentAppendable = pAssignmentAppendable;
  }

  void setBooleanLiteralConverter(final BooleanLiteralConverter pBooleanLiteralConverter) {
    booleanLiteralConverter = pBooleanLiteralConverter;
  }

  void setConditionalExpressionAppendable(
      final ConditionalExpressionAppendable pConditionalExpressionAppendable) {
    conditionalExpressionAppendable = pConditionalExpressionAppendable;
  }

  void setFieldAccessAppendable(final FieldAccessAppendable pFieldAccessAppendable) {
    fieldAccessAppendable = pFieldAccessAppendable;
  }

  void setFunctionExpressionAppendable(
      final FunctionExpressionAppendable pFunctionExpressionAppendable) {
    functionExpressionAppendable = pFunctionExpressionAppendable;
  }

  void setFunctionInvocationAppendable(
      final FunctionInvocationAppendable pFunctionInvocationAppendable) {
    functionInvocationAppendable = pFunctionInvocationAppendable;
  }

  void setInfixExpressionAppendable(
      final InfixExpressionAppendable pInfixExpressionAppendable) {
    infixExpressionAppendable = pInfixExpressionAppendable;
  }

  void setNullLiteralConverter(final NullLiteralConverter pNullLiteralConverter) {
    nullLiteralConverter = pNullLiteralConverter;
  }

  void setNumberLiteralConverter(final NumberLiteralConverter pNumberLiteralConverter) {
    numberLiteralConverter = pNumberLiteralConverter;
  }

  void setObjectLiteralAppendable(final ObjectLiteralAppendable pObjectLiteralAppendable) {
    objectLiteralAppendable = pObjectLiteralAppendable;
  }

  void setParenthesizedExpressionAppendable(
      final ParenthesizedExpressionAppendable pParenthesizedExpressionAppendable) {
    parenthesizedExpressionAppendable = pParenthesizedExpressionAppendable;
  }

  void setPrefixExpressionAppendable(
      final PrefixExpressionAppendable pPrefixExpressionAppendable) {
    prefixExpressionAppendable = pPrefixExpressionAppendable;
  }

  void setPostfixExpressionAppendable(
      final PostfixExpressionAppendable pPostfixExpressionAppendable) {
    postfixExpressionAppendable = pPostfixExpressionAppendable;
  }

  void setSimpleNameResolver(final SimpleNameResolver pSimpleNameResolver) {
    simpleNameResolver = pSimpleNameResolver;
  }

  void setStringLiteralConverter(final StringLiteralConverter pStringLiteralConverter) {
    stringLiteralConverter = pStringLiteralConverter;
  }

  void setUndefinedLiteralConverter(final UndefinedLiteralConverter pUndefinedLiteralConverter) {
    undefinedLiteralConverter = pUndefinedLiteralConverter;
  }

  void setVariableDeclarationExpressionAppendable(final VariableDeclarationExpressionAppendable pVariableDeclarationExpressionAppendable) {
    variableDeclarationExpressionAppendable = pVariableDeclarationExpressionAppendable;
  }

  @Override
  public JSExpression append(final JavaScriptCFABuilder pBuilder, final Expression pExpression) {
    if (pExpression instanceof Assignment) {
      return assignmentAppendable.append(pBuilder, (Assignment) pExpression);
    } else if (pExpression instanceof ConditionalExpression) {
      return conditionalExpressionAppendable.append(pBuilder, (ConditionalExpression) pExpression);
    } else if (pExpression instanceof FieldAccess) {
      return fieldAccessAppendable.append(pBuilder, (FieldAccess) pExpression);
    } else if (pExpression instanceof FunctionExpression) {
      return functionExpressionAppendable.append(pBuilder, (FunctionExpression) pExpression);
    } else if (pExpression instanceof FunctionInvocation) {
      return functionInvocationAppendable.append(pBuilder, (FunctionInvocation) pExpression);
    } else if (pExpression instanceof InfixExpression) {
      return infixExpressionAppendable.append(pBuilder, (InfixExpression) pExpression);
    } else if (pExpression instanceof NullLiteral) {
      return nullLiteralConverter.convert(pBuilder, (NullLiteral) pExpression);
    } else if (pExpression instanceof NumberLiteral) {
      return numberLiteralConverter.convert(pBuilder, (NumberLiteral) pExpression);
    } else if (pExpression instanceof ObjectLiteral) {
      return objectLiteralAppendable.append(pBuilder, (ObjectLiteral) pExpression);
    } else if (pExpression instanceof ParenthesizedExpression) {
      return parenthesizedExpressionAppendable.append(
          pBuilder, (ParenthesizedExpression) pExpression);
    } else if (pExpression instanceof PostfixExpression) {
      return postfixExpressionAppendable.append(pBuilder, (PostfixExpression) pExpression);
    } else if (pExpression instanceof PrefixExpression) {
      return prefixExpressionAppendable.append(pBuilder, (PrefixExpression) pExpression);
    } else if (pExpression instanceof SimpleName) {
      // undefined is writable in ES3, but not writable in ES5, see:
      // https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/undefined#Description
      // The used parser of Eclipse JSDT 3.9 does only support ES3.
      // Thereby, it creates a SimpleName for undefined instead of an UndefinedLiteral.
      // We like to be conformant to ES5.
      // That's why we convert it to an JSUndefinedLiteralExpression if it is used as an Expression.
      final SimpleName simpleName = (SimpleName) pExpression;
      return simpleName.getIdentifier().equals("undefined")
          ? new JSUndefinedLiteralExpression(pBuilder.getFileLocation(simpleName))
          : simpleNameResolver.resolve(pBuilder, (SimpleName) pExpression);
    } else if (pExpression instanceof StringLiteral) {
      return stringLiteralConverter.convert(pBuilder, (StringLiteral) pExpression);
    } else if (pExpression instanceof BooleanLiteral) {
      return booleanLiteralConverter.convert(pBuilder, (BooleanLiteral) pExpression);
    } else if (pExpression instanceof UndefinedLiteral) {
      return undefinedLiteralConverter.convert(pBuilder, (UndefinedLiteral) pExpression);
    } else if (pExpression instanceof VariableDeclarationExpression) {
      variableDeclarationExpressionAppendable.append(pBuilder,
          (VariableDeclarationExpression) pExpression);
      return null;
    } else if (pExpression == null) {
      // This might be caused by a bug in the eclipse parser,
      // for example: https://bugs.eclipse.org/bugs/show_bug.cgi?id=518324
      throw new CFAGenerationRuntimeException(
          "The expression to convert is null. This might be "
              + "caused by explicitly assigning undefined in a variable declaration.");
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of expression (not handled yet): " + pExpression.getClass().getSimpleName(),
        pExpression);
  }
}
