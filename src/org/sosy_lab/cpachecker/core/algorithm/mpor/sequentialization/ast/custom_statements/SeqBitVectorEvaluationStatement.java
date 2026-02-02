// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;

/**
 * The statement for evaluating bit vectors (including {@code if (...)}). Used for both {@link
 * ReductionMode#ACCESS_ONLY} and {@link ReductionMode#READ_AND_WRITE}.
 */
public record SeqBitVectorEvaluationStatement(
    MPOROptions options,
    Optional<CExportExpression> evaluationExpression,
    SeqBlockLabelStatement targetGoto)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    // no evaluation due to no global accesses -> just goto
    if (evaluationExpression.isEmpty()) {
      return ImmutableList.of(new CGotoStatement(targetGoto.toCLabelStatement()));
    }
    // for next_thread nondeterminism, we use goto instead of assume, if there is no conflict
    if (options.nondeterminismSource().equals(NondeterminismSource.NEXT_THREAD)) {
      CLogicalNotExpression ifExpression = evaluationExpression.orElseThrow().negate();
      CGotoStatement gotoStatement = new CGotoStatement(targetGoto.toCLabelStatement());
      CCompoundStatement compoundStatement = new CCompoundStatement(gotoStatement);
      return ImmutableList.of(new CIfStatement(ifExpression, compoundStatement));
    }
    return ImmutableList.of(
        SeqAssumeFunction.buildAssumeFunctionCallStatement(evaluationExpression.orElseThrow()));
  }

  @Override
  public SeqInjectedStatementWithTargetGoto withTargetNumber(int pTargetNumber) {
    return new SeqBitVectorEvaluationStatement(
        options, evaluationExpression, targetGoto.withLabelNumber(pTargetNumber));
  }

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return false;
  }
}
