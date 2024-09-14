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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.NegationExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ReturnExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqValue;

// TODO will probably be unused
public class AnyFalse implements SeqFunction {

  private static final Variable array = new Variable(SeqToken.ARRAY);

  private static final Variable size = new Variable(SeqToken.SIZE);

  private static final Variable index = new Variable(SeqToken.INDEX);

  private static final FunctionSignature functionSignature =
      new FunctionSignature(
          SeqDataType.BOOL,
          new FunctionCallExpr(SeqToken.ANY_FALSE, Optional.of(initParameters())));

  private static final DeclareExpr declareExpr =
      new DeclareExpr(new VariableExpr(SeqDataType.INT, index), new Value(SeqValue.ZERO));

  private static final LoopExpr loopExpr =
      new LoopExpr(new BooleanExpr(index, SeqOperator.LESS, size));

  private static final IfExpr ifExpr = new IfExpr(new NegationExpr(new ArrayElement(array, index)));

  private static final ReturnExpr returnTrue = new ReturnExpr(new Value(SeqValue.TRUE));

  private static final IncrementExpr incrementExpr = new IncrementExpr(index);

  private static final ReturnExpr returnFalse = new ReturnExpr(new Value(SeqValue.FALSE));

  private static ImmutableList<SeqExpression> initParameters() {
    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    rParameters.add(new VariableExpr(SeqDataType.BOOL, new ArrayExpr(array, Optional.empty())));
    rParameters.add(new VariableExpr(SeqDataType.INT, size));
    return rParameters.build();
  }

  @Override
  public String createString() {
    return functionSignature.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + declareExpr.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + loopExpr.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.TAB
        + ifExpr.createString()
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
        + incrementExpr.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.NEWLINE
        + SeqSyntax.TAB
        + returnFalse.createString()
        + SeqSyntax.NEWLINE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
