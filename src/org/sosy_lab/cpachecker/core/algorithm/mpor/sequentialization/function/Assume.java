// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Assume implements SeqFunction {

  private static final CFunctionCallExpression abortCall =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          SeqVoidType.VOID,
          SeqIdExpression.ABORT,
          ImmutableList.of(),
          SeqFunctionDeclaration.ABORT);

  private final IfExpr ifCond;

  public Assume(CBinaryExpressionBuilder pBinExprBuilder) throws UnrecognizedCodeException {
    ifCond =
        new IfExpr(
            pBinExprBuilder.buildBinaryExpression(
                SeqIdExpression.COND, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS));
  }

  @Override
  public String toASTString() {
    return getDeclarationWithParameterNames()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(ifCond.toASTString()))
        + SeqUtil.prependTabsWithNewline(2, abortCall.toASTString() + SeqSyntax.SEMICOLON)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public CType getReturnType() {
    return SeqVoidType.VOID;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpression.ASSUME;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    ImmutableList.Builder<CParameterDeclaration> rParameters = ImmutableList.builder();
    rParameters.add(SeqParameterDeclaration.COND);
    return rParameters.build();
  }

  @Override
  public CFunctionDeclaration getDeclaration() {
    return SeqFunctionDeclaration.ASSUME;
  }
}
