// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqLoopHeadLabelStatement implements SeqCaseBlockInjectedStatement {

  public final String labelName;

  public SeqLoopHeadLabelStatement(String pLabelName) {
    labelName = pLabelName;
  }

  @Override
  public String toASTString() {
    return labelName + SeqSyntax.COLON;
  }
}
