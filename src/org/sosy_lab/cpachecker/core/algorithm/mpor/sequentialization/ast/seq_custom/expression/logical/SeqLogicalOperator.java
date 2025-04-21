// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public enum SeqLogicalOperator {
  AND(SeqSyntax.LOGICAL_AND),
  OR(SeqSyntax.LOGICAL_OR),
  NOT(SeqSyntax.LOGICAL_NOT);

  private final String string;

  SeqLogicalOperator(String pString) {
    string = pString;
  }

  @Override
  public String toString() {
    return string;
  }
}
