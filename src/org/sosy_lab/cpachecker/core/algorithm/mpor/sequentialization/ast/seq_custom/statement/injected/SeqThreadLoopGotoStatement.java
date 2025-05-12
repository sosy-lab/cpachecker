// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadLoopGotoStatement implements SeqInjectedStatement {

  private final CBinaryExpression rSmallerK;

  private final SeqLabelStatement gotoLabel;

  public SeqThreadLoopGotoStatement(CBinaryExpression pRSmallerK, SeqLabelStatement pGotoLabel) {
    rSmallerK = pRSmallerK;
    gotoLabel = pGotoLabel;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.empty();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqSingleControlFlowStatement ifStatement =
        new SeqSingleControlFlowStatement(rSmallerK, SeqControlFlowStatementType.IF);
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    return ifStatement.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyInwards(gotoStatement.toASTString());
  }
}
