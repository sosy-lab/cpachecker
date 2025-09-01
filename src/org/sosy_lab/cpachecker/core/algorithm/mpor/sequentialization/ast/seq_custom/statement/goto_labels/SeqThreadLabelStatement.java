// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadLabelStatement implements SeqLabelStatement {

  private final String name;

  public SeqThreadLabelStatement(String pName) {
    name = pName;
  }

  @Override
  public String toASTStringWithoutColon() {
    return name;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return toASTStringWithoutColon() + SeqSyntax.COLON;
  }
}
