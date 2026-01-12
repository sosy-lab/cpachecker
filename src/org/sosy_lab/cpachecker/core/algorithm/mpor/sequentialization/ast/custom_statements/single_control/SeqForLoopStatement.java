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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Used to export loop statements such as {@code for (int i = 0; i < N; i++) { ... }} that can be
 * used for finite loops.
 */
public final class SeqForLoopStatement extends CSeqLoopStatement {

  private final CVariableDeclaration counterDeclaration;

  private final CExpression conditionExpression;

  private final CExpressionAssignmentStatement iterationUpdate;

  public SeqForLoopStatement(
      CVariableDeclaration pForCounterDeclaration,
      CExpression pForExpression,
      CExpressionAssignmentStatement pForIterationUpdate,
      ImmutableList<String> pStatements) {

    super(new SeqCompoundStatement(pStatements));
    counterDeclaration = pForCounterDeclaration;
    conditionExpression = pForExpression;
    iterationUpdate = pForIterationUpdate;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(buildForLoopControlFlowPrefix());
    joiner.add(compoundStatement.toASTString());
    return joiner.toString();
  }

  private String buildForLoopControlFlowPrefix() {
    StringJoiner outerJoiner = new StringJoiner(SeqSyntax.SPACE);
    outerJoiner.add(LoopType.FOR.getKeyword());

    StringJoiner innerJoiner = new StringJoiner(SeqSyntax.SPACE);
    // build the variable declaration without semicolon, it is appended by
    innerJoiner.add(counterDeclaration.toASTString());
    innerJoiner.add(conditionExpression.toASTString() + SeqSyntax.SEMICOLON);
    // exclude the semicolon in the assignment statement
    innerJoiner.add(
        iterationUpdate.toASTString().replace(SeqSyntax.SEMICOLON, SeqSyntax.EMPTY_STRING));

    outerJoiner.add(SeqStringUtil.wrapInBrackets(innerJoiner.toString()));

    return outerJoiner.toString();
  }
}
