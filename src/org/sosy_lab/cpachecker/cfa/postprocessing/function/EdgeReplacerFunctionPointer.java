// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

@Options
public class EdgeReplacerFunctionPointer extends EdgeReplacer {
  public EdgeReplacerFunctionPointer(MutableCFA pCfa, Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pCfa, config, pLogger);
  }

  @Override
  protected CFunctionCallExpression createNewCallExpression(
      CFunctionCallExpression oldCallExpr,
      CExpression nameExp,
      FunctionEntryNode fNode,
      CIdExpression func) {
    CIdExpression funcName =
        new CIdExpression(
            oldCallExpr.getFunctionNameExpression().getFileLocation(),
            oldCallExpr.getFunctionNameExpression().getExpressionType(),
            fNode.getFunctionName(),
            (CSimpleDeclaration) fNode.getFunctionDefinition());
    return new CFunctionCallExpression(
        oldCallExpr.getFileLocation(),
        oldCallExpr.getExpressionType(),
        funcName,
        oldCallExpr.getParameterExpressions(),
        (CFunctionDeclaration) fNode.getFunctionDefinition());
  }

  @Override
  protected AbstractCFAEdge createSummaryEdge(
      CStatementEdge statement, CFANode rootNode, CFANode end) {
    return new CStatementEdge(
        statement.getRawStatement(),
        statement.getStatement(),
        statement.getFileLocation(),
        rootNode,
        end);
  }
}
