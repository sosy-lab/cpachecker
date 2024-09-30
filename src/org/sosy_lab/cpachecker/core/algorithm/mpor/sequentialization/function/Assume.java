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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.NegationExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class Assume implements SeqFunction {

  private static final Variable cond = new Variable(SeqToken.COND);

  private static final IfExpr ifCond = new IfExpr(new NegationExpr(cond));

  private static final FunctionCallExpr abortCall =
      new FunctionCallExpr(SeqToken.ABORT, ImmutableList.of());

  @Override
  public String toString() {
    return getSignature().toString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(ifCond.toString()))
        + SeqUtil.prependTabsWithNewline(2, abortCall.toString() + SeqSyntax.SEMICOLON)
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

  @Override
  public FunctionSignature getSignature() {
    return new FunctionSignature(getReturnType(), new FunctionCallExpr(getName(), getParameters()));
  }
}
