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
import java.util.logging.Level;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

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

  public JSVariableDeclaration convert(
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
    if (pExpression instanceof StringLiteral) {
      return convert((StringLiteral) pExpression);
    } else if (pExpression instanceof NumberLiteral) {
      return convert((NumberLiteral) pExpression);
    } else if (pExpression instanceof BooleanLiteral) {
      return convert((BooleanLiteral) pExpression);
    } else if (pExpression instanceof NullLiteral) {
      return convert((NullLiteral) pExpression);
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of expression (not handled yet).", pExpression);
  }

  public JSStringLiteralExpression convert(final StringLiteral pStringLiteral) {
    return new JSStringLiteralExpression(
        getFileLocation(pStringLiteral),
        JSAnyType.ANY,
        pStringLiteral.getEscapedValue());
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

}
