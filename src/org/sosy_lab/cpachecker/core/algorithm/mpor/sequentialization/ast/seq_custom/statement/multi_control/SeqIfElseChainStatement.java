// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqElseIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadEndLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIfElseChainStatement implements SeqMultiControlStatement {

  // TODO generalize into preceding statements, ImmutableList<CStatement>?
  private final Optional<CFunctionCallStatement> assumption;

  private final ImmutableMap<CExpression, ? extends SeqStatement> statements;

  private final Optional<SeqThreadEndLabelStatement> threadEndLabel;

  private final Optional<CExpressionAssignmentStatement> lastThreadUpdate;

  SeqIfElseChainStatement(
      Optional<CFunctionCallStatement> pAssumption,
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements,
      Optional<SeqThreadEndLabelStatement> pThreadEndLabel,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate) {

    assumption = pAssumption;
    statements = pStatements;
    threadEndLabel = pThreadEndLabel;
    lastThreadUpdate = pLastThreadUpdate;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    if (assumption.isPresent()) {
      ifElseChain.add(LineOfCode.of(assumption.orElseThrow().toASTString()));
    }
    ifElseChain.addAll(buildIfElseChain(statements));
    if (threadEndLabel.isPresent()) {
      ifElseChain.add(LineOfCode.of(threadEndLabel.orElseThrow().toASTString()));
    }
    if (lastThreadUpdate.isPresent()) {
      ifElseChain.add(LineOfCode.of(lastThreadUpdate.orElseThrow().toASTString()));
    }
    return LineOfCodeUtil.buildString(ifElseChain.build());
  }

  private static ImmutableList<LineOfCode> buildIfElseChain(
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    boolean isFirst = true;
    for (var statement : pStatements.entrySet()) {
      // first statement: use "if", otherwise "else if"
      SeqSingleControlExpression controlExpression =
          isFirst
              ? new SeqIfExpression(statement.getKey())
              : new SeqElseIfExpression(statement.getKey());
      String controlStatementString = controlExpression.toASTString();
      ifElseChain.add(
          LineOfCode.of(
              isFirst
                  ? SeqStringUtil.appendCurlyBracketRight(controlStatementString)
                  : SeqStringUtil.wrapInCurlyBracketsOutwards(controlStatementString)));
      ifElseChain.add(LineOfCode.of(statement.getValue().toASTString()));
      isFirst = false;
    }
    ifElseChain.add(LineOfCode.of(SeqSyntax.CURLY_BRACKET_RIGHT));
    return ifElseChain.build();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.IF_ELSE_CHAIN;
  }
}
