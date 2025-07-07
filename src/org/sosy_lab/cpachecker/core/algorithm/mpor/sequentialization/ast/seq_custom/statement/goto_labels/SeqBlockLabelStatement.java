// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/** The label of a block of {@link SeqThreadStatement}s. */
public class SeqBlockLabelStatement implements SeqLabelStatement {

  public final String threadPrefix;

  public final int labelNumber;

  public SeqBlockLabelStatement(String pThreadPrefix, int pLabelNumber) {
    // we store the thread prefix so that cloning does not require the options (shortVariables)
    threadPrefix = pThreadPrefix;
    labelNumber = pLabelNumber;
  }

  @Override
  public String toASTString() {
    return toASTStringWithoutColon() + SeqSyntax.COLON;
  }

  @Override
  public String getLabelName() {
    return threadPrefix + labelNumber;
  }

  @Override
  public String toASTStringWithoutColon() {
    return getLabelName();
  }

  public SeqBlockLabelStatement cloneWithLabelNumber(int pLabelNumber) {
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }
}
