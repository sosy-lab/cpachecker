// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

// TODO rename, this is not necessarily linked to switch (bintree if-else)
public class SeqSwitchCaseGotoLabelStatement implements SeqLabelStatement {

  public final String threadPrefix;

  public final int labelNumber;

  public SeqSwitchCaseGotoLabelStatement(String pThreadPrefix, int pLabelNumber) {
    // we store the thread prefix so that cloning does not require the options (shortVariables)
    threadPrefix = pThreadPrefix;
    labelNumber = pLabelNumber;
  }

  @Override
  public String toASTString() {
    return getLabelName() + SeqSyntax.COLON;
  }

  @Override
  public String getLabelName() {
    return threadPrefix + labelNumber;
  }

  public SeqSwitchCaseGotoLabelStatement cloneWithLabelNumber(int pLabelNumber) {
    return new SeqSwitchCaseGotoLabelStatement(threadPrefix, pLabelNumber);
  }
}
