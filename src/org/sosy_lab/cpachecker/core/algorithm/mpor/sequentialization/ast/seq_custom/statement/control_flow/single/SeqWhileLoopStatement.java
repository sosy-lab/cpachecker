// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqWhileLoopStatement implements SeqLoopStatement {

  private final CExpression loopCondition;

  public SeqWhileLoopStatement(CExpression pLoopCondition) {
    loopCondition = pLoopCondition;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return SeqLoopStatementEncoding._while.keyword
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInBrackets(loopCondition.toASTString());
  }
}
