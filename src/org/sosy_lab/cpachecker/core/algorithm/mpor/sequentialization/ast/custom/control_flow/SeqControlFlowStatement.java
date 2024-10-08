// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.control_flow;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqControlFlowStatement implements SeqExpression {

  public enum SeqControlFlowStatementType {
    ELSE("else"),
    ELSE_IF("else if"),
    IF("if"),
    SWITCH("switch"),
    WHILE("while");

    public final String keyword;

    SeqControlFlowStatementType(String pKeyword) {
      keyword = pKeyword;
    }
  }

  private final SeqControlFlowStatementType type;

  private final Optional<CExpression> cExpression;

  private final Optional<SeqExpression> seqExpression;

  private final Optional<CAssumeEdge> assumeEdge;

  public SeqControlFlowStatement(CExpression pCExpression, SeqControlFlowStatementType pType) {
    type = pType;
    cExpression = Optional.of(pCExpression);
    seqExpression = Optional.empty();
    assumeEdge = Optional.empty();
  }

  /** Use this constructor if the expression is a logical AND, OR, NOT. */
  public SeqControlFlowStatement(SeqExpression pSeqExpression, SeqControlFlowStatementType pType) {
    type = pType;
    cExpression = Optional.empty();
    seqExpression = Optional.of(pSeqExpression);
    assumeEdge = Optional.empty();
  }

  /**
   * {@link CAssumeEdge#getExpression()} does not negate the returned {@link CExpression}, forcing
   * us to use {@link CAssumeEdge#getCode()} instead.
   */
  public SeqControlFlowStatement(CAssumeEdge pAssumeEdge, SeqControlFlowStatementType pType) {
    type = pType;
    cExpression = Optional.empty();
    seqExpression = Optional.empty();
    assumeEdge = Optional.of(pAssumeEdge);
  }

  @Override
  public String toASTString() {
    String expression;
    if (cExpression.isPresent()) {
      expression = cExpression.orElseThrow().toASTString();
    } else if (seqExpression.isPresent()) {
      expression = seqExpression.orElseThrow().toASTString();
    } else if (assumeEdge.isPresent()) {
      expression = assumeEdge.orElseThrow().getCode();
    } else {
      throw new IllegalArgumentException(
          "either CExpression, SeqExpression or CAssumeEdge must be present");
    }
    return type.keyword
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + expression
        + SeqSyntax.BRACKET_RIGHT;
  }
}
