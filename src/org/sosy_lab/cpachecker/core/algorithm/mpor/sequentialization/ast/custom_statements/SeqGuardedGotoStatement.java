// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

/**
 * An injected statement with a guarded {@code goto}, e.g. {@code if (condition) { goto label; }}.
 * {@code precedingStatements} can be used to specify guarded statements that are injected before
 * the {@code goto}, e.g. {@code if (condition) { statement1; goto label; }}
 */
public record SeqGuardedGotoStatement(
    CExpression condition,
    ImmutableList<CStatement> precedingStatements,
    SeqBlockLabelStatement targetLabel)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public SeqInjectedStatementWithTargetGoto withTargetNumber(int pTargetNumber) {
    return new SeqGuardedGotoStatement(
        condition, precedingStatements, targetLabel.withLabelNumber(pTargetNumber));
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    ImmutableList<CExportStatement> ifStatements =
        ImmutableList.<CExportStatement>builder()
            .addAll(precedingStatements.stream().map(s -> new CStatementWrapper(s)).iterator())
            .add(new CGotoStatement(targetLabel.toCLabelStatement()))
            .build();
    CCompoundStatement compoundStatement = new CCompoundStatement(ifStatements);
    return ImmutableList.of(new CIfStatement(new CExpressionWrapper(condition), compoundStatement));
  }

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return true;
  }
}
