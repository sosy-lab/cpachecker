// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqThreadLabelStatement(String name) implements SeqLabelStatement {

  @Override
  public String toASTStringWithoutColon() {
    return name;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return toASTStringWithoutColon() + SeqSyntax.COLON;
  }
}
