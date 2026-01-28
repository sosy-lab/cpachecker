// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels;

import org.sosy_lab.cpachecker.cfa.ast.c.CLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * The label before a block of {@link CSeqThreadStatement}s, e.g. {@code T0_0: stmt1; stmt2; ...}.
 *
 * <p>This cannot be replaced with a {@link CLabelStatement} directly because the name of the label
 * includes a number which is adjusted during the sequentialization process. This class provides the
 * necessary interface to adjust the label number.
 */
// we store the thread prefix so that cloning does not require the options (shortVariables)
public record SeqBlockLabelStatement(String threadPrefix, int number) implements SeqStatement {

  public SeqBlockLabelStatement withLabelNumber(int pLabelNumber) {
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  public CLabelStatement toCLabelStatement() {
    return new CLabelStatement(threadPrefix + SeqSyntax.UNDERSCORE + number);
  }

  @Override
  public String toASTString() {
    return threadPrefix + SeqSyntax.UNDERSCORE + number + SeqSyntax.COLON;
  }
}
