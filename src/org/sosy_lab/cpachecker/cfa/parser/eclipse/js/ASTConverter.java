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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.logging.Level;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.core.dom.binding.FunctionBinding;
import org.eclipse.wst.jsdt.internal.core.dom.binding.VariableBinding;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSFunctionType;

class ASTConverter {

  private final Scope scope;
  private final LogManager logger;

  ASTConverter(final Scope pScope, final LogManager pLogger) {
    scope = pScope;
    logger = pLogger;
  }

  /**
   * Takes a ASTNode, and tries to get Information of its Placement in the Source Code. If it
   * doesnt't find such information, returns an empty FileLocation Object.
   *
   * @param pNode A Code piece wrapped in an ASTNode
   * @return FileLocation with Placement Information of the Code Piece, or null if such Information
   *     could not be obtained.
   */
  public FileLocation getFileLocation(final ASTNode pNode) {
    if (pNode == null) {
      return FileLocation.DUMMY;
    } else if (pNode.getRoot().getNodeType() != ASTNode.JAVASCRIPT_UNIT) {
      logger.log(Level.WARNING, "Can't find Placement Information for :" + pNode.toString());
      return FileLocation.DUMMY;
    }

    final JavaScriptUnit javaScriptUnit = (JavaScriptUnit) pNode.getRoot();

    return new FileLocation(
        scope.getFileName(),
        pNode.getStartPosition(),
        pNode.getLength(),
        javaScriptUnit.getLineNumber(pNode.getStartPosition()),
        javaScriptUnit.getLineNumber(pNode.getLength() + pNode.getStartPosition()));
  }

  // TODO should only be used for resolving variables (do not create another JSVariableDeclaration)
  private JSVariableDeclaration convert(
      final VariableDeclarationFragment pVariableDeclarationFragment) {
    final String variableIdentifier = pVariableDeclarationFragment.getName().getIdentifier();
    final Expression initializer = pVariableDeclarationFragment.getInitializer();
    final JSInitializerExpression initializerExpression =
        new JSInitializerExpression(getFileLocation(initializer), convert(initializer));
    return new JSVariableDeclaration(
        getFileLocation(pVariableDeclarationFragment),
        false,
        JSAnyType.ANY,
        variableIdentifier,
        variableIdentifier,
        variableIdentifier,
        initializerExpression);
  }

  public JSExpression convert(final Expression pExpression) {
    if (pExpression instanceof SimpleName) {
      return convert((SimpleName) pExpression);
    } else if (pExpression instanceof InfixExpression) {
      return convert((InfixExpression) pExpression);
    } else if (pExpression instanceof PrefixExpression) {
      return convert((PrefixExpression) pExpression);
    } else if (pExpression instanceof StringLiteral) {
      return convert((StringLiteral) pExpression);
    } else if (pExpression instanceof NumberLiteral) {
      return convert((NumberLiteral) pExpression);
    } else if (pExpression instanceof BooleanLiteral) {
      return convert((BooleanLiteral) pExpression);
    } else if (pExpression instanceof NullLiteral) {
      return convert((NullLiteral) pExpression);
    } else if (pExpression instanceof UndefinedLiteral) {
      return convert((UndefinedLiteral) pExpression);
    } else if (pExpression == null) {
      // This might be caused by a bug in the eclipse parser,
      // for example: https://bugs.eclipse.org/bugs/show_bug.cgi?id=518324
      throw new CFAGenerationRuntimeException("The expression to convert is null");
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of expression (not handled yet): " + pExpression.getClass().getSimpleName(),
        pExpression);
  }

  public JSStringLiteralExpression convert(final StringLiteral pStringLiteral) {
    return new JSStringLiteralExpression(
        getFileLocation(pStringLiteral), JSAnyType.ANY, pStringLiteral.getEscapedValue());
  }

  public JSBooleanLiteralExpression convert(final BooleanLiteral pBooleanLiteral) {
    return new JSBooleanLiteralExpression(
        getFileLocation(pBooleanLiteral), pBooleanLiteral.booleanValue());
  }

  public JSExpression convert(final NumberLiteral pNumberLiteral) {
    final String numberToken = pNumberLiteral.getToken();
    final FileLocation fileLocation = getFileLocation(pNumberLiteral);
    return numberToken.contains(".")
        ? new JSFloatLiteralExpression(fileLocation, new BigDecimal(numberToken))
        : new JSIntegerLiteralExpression(fileLocation, new BigInteger(numberToken));
  }

  public JSNullLiteralExpression convert(final NullLiteral pNullLiteral) {
    return new JSNullLiteralExpression(getFileLocation(pNullLiteral));
  }

  public JSUndefinedLiteralExpression convert(final UndefinedLiteral pUndefinedLiteral) {
    return new JSUndefinedLiteralExpression(getFileLocation(pUndefinedLiteral));
  }

  public JSUnaryExpression convert(final PrefixExpression pPrefixExpression) {
    return new JSUnaryExpression(
        getFileLocation(pPrefixExpression),
        JSAnyType.ANY,
        convert(pPrefixExpression.getOperand()),
        convert(pPrefixExpression.getOperator()));
  }

  private UnaryOperator convert(final PrefixExpression.Operator pOperator) {
    if (PrefixExpression.Operator.INCREMENT == pOperator) {
      return UnaryOperator.INCREMENT;
    } else if (PrefixExpression.Operator.DECREMENT == pOperator) {
      return UnaryOperator.DECREMENT;
    } else if (PrefixExpression.Operator.PLUS == pOperator) {
      return UnaryOperator.PLUS;
    } else if (PrefixExpression.Operator.MINUS == pOperator) {
      return UnaryOperator.MINUS;
    } else if (PrefixExpression.Operator.COMPLEMENT == pOperator) {
      return UnaryOperator.COMPLEMENT;
    } else if (PrefixExpression.Operator.NOT == pOperator) {
      return UnaryOperator.NOT;
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of unary operator (not handled yet): " + pOperator.toString());
  }

  public JSBinaryExpression convert(final InfixExpression pInfixExpression) {
    return new JSBinaryExpression(
        getFileLocation(pInfixExpression),
        JSAnyType.ANY,
        JSAnyType.ANY,
        convert(pInfixExpression.getLeftOperand()),
        convert(pInfixExpression.getRightOperand()),
        convert(pInfixExpression.getOperator()));
  }

  public BinaryOperator convert(final InfixExpression.Operator pOperator) {
    if (InfixExpression.Operator.AND == pOperator) {
      return BinaryOperator.AND;
    } else if (InfixExpression.Operator.CONDITIONAL_AND == pOperator) {
      return BinaryOperator.CONDITIONAL_AND;
    } else if (InfixExpression.Operator.CONDITIONAL_OR == pOperator) {
      return BinaryOperator.CONDITIONAL_OR;
    } else if (InfixExpression.Operator.DIVIDE == pOperator) {
      return BinaryOperator.DIVIDE;
    } else if (InfixExpression.Operator.EQUALS == pOperator) {
      return BinaryOperator.EQUALS;
    } else if (InfixExpression.Operator.EQUAL_EQUAL_EQUAL == pOperator) {
      return BinaryOperator.EQUAL_EQUAL_EQUAL;
    } else if (InfixExpression.Operator.GREATER == pOperator) {
      return BinaryOperator.GREATER;
    } else if (InfixExpression.Operator.GREATER_EQUALS == pOperator) {
      return BinaryOperator.GREATER_EQUALS;
    } else if (InfixExpression.Operator.IN == pOperator) {
      return BinaryOperator.IN;
    } else if (InfixExpression.Operator.INSTANCEOF == pOperator) {
      return BinaryOperator.INSTANCEOF;
    } else if (InfixExpression.Operator.LEFT_SHIFT == pOperator) {
      return BinaryOperator.LEFT_SHIFT;
    } else if (InfixExpression.Operator.LESS == pOperator) {
      return BinaryOperator.LESS;
    } else if (InfixExpression.Operator.LESS_EQUALS == pOperator) {
      return BinaryOperator.LESS_EQUALS;
    } else if (InfixExpression.Operator.MINUS == pOperator) {
      return BinaryOperator.MINUS;
    } else if (InfixExpression.Operator.NOT_EQUAL_EQUAL == pOperator) {
      return BinaryOperator.NOT_EQUAL_EQUAL;
    } else if (InfixExpression.Operator.NOT_EQUALS == pOperator) {
      return BinaryOperator.NOT_EQUALS;
    } else if (InfixExpression.Operator.OR == pOperator) {
      return BinaryOperator.OR;
    } else if (InfixExpression.Operator.PLUS == pOperator) {
      return BinaryOperator.PLUS;
    } else if (InfixExpression.Operator.REMAINDER == pOperator) {
      return BinaryOperator.REMAINDER;
    } else if (InfixExpression.Operator.RIGHT_SHIFT_SIGNED == pOperator) {
      return BinaryOperator.RIGHT_SHIFT_SIGNED;
    } else if (InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED == pOperator) {
      return BinaryOperator.RIGHT_SHIFT_UNSIGNED;
    } else if (InfixExpression.Operator.TIMES == pOperator) {
      return BinaryOperator.TIMES;
    } else if (InfixExpression.Operator.XOR == pOperator) {
      return BinaryOperator.XOR;
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of binary operator (not handled yet): " + pOperator.toString());
  }

  public JSIdExpression convert(final SimpleName pSimpleName) {
    final IBinding binding = pSimpleName.resolveBinding();
    assert binding != null;
    return new JSIdExpression(
        getFileLocation(pSimpleName), JSAnyType.ANY, pSimpleName.getIdentifier(), convert(binding));
  }

  public JSSimpleDeclaration convert(final IBinding pBinding) {
    if (pBinding instanceof VariableBinding) {
      return convert((VariableBinding) pBinding);
    } else if (pBinding instanceof FunctionBinding) {
      return convert((FunctionBinding) pBinding);
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of binding (not handled yet): " + pBinding.toString());
  }

  public JSVariableDeclaration convert(final VariableBinding pBinding) {
    return convert((VariableDeclarationFragment) pBinding.getDeclaration().getNode().getParent());
  }

  public JSFunctionDeclaration convert(final FunctionBinding pBinding) {
    return convert((FunctionDeclaration) pBinding.getDeclaration().getNode().getParent());
  }

  public JSFunctionDeclaration convert(final FunctionDeclaration pDeclaration) {
    return new JSFunctionDeclaration(
        getFileLocation(pDeclaration),
        new JSFunctionType(JSAnyType.ANY, Collections.emptyList()),
        getFunctionName(pDeclaration),
        Collections.emptyList());
  }

  public static String getFunctionName(final FunctionDeclaration node) {
    return ((SimpleName) node.getMethodName()).getIdentifier();
  }
}
