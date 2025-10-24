// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.ASeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/** The label of a block of {@link ASeqThreadStatement}s. */
public class SeqBlockLabelStatement implements SeqLabelStatement {

  public final String threadPrefix;

  public final int number;

  public SeqBlockLabelStatement(String pThreadPrefix, int pNumber) {
    // we store the thread prefix so that cloning does not require the options (shortVariables)
    threadPrefix = pThreadPrefix;
    number = pNumber;
  }

  @Override
  public String toASTString() {
    return toASTStringWithoutColon() + SeqSyntax.COLON;
  }

  @Override
  public String toASTStringWithoutColon() {
    return threadPrefix + SeqSyntax.UNDERSCORE + number;
  }

  public SeqBlockLabelStatement cloneWithLabelNumber(int pLabelNumber) {
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  public int getNumber() {
    return number;
  }
}
