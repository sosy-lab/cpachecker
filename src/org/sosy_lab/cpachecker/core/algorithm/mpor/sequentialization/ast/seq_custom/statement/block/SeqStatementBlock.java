// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;

public interface SeqStatementBlock extends SeqStatement {

  SeqBlockGotoLabelStatement getGotoLabel();

  SeqThreadStatement getFirstStatement();

  ImmutableList<SeqThreadStatement> getStatements();

  SeqStatementBlock cloneWithStatements(ImmutableList<SeqThreadStatement> pStatements);

  SeqStatementBlock cloneWithLabelAndStatements(
      int pLabelNumber, ImmutableList<SeqThreadStatement> pStatements);

  boolean startsAtomicBlock();

  boolean startsInAtomicBlock();
}
