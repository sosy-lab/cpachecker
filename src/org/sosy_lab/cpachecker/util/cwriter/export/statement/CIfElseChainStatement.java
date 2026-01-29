// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

public final class CIfElseChainStatement extends CMultiControlStatement {

  public CIfElseChainStatement(
      ImmutableListMultimap<CExportExpression, ? extends CExportStatement> pStatements) {

    super(pStatements);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
        statementList = transformStatements();

    // start with the very last element (the innermost branch)
    CIfStatement chain =
        new CIfStatement(
            statementList.getLast().getKey(),
            new CCompoundStatement(ImmutableList.copyOf(statementList.getLast().getValue())));

    // wrap it backwards
    for (int i = statementList.size() - 2; i >= 0; i--) {
      Entry<CExportExpression, ImmutableList<? extends CExportStatement>> current =
          statementList.get(i);
      // nest the previous 'if' inside the 'else'
      chain =
          new CIfStatement(
              current.getKey(),
              new CCompoundStatement(ImmutableList.copyOf(current.getValue())),
              new CCompoundStatement(ImmutableList.of(chain)));
    }
    return chain.toASTString(pAAstNodeRepresentation);
  }
}
