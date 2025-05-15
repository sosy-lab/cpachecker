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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementBlock implements SeqStatement {

  private static final int GOTO_LABEL_TABS = 6;

  private static final int BLOCK_TABS = GOTO_LABEL_TABS + 1;

  private final MPOROptions options;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final SeqBlockGotoLabelStatement gotoLabel;

  public final ImmutableList<SeqThreadStatement> statements;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      SeqBlockGotoLabelStatement pGotoLabel,
      ImmutableList<SeqThreadStatement> pStatements) {

    options = pOptions;
    gotoLabel = pGotoLabel;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder statementsString = new StringBuilder();
    for (int i = 0; i < statements.size(); i++) {
      // we use a fixed tab length so that blocks are aligned, even in binary if-else trees
      statementsString.append(SeqSyntax.NEWLINE).append(SeqStringUtil.buildTab(BLOCK_TABS));
      statementsString.append(statements.get(i).toASTString()).append(SeqSyntax.SPACE);
    }
    Optional<String> suffix = tryBuildControlFlowSuffixByEncoding(options, statements);
    return SeqSyntax.NEWLINE
        + SeqStringUtil.buildTab(GOTO_LABEL_TABS)
        + gotoLabel.toASTString()
        + SeqSyntax.SPACE
        + statementsString
        + (suffix.isPresent() ? suffix.orElseThrow() : SeqSyntax.EMPTY_STRING);
  }

  public SeqBlockGotoLabelStatement getGotoLabel() {
    return gotoLabel;
  }

  public SeqThreadStatement getFirstStatement() {
    return statements.get(0);
  }

  public ImmutableList<SeqThreadStatement> getStatements() {
    return statements;
  }

  public SeqThreadStatementBlock cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, gotoLabel.cloneWithLabelNumber(pLabelNumber), statements);
  }

  public SeqThreadStatementBlock cloneWithStatements(
      ImmutableList<SeqThreadStatement> pStatements) {

    return new SeqThreadStatementBlock(options, gotoLabel, pStatements);
  }

  public boolean startsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicBeginStatement;
  }

  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }

  private static Optional<String> tryBuildControlFlowSuffixByEncoding(
      MPOROptions pOptions, ImmutableList<SeqThreadStatement> pStatements) {

    if (SeqThreadStatementUtil.allHaveTargetGoto(pStatements)) {
      return Optional.empty();
    }
    if (SeqThreadStatementUtil.allHaveBitVectorEvaluationWithOnlyGoto(pStatements)) {
      return Optional.empty();
    }
    return Optional.of(SeqStringUtil.buildControlFlowSuffixByEncoding(pOptions));
  }
}
