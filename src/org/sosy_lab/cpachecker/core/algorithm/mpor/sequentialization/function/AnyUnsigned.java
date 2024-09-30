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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ArrayExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IncrementExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ReturnExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqValue;

public class AnyUnsigned implements SeqFunction {

  private static final Variable array = new Variable(SeqToken.ARRAY);

  private static final Variable size = new Variable(SeqToken.SIZE);

  private static final Variable i = new Variable(SeqToken.I);

  private static final DeclareExpr declareIndex =
      new DeclareExpr(
          false,
          new VariableExpr(Optional.of(SeqDataType.INT), i),
          Optional.of(new Value(SeqValue.ZERO)));

  private static final LoopExpr loopIndexLessSize =
      new LoopExpr(new BooleanExpr(i, SeqOperator.LESS, size));

  private static final IfExpr ifElemGeqZero =
      new IfExpr(
          new BooleanExpr(
              new ArrayElement(array, i), SeqOperator.GREATER_OR_EQUAL, new Value(SeqValue.ZERO)));

  private static final ReturnExpr returnOne = new ReturnExpr(new Value(SeqValue.ONE));

  private static final IncrementExpr incrementI = new IncrementExpr(i);

  private static final ReturnExpr returnZero = new ReturnExpr(new Value(SeqValue.ZERO));

  @Override
  public String toString() {
    return getSignature().toString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, declareIndex.toString())
        + SeqUtil.prependTabsWithNewline(
            1, SeqUtil.appendOpeningCurly(loopIndexLessSize.toString()))
        + SeqUtil.prependTabsWithNewline(2, SeqUtil.appendOpeningCurly(ifElemGeqZero.toString()))
        + SeqUtil.prependTabsWithNewline(3, returnOne.toString())
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(2, incrementI.toString())
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, returnZero.toString())
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public String getReturnType() {
    return SeqDataType.INT;
  }

  @Override
  public String getName() {
    return SeqNameBuilder.createFuncName(SeqToken.ANY_UNSIGNED);
  }

  @Override
  public ImmutableList<SeqExpression> getParameters() {
    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    rParameters.add(
        new VariableExpr(Optional.of(SeqDataType.INT), new ArrayExpr(array, Optional.empty())));
    rParameters.add(new VariableExpr(Optional.of(SeqDataType.INT), size));
    return rParameters.build();
  }

  @Override
  public FunctionSignature getSignature() {
    return new FunctionSignature(getReturnType(), new FunctionCallExpr(getName(), getParameters()));
  }
}
