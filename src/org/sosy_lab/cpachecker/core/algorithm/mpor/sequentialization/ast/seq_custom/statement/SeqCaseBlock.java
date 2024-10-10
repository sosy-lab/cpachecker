// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqCaseBlock implements SeqStatement {

  public final ImmutableList<SeqCaseBlockStatement> statements;

  public SeqCaseBlock(ImmutableList<SeqCaseBlockStatement> pStatements) {
    statements = pStatements;
  }

  @Override
  public String toASTString() {
    StringBuilder statement = new StringBuilder();
    for (SeqCaseBlockStatement stmt : statements) {
      statement.append(stmt.toASTString()).append(SeqSyntax.SPACE);
    }
    return statement.toString();
  }
}
