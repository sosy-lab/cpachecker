// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqCaseLabel implements SeqStatement {

  public final int value;

  public SeqCaseLabel(int pValue) {
    value = pValue;
  }

  @Override
  public String toASTString() {
    return SeqToken.CASE + SeqSyntax.SPACE + value + SeqSyntax.COLON;
  }
}
