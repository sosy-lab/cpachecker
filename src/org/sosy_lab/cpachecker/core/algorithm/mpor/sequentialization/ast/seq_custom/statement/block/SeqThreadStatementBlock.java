// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block;


import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementBlock implements SeqStatementBlock {

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  public final SeqBlockGotoLabelStatement gotoLabel;

  public final ImmutableList<SeqThreadStatement> statements;

  public SeqThreadStatementBlock(
      SeqBlockGotoLabelStatement pGotoLabel, ImmutableList<SeqThreadStatement> pStatements) {

    gotoLabel = pGotoLabel;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder statementsString = new StringBuilder();
    for (int i = 0; i < statements.size(); i++) {
      statementsString.append(SeqSyntax.NEWLINE).append(SeqSyntax.SPACE.repeat(25));
      if (i == statements.size() - 1) {
        // last statement -> no space
        statementsString.append(statements.get(i).toASTString());
      } else {
        statementsString.append(statements.get(i).toASTString()).append(SeqSyntax.SPACE);
      }
    }
    // tests showed that using break is more efficient than continue, despite the loop
    return gotoLabel.toASTString() + SeqSyntax.SPACE + statementsString;
  }

  @Override
  public SeqBlockGotoLabelStatement getGotoLabel() {
    return gotoLabel;
  }

  @Override
  public SeqThreadStatement getFirstStatement() {
    return statements.get(0);
  }

  @Override
  public ImmutableList<SeqThreadStatement> getStatements() {
    return statements;
  }

  @Override
  public SeqThreadStatementBlock cloneWithStatements(
      ImmutableList<SeqThreadStatement> pStatements) {
    return new SeqThreadStatementBlock(gotoLabel, pStatements);
  }

  @Override
  public boolean startsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicBeginStatement;
  }

  @Override
  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }
}
