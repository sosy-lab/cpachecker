// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqSingleControlStatement implements SeqStatement {

  // TODO add interface and separate classes here
  public enum SingleControlStatementEncoding {
    ELSE("else"),
    ELSE_IF("else if"),
    IF("if"),
    SWITCH("switch"),
    WHILE("while");

    public final String keyword;

    SingleControlStatementEncoding(String pKeyword) {
      keyword = pKeyword;
    }
  }

  public final SingleControlStatementEncoding encoding;

  private final Optional<CExpression> cExpression;

  private final Optional<SeqExpression> seqExpression;

  private final Optional<CAssumeEdge> assumeEdge;

  public SeqSingleControlStatement(
      CExpression pCExpression, SingleControlStatementEncoding pEncoding) {

    encoding = pEncoding;
    cExpression = Optional.of(pCExpression);
    seqExpression = Optional.empty();
    assumeEdge = Optional.empty();
  }

  /** Use this constructor if the expression is a logical AND, OR, NOT. */
  public SeqSingleControlStatement(
      SeqExpression pSeqExpression, SingleControlStatementEncoding pEncoding) {

    encoding = pEncoding;
    cExpression = Optional.empty();
    seqExpression = Optional.of(pSeqExpression);
    assumeEdge = Optional.empty();
  }

  /**
   * {@link CAssumeEdge#getExpression()} does not negate the returned {@link CExpression}, forcing
   * us to use {@link CAssumeEdge#getCode()} instead.
   */
  public SeqSingleControlStatement(
      CAssumeEdge pAssumeEdge, SingleControlStatementEncoding pEncoding) {

    encoding = pEncoding;
    cExpression = Optional.empty();
    seqExpression = Optional.empty();
    assumeEdge = Optional.of(pAssumeEdge);
  }

  // TODO refactor this, empty constructor is not so nice and unclear
  /** Use this constructor when there is no expression, i.e. {@code else { ... }} */
  public SeqSingleControlStatement() {
    encoding = SingleControlStatementEncoding.ELSE;
    cExpression = Optional.empty();
    seqExpression = Optional.empty();
    assumeEdge = Optional.empty();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String expression;
    if (cExpression.isPresent()) {
      expression = cExpression.orElseThrow().toASTString();
    } else if (seqExpression.isPresent()) {
      expression = seqExpression.orElseThrow().toASTString();
    } else if (assumeEdge.isPresent()) {
      expression = assumeEdge.orElseThrow().getCode();
    } else {
      // no expression -> just else
      return encoding.keyword;
    }
    return encoding.keyword
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + expression
        + SeqSyntax.BRACKET_RIGHT;
  }
}
