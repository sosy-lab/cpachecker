// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A block features a {@code goto} label and a list of {@link CSeqThreadStatement}. An inner block
 * is only reachable from inside a thread simulation via its {@code goto} label.
 */
public class SeqThreadStatementBlock implements SeqStatement {

  private final MPOROptions options;

  private final Optional<SeqThreadLabelStatement> nextThreadLabel;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final SeqBlockLabelStatement label;

  private final ImmutableList<CSeqThreadStatement> statements;

  private final boolean isLoopStart;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      Optional<SeqThreadLabelStatement> pNextThreadLabel,
      SeqBlockLabelStatement pLabel,
      ImmutableList<CSeqThreadStatement> pStatements) {

    options = pOptions;
    nextThreadLabel = pNextThreadLabel;
    label = pLabel;
    statements = pStatements;
    isLoopStart = SeqThreadStatementBlockUtil.isLoopStart(statements);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(label.toASTString() + SeqSyntax.SPACE);
    for (CSeqThreadStatement statement : statements) {
      joiner.add(statement.toASTString() + SeqSyntax.SPACE);
    }
    Optional<String> suffix =
        SeqStringUtil.tryBuildBlockSuffix(options, nextThreadLabel, statements);
    if (suffix.isPresent()) {
      joiner.add(suffix.orElseThrow());
    }
    return joiner.toString();
  }

  public SeqBlockLabelStatement getLabel() {
    return label;
  }

  public CSeqThreadStatement getFirstStatement() {
    return statements.getFirst();
  }

  public ImmutableList<CSeqThreadStatement> getStatements() {
    return statements;
  }

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public SeqThreadStatementBlock cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, nextThreadLabel, label.cloneWithLabelNumber(pLabelNumber), statements);
  }

  public SeqThreadStatementBlock cloneWithStatements(
      ImmutableList<CSeqThreadStatement> pStatements) {

    return new SeqThreadStatementBlock(options, nextThreadLabel, label, pStatements);
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
