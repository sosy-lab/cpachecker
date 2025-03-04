// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

/** Of the form {@code case n:} where {@code n} is an integer. */
public class SeqCaseLabel implements SeqStatement {

  public final int value;

  public SeqCaseLabel(int pValue) {
    value = pValue;
  }

  public SeqCaseLabel(CIntegerLiteralExpression pValueExpression) {
    value = pValueExpression.getValue().intValue();
  }

  @Override
  public String toASTString() {
    return SeqToken._case + SeqSyntax.SPACE + value + SeqSyntax.COLON;
  }
}
