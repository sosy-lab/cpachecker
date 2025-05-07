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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAtomicStatementBlock implements SeqStatementBlock {

  public final ImmutableList<SeqThreadStatementBlock> blocks;

  public SeqAtomicStatementBlock(ImmutableList<SeqThreadStatementBlock> pBlocks) {
    blocks = pBlocks;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder rBlock = new StringBuilder();
    for (SeqThreadStatementBlock block : blocks) {
      rBlock.append(block.toASTString());
    }
    return rBlock.toString();
  }

  @Override
  public SeqBlockGotoLabelStatement getGotoLabel() {
    return blocks.get(0).getGotoLabel();
  }

  @Override
  public SeqThreadStatement getFirstStatement() {
    return blocks.get(0).getFirstStatement();
  }

  @Override
  public ImmutableList<SeqThreadStatement> getStatements() {
    return blocks.stream()
        .flatMap(block -> block.getStatements().stream())
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public SeqStatementBlock cloneWithStatements(ImmutableList<SeqThreadStatement> pStatements) {
    throw new UnsupportedOperationException(this.getClass().getName() + " cannot be cloned");
  }

  @Override
  public boolean startsAtomicBlock() {
    return blocks.get(0).startsAtomicBlock();
  }

  @Override
  public boolean startsInAtomicBlock() {
    return false;
  }
}
