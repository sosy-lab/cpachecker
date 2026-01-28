// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels;

import org.sosy_lab.cpachecker.cfa.ast.c.CLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/** The label of a block of {@link CSeqThreadStatement}s. */
// we store the thread prefix so that cloning does not require the options (shortVariables)
public record SeqBlockLabelStatement(String threadPrefix, int number) implements SeqLabelStatement {

  @Override
  public String toASTString() {
    return toASTStringWithoutColon() + SeqSyntax.COLON;
  }

  @Override
  public String toASTStringWithoutColon() {
    return threadPrefix + SeqSyntax.UNDERSCORE + number;
  }

  public SeqBlockLabelStatement withLabelNumber(int pLabelNumber) {
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  @Override
  public CLabelStatement toCLabelStatement() {
    return new CLabelStatement(toASTStringWithoutColon());
  }
}
