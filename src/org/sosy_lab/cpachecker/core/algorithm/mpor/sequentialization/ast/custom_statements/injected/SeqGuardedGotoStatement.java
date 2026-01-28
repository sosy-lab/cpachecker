// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CCompoundStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CExportStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CGotoStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CIfStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CStatementWrapper;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

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
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    ImmutableList<CExportStatement> ifStatements =
        ImmutableList.<CExportStatement>builder()
            .addAll(precedingStatements.stream().map(s -> new CStatementWrapper(s)).iterator())
            .add(new CGotoStatement(targetLabel.toCLabelStatement()))
            .build();
    CCompoundStatement compoundStatement = new CCompoundStatement(ifStatements);
    CIfStatement ifStatement =
        new CIfStatement(new CExpressionWrapper(condition), compoundStatement);
    return ifStatement.toASTString(pAAstNodeRepresentation);
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
