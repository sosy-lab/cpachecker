// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqLoopStatement implements SeqSingleControlStatement {

  private final Optional<CExpression> whileExpression;

  private final Optional<CVariableDeclaration> forCounterDeclaration;

  private final Optional<CExpression> forExpression;

  private final Optional<CExpressionAssignmentStatement> forIterationUpdate;

  /**
   * The list of statements inside the loop block. Can be empty to model infinite loops that do
   * nothing. We use {@code String}s so that comments are accepted too.
   */
  private final ImmutableList<String> statements;

  /** Use this constructor for an {@code while (...) { ... }} statement. */
  public SeqLoopStatement(CExpression pWhileExpression, ImmutableList<String> pStatements) {
    whileExpression = Optional.of(pWhileExpression);
    forCounterDeclaration = Optional.empty();
    forExpression = Optional.empty();
    forIterationUpdate = Optional.empty();
    statements = pStatements;
  }

  /**
   * Use this constructor for an {@code for (...) { ... }} statement. The {@code for} expression
   * consists of three parts: the counter declaration e.g. {@code int i = 0}, the stop condition
   * expression e.g. {@code i < 42} and iteration update e.g. {@code i = i + 1}.
   */
  public SeqLoopStatement(
      CVariableDeclaration pForCounterDeclaration,
      CExpression pForExpression,
      CExpressionAssignmentStatement pForIterationUpdate,
      ImmutableList<String> pStatements) {

    whileExpression = Optional.empty();
    forCounterDeclaration = Optional.of(pForCounterDeclaration);
    forExpression = Optional.of(pForExpression);
    forIterationUpdate = Optional.of(pForIterationUpdate);
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    checkArgument(
        whileExpression.isPresent()
            || (forCounterDeclaration.isPresent()
                && forExpression.isPresent()
                && forIterationUpdate.isPresent()),
        "either all necessary while or for loop fields must be present");

    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    if (whileExpression.isPresent()) {
      joiner.add(
          Joiner.on(SeqSyntax.SPACE)
              .join(
                  LoopType.WHILE.getKeyword(),
                  SeqStringUtil.wrapInBrackets(whileExpression.orElseThrow().toASTString()),
                  SeqSyntax.CURLY_BRACKET_LEFT));
    } else {
      joiner.add(buildForLoopControlFlowPrefix());
    }
    statements.forEach(statement -> joiner.add(statement));
    return joiner.add(SeqSyntax.CURLY_BRACKET_RIGHT).toString();
  }

  private String buildForLoopControlFlowPrefix() {
    CVariableDeclaration counterDeclaration = forCounterDeclaration.orElseThrow();
    CExpression expression = forExpression.orElseThrow();
    CExpressionAssignmentStatement iterationUpdate = forIterationUpdate.orElseThrow();

    StringJoiner outerJoiner = new StringJoiner(SeqSyntax.SPACE);
    outerJoiner.add(LoopType.FOR.getKeyword());

    StringJoiner innerJoiner = new StringJoiner(SeqSyntax.SPACE);
    // build the variable declaration without semicolon, it is appended by
    innerJoiner.add(counterDeclaration.toASTString());
    innerJoiner.add(expression.toASTString() + SeqSyntax.SEMICOLON);
    // exclude the semicolon in the assignment statement
    innerJoiner.add(
        iterationUpdate.toASTString().replace(SeqSyntax.SEMICOLON, SeqSyntax.EMPTY_STRING));

    outerJoiner.add(SeqStringUtil.wrapInBrackets(innerJoiner.toString()));
    outerJoiner.add(SeqSyntax.CURLY_BRACKET_LEFT);

    return outerJoiner.toString();
  }
}
