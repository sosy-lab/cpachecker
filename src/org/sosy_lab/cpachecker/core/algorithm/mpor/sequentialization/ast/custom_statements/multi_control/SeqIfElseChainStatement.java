// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SingleControlStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIfElseChainStatement implements SeqMultiControlStatement {

  private final ImmutableList<CStatement> precedingStatements;

  private final ImmutableMap<CExpression, ? extends SeqStatement> statements;

  SeqIfElseChainStatement(
      ImmutableList<CStatement> pPrecedingStatements,
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements) {

    precedingStatements = pPrecedingStatements;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> ifElseChain = ImmutableList.builder();
    ifElseChain.addAll(SeqStringUtil.buildLinesOfCodeFromCAstNodes(precedingStatements));
    ifElseChain.addAll(buildIfElseChain(statements));
    return SeqStringUtil.joinWithNewlines(ifElseChain.build());
  }

  private static ImmutableList<String> buildIfElseChain(
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> ifElseChain = ImmutableList.builder();
    boolean isFirst = true;
    for (var statement : pStatements.entrySet()) {
      // first statement: use "if", otherwise "else if"
      SingleControlStatementType statementType =
          isFirst ? SingleControlStatementType.IF : SingleControlStatementType.ELSE_IF;
      ifElseChain.add(statementType.buildControlFlowPrefix(statement.getKey()));
      ifElseChain.add(statement.getValue().toASTString());
      isFirst = false;
    }
    ifElseChain.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return ifElseChain.build();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.IF_ELSE_CHAIN;
  }
}
