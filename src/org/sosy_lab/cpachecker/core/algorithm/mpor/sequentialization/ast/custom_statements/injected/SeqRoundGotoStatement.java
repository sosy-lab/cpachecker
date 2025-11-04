// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.gotos.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqRoundGotoStatement(
    CBinaryExpression roundSmallerMax,
    CExpressionAssignmentStatement roundIncrement,
    SeqLabelStatement gotoLabel)
    implements SeqInjectedStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(
            roundSmallerMax.toASTString(),
            ImmutableList.of(roundIncrement.toASTString(), gotoStatement.toASTString()));
    return ifStatement.toASTString();
  }
}
