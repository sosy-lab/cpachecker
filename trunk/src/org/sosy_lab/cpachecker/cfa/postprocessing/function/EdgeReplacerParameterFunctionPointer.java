// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

@Options
public class EdgeReplacerParameterFunctionPointer extends EdgeReplacer {
  @Option(
      secure = true,
      name = "analysis.replacedFunctionsWithParameters",
      description = "Functions with function pointer parameter which will be instrumented")
  protected Set<String> replacedFunctionsWithParameters = ImmutableSet.of("pthread_create");

  public EdgeReplacerParameterFunctionPointer(
      MutableCFA pCfa, Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pCfa, config, pLogger);
    config.inject(this);
  }

  @Override
  protected boolean shouldBeInstrumented(CFunctionCall functionCall) {
    return replacedFunctionsWithParameters.contains(
        functionCall.getFunctionCallExpression().getFunctionNameExpression().toString());
  }

  @Override
  protected CFunctionCallExpression createNewCallExpression(
      CFunctionCallExpression oldCallExpr,
      CExpression nameExp,
      FunctionEntryNode fNode,
      CIdExpression func) {
    List<CExpression> params = new ArrayList<>();
    for (CExpression param : oldCallExpr.getParameterExpressions()) {
      if (param == nameExp) {
        params.add(func);
      } else {
        params.add(param);
      }
    }
    return new CFunctionCallExpression(
        oldCallExpr.getFileLocation(),
        oldCallExpr.getExpressionType(),
        oldCallExpr.getFunctionNameExpression(),
        params,
        oldCallExpr.getDeclaration());
  }

  // class ThreadCreateTransformer does not support SummaryEdge with a pointer parameter or a
  // structure field
  @Override
  protected AbstractCFAEdge createSummaryEdge(
      CStatementEdge statement, CFANode rootNode, CFANode end) {
    return new BlankEdge("skip " + statement, statement.getFileLocation(), rootNode, end, "skip");
  }
}
