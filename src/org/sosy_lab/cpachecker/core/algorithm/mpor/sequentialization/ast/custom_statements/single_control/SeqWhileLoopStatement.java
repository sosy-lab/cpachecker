// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Used to export loop statements such as {@code while (1) { ... }} that can be used for infinite
 * loops with minimal overhead (= no iteration variable such as {@code i}).
 */
public final class SeqWhileLoopStatement extends CSeqLoopStatement {

  private final CExpression conditionExpression;

  public SeqWhileLoopStatement(
      CExpression pConditionExpression, ImmutableList<String> pStatements) {

    super(new SeqCompoundStatement(pStatements));
    conditionExpression = pConditionExpression;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(
        LoopType.WHILE.getKeyword()
            + SeqStringUtil.wrapInBrackets(conditionExpression.toASTString()));
    joiner.add(compoundStatement.toASTString());
    return joiner.toString();
  }
}
