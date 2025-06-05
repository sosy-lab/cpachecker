// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorAccessEvaluationStatement implements SeqBitVectorEvaluationStatement {

  private final BitVectorEvaluationExpression evaluationExpression;

  public final SeqBlockGotoLabelStatement gotoLabel;

  public SeqBitVectorAccessEvaluationStatement(
      BitVectorEvaluationExpression pEvaluationExpression, SeqBlockGotoLabelStatement pGotoLabel) {

    evaluationExpression = pEvaluationExpression;
    gotoLabel = pGotoLabel;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.empty();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    if (evaluationExpression.isEmpty()) {
      // no evaluation due to no global accesses -> just goto
      return gotoStatement.toASTString();
    } else {
      SeqSingleControlFlowStatement ifStatement =
          new SeqSingleControlFlowStatement(evaluationExpression, SeqControlFlowStatementType.IF);
      return ifStatement.toASTString()
          + SeqSyntax.SPACE
          + SeqStringUtil.wrapInCurlyInwards(gotoStatement.toASTString());
    }
  }

  @Override
  public SeqBitVectorAccessEvaluationStatement cloneWithGotoLabelNumber(int pLabelNumber) {
    return new SeqBitVectorAccessEvaluationStatement(
        evaluationExpression, gotoLabel.cloneWithLabelNumber(pLabelNumber));
  }

  @Override
  public BitVectorEvaluationExpression getEvaluationExpression() {
    return evaluationExpression;
  }
}
