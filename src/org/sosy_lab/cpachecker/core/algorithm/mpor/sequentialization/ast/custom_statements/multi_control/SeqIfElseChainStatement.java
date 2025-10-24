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
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.BranchType;
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
    StringJoiner ifElseChain = new StringJoiner(SeqSyntax.NEWLINE);
    // first add preceding statements
    precedingStatements.forEach(statement -> ifElseChain.add(statement.toASTString()));
    // then add all statements via if ... else if ...
    boolean isFirst = true;
    for (var statement : statements.entrySet()) {
      // first statement: use "if", otherwise "else if"
      BranchType branchType = isFirst ? BranchType.IF : BranchType.ELSE_IF;
      ifElseChain.add(branchType.buildPrefix(statement.getKey().toASTString()));
      ifElseChain.add(statement.getValue().toASTString());
      isFirst = false;
    }
    ifElseChain.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return ifElseChain.toString();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.IF_ELSE_CHAIN;
  }
}
