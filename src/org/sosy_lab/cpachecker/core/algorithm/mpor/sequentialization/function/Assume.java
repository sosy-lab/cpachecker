// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Assume implements SeqFunction {

  private static final CFunctionCallExpression abortCall =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          SeqTypes.ABORT,
          SeqExpressions.ABORT,
          ImmutableList.of(),
          SeqDeclarations.ABORT);

  private final IfExpr ifCond;

  public Assume(CBinaryExpressionBuilder pBinExprBuilder) throws UnrecognizedCodeException {
    ifCond =
        new IfExpr(
            pBinExprBuilder.buildBinaryExpression(
                SeqExpressions.COND, SeqExpressions.INT_ZERO, BinaryOperator.EQUALS));
  }

  @Override
  public String toString() {
    return getSignature().toString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(ifCond.toString()))
        + SeqUtil.prependTabsWithNewline(2, abortCall.toASTString() + SeqSyntax.SEMICOLON)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public String getReturnType() {
    return SeqDataType.VOID;
  }

  @Override
  public String getName() {
    return SeqNameBuilder.createFuncName(SeqToken.ASSUME);
  }

  @Override
  public ImmutableList<SeqExpression> getParameters() {
    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    rParameters.add(new VariableExpr(Optional.of(SeqDataType.INT), new Variable(SeqToken.COND)));
    return rParameters.build();
  }
}
