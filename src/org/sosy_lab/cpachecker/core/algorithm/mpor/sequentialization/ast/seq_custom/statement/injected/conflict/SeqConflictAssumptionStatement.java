// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqConflictAssumptionStatement implements SeqInjectedStatement {

  private final CLeftHandSide pcLeftHandSide;

  private final BitVectorEvaluationExpression bitVectorEvaluation;

  private final CBinaryExpression assumptionExpression;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  SeqConflictAssumptionStatement(
      CLeftHandSide pPcLeftHandSide,
      BitVectorEvaluationExpression pBitVectorEvaluation,
      CBinaryExpression pAssumptionExpression,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    pcLeftHandSide = pPcLeftHandSide;
    bitVectorEvaluation = pBitVectorEvaluation;
    assumptionExpression = pAssumptionExpression;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.empty();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // TODO adjust tabs
    ImmutableList.Builder<LineOfCode> lines = ImmutableList.builder();
    CBinaryExpression pcActiveExpression =
        SeqExpressionBuilder.buildPcUnequalExitPc(pcLeftHandSide, binaryExpressionBuilder);
    SeqIfExpression ifPcActiveExpression = new SeqIfExpression(pcActiveExpression);
    lines.add(
        LineOfCode.of(
            3, SeqStringUtil.appendCurlyBracketRight(ifPcActiveExpression.toASTString())));
    SeqIfExpression ifBitVectorExpression = new SeqIfExpression(bitVectorEvaluation);
    lines.add(
        LineOfCode.of(
            4, SeqStringUtil.appendCurlyBracketRight(ifBitVectorExpression.toASTString())));
    CFunctionCallStatement assumeCall = SeqAssumptionBuilder.buildAssumption(assumptionExpression);
    lines.add(LineOfCode.of(5, assumeCall.toASTString()));
    lines.add(LineOfCode.of(4, SeqSyntax.CURLY_BRACKET_RIGHT));
    lines.add(LineOfCode.of(3, SeqSyntax.CURLY_BRACKET_RIGHT));
    return LineOfCodeUtil.buildString(lines.build());
  }
}
