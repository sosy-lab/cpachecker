// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqGotoStatement implements SeqStatement {

  private final String labelName;

  public SeqGotoStatement(String pLabelName) {
    labelName = pLabelName;
  }

  @Override
  public String toASTString() {
    return SeqToken._goto + SeqSyntax.SPACE + labelName + SeqSyntax.SEMICOLON;
  }
}
