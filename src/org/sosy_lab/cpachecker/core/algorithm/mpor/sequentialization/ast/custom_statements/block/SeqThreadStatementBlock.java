// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.ASeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementBlock implements SeqStatement {

  private final MPOROptions options;

  private final Optional<MPORThread> nextThread;

  private final ImmutableMap<MPORThread, SeqThreadLabelStatement> threadLabels;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final SeqBlockLabelStatement label;

  private final ImmutableList<ASeqThreadStatement> statements;

  private final boolean isLoopStart;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      Optional<MPORThread> pNextThread,
      ImmutableMap<MPORThread, SeqThreadLabelStatement> pThreadLabels,
      SeqBlockLabelStatement pLabel,
      ImmutableList<ASeqThreadStatement> pStatements) {

    options = pOptions;
    nextThread = pNextThread;
    threadLabels = pThreadLabels;
    label = pLabel;
    statements = pStatements;
    isLoopStart = SeqThreadStatementBlockUtil.isLoopStart(statements);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(label.toASTString() + SeqSyntax.SPACE);
    for (ASeqThreadStatement statement : statements) {
      joiner.add(statement.toASTString() + SeqSyntax.SPACE);
    }
    Optional<String> suffix =
        SeqStringUtil.tryBuildBlockSuffix(options, nextThread, threadLabels, statements);
    if (suffix.isPresent()) {
      joiner.add(suffix.orElseThrow());
    }
    return joiner.toString();
  }

  public SeqBlockLabelStatement getLabel() {
    return label;
  }

  public ASeqThreadStatement getFirstStatement() {
    return statements.getFirst();
  }

  public ImmutableList<ASeqThreadStatement> getStatements() {
    return statements;
  }

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public SeqThreadStatementBlock cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, nextThread, threadLabels, label.cloneWithLabelNumber(pLabelNumber), statements);
  }

  public SeqThreadStatementBlock cloneWithStatements(
      ImmutableList<ASeqThreadStatement> pStatements) {

    return new SeqThreadStatementBlock(options, nextThread, threadLabels, label, pStatements);
  }

  public boolean startsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicBeginStatement;
  }

  public boolean endsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicEndStatement;
  }

  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }
}
