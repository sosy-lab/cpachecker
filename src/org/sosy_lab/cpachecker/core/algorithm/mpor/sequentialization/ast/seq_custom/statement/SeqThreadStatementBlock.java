// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementBlock implements SeqStatement {

  public final ImmutableList<SeqThreadStatement> statements;

  public SeqThreadStatementBlock(ImmutableList<SeqThreadStatement> pStatements) {
    statements = pStatements;
  }

  public SeqThreadStatement getFirstStatement() {
    checkArgument(!statements.isEmpty(), "there are no statements, cannot get first");
    return statements.get(0);
  }

  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder statementsString = new StringBuilder();
    for (int i = 0; i < statements.size(); i++) {
      if (i == statements.size() - 1) {
        // last statement -> no space
        statementsString.append(statements.get(i).toASTString());
      } else {
        statementsString.append(statements.get(i).toASTString()).append(SeqSyntax.SPACE);
      }
    }
    // tests showed that using break is more efficient than continue, despite the loop
    return statementsString.toString();
  }
}
