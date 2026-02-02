// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;

/**
 * The label before a block of {@link CSeqThreadStatement}s, e.g. {@code T0_0: stmt1; stmt2; ...}.
 */
public final class SeqBlockLabelStatement extends SeqExportedStatement {

  private final String threadPrefix;

  private final int labelNumber;

  /**
   * Returns an instance of a {@link SeqBlockLabelStatement}, {@code T0_0: ...}.
   *
   * @param pThreadPrefix The thread prefix in the label, e.g. {@code T0_}
   * @param pLabelNumber The number of the block, e.g. {@code 0}
   */
  public SeqBlockLabelStatement(String pThreadPrefix, int pLabelNumber) {
    threadPrefix = pThreadPrefix;
    labelNumber = pLabelNumber;
  }

  public SeqBlockLabelStatement withLabelNumber(int pLabelNumber) {
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  public CLabelStatement toCLabelStatement() {
    return new CLabelStatement(threadPrefix + SeqSyntax.UNDERSCORE + labelNumber);
  }

  @Override
  CExportStatement toCExportStatement() {
    return new CLabelStatement(threadPrefix + SeqSyntax.UNDERSCORE + labelNumber);
  }

  public int getLabelNumber() {
    return labelNumber;
  }
}
