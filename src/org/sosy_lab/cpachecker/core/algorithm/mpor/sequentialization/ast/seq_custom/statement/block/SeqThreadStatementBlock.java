// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementBlock implements SeqStatement {

  private final MPOROptions options;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final SeqBlockLabelStatement label;

  public final ImmutableList<SeqThreadStatement> statements;

  /** The thread executing this block. */
  private final MPORThread thread;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      SeqBlockLabelStatement pLabel,
      ImmutableList<SeqThreadStatement> pStatements,
      MPORThread pThread) {

    options = pOptions;
    label = pLabel;
    statements = pStatements;
    thread = pThread;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> lines = ImmutableList.builder();
    lines.add(LineOfCode.of(label.toASTString() + SeqSyntax.SPACE));
    for (SeqThreadStatement statement : statements) {
      lines.add(LineOfCode.of(statement.toASTString() + SeqSyntax.SPACE));
    }
    Optional<String> suffix =
        tryBuildSuffixByMultiControlStatementEncoding(options, statements, thread);
    lines.add(suffix.isPresent() ? LineOfCode.of(suffix.orElseThrow()) : LineOfCode.empty());
    return LineOfCodeUtil.buildString(lines.build());
  }

  public SeqBlockLabelStatement getLabel() {
    return label;
  }

  public SeqThreadStatement getFirstStatement() {
    return statements.get(0);
  }

  public ImmutableList<SeqThreadStatement> getStatements() {
    return statements;
  }

  public SeqThreadStatementBlock cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, label.cloneWithLabelNumber(pLabelNumber), statements, thread);
  }

  public SeqThreadStatementBlock cloneWithStatements(
      ImmutableList<SeqThreadStatement> pStatements) {

    return new SeqThreadStatementBlock(options, label, pStatements, thread);
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

  private static Optional<String> tryBuildSuffixByMultiControlStatementEncoding(
      MPOROptions pOptions, ImmutableList<SeqThreadStatement> pStatements, MPORThread pThread)
      throws UnrecognizedCodeException {

    if (SeqThreadStatementUtil.allHaveTargetGoto(pStatements)) {
      return Optional.empty();
    }
    if (SeqThreadStatementUtil.anyContainsEmptyBitVectorEvaluationExpression(pStatements)) {
      return Optional.empty();
    }
    return Optional.of(SeqStringUtil.buildSuffixByMultiControlStatementEncoding(pOptions, pThread));
  }
}
