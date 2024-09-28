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

public class AnyNonNegative implements SeqFunction {

  private static final Variable array = new Variable(SeqToken.ARRAY);

  private static final Variable size = new Variable(SeqToken.SIZE);

  private static final Variable index = new Variable(SeqToken.INDEX);

  private static final DeclareExpr declareIndex =
      new DeclareExpr(
          new VariableExpr(Optional.of(SeqDataType.INT), index),
          Optional.of(new Value(SeqValue.ZERO)));

  private static final LoopExpr loopIndexLessSize =
      new LoopExpr(new BooleanExpr(index, SeqOperator.LESS, size));

  private static final IfExpr ifElemGeqZero =
      new IfExpr(
          new BooleanExpr(
              new ArrayElement(array, index),
              SeqOperator.GREATER_OR_EQUAL,
              new Value(SeqValue.ZERO)));

  private static final ReturnExpr returnTrue = new ReturnExpr(new Value(SeqValue.TRUE));

  private static final IncrementExpr incrementIndex = new IncrementExpr(index);

  private static final ReturnExpr returnFalse = new ReturnExpr(new Value(SeqValue.FALSE));

  @Override
  public String createString() {
    return getSignature().createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + declareIndex.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + loopIndexLessSize.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.TAB
        + ifElemGeqZero.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.TAB
        + SeqSyntax.TAB
        + returnTrue.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.TAB
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.TAB
        + incrementIndex.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + returnFalse.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public String getReturnType() {
    return SeqDataType.BOOL;
  }

  @Override
  public String getName() {
    return SeqToken.ANY_NON_NEGATIVE;
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
