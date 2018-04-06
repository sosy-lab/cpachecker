/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
    description = "Functions with function pointer parameter which will be instrumented"
  )
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
