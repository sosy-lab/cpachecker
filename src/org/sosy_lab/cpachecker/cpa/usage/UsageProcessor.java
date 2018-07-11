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
package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;

@Options(prefix = "cpa.usage")
public class UsageProcessor {

  private final Map<String, BinderFunctionInfo> binderFunctionInfo;

  private final LogManager logger;
  private final VariableSkipper varSkipper;

  private Map<CFANode, Map<GeneralIdentifier, DataType>> precision;
  // Not a set, as usage.equals do not consider id
  private List<UsageInfo> result;

  public UsageProcessor(
      Configuration config,
      LogManager pLogger,
      Map<CFANode, Map<GeneralIdentifier, DataType>> pPrecision,
      Map<String, BinderFunctionInfo> pBinderFunctionInfo)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    binderFunctionInfo = pBinderFunctionInfo;

    varSkipper = new VariableSkipper(config);
    precision = pPrecision;
  }

  public List<UsageInfo> getUsagesForState(AbstractState pState) {

    result = new ArrayList<>();

    ARGState argState = (ARGState) pState;

    for (ARGState parent : argState.getParents()) {
      CFAEdge edge = parent.getEdgeToChild(argState);
      if (edge != null) {
        getUsagesForEdge(pState, edge);
      }
    }
    return result;
  }

  public void getUsagesForEdge(AbstractState pState, CFAEdge pCfaEdge) {

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        {
          CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
          handleDeclaration(pState, declEdge);
          break;
        }

        // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
          handleStatement(pState, statementEdge.getStatement());
          break;
        }

      case AssumeEdge:
        {
          visitStatement(pState, ((CAssumeEdge) pCfaEdge).getExpression(), Access.READ);
          break;
        }

      case FunctionCallEdge:
        {
          handleFunctionCall(pState, (CFunctionCallEdge) pCfaEdge);
          break;
        }

      default:
        {
          break;
        }
    }
  }

  private void handleFunctionCall(AbstractState pState, CFunctionCallEdge edge) {
    CStatement statement = edge.getRawAST().get();

    if (statement instanceof CFunctionCallAssignmentStatement) {
      /*
       * a = f(b)
       */
      CFunctionCallExpression right =
          ((CFunctionCallAssignmentStatement) statement).getRightHandSide();
      CExpression variable = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();

      // expression - only name of function
      handleFunctionCallExpression(pState, right);
      visitStatement(pState, variable, Access.WRITE);

    } else if (statement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          pState, ((CFunctionCallStatement) statement).getFunctionCallExpression());
    }
  }

  private void handleDeclaration(AbstractState pState, CDeclarationEdge declEdge) {

    if (declEdge.getDeclaration().getClass() != CVariableDeclaration.class) {
      // not a variable declaration
      return;
    }
    CVariableDeclaration decl = (CVariableDeclaration) declEdge.getDeclaration();

    if (decl.isGlobal()) {
      return;
    }

    CInitializer init = decl.getInitializer();

    if (init == null) {
      // no assignment
      return;
    }

    if (init instanceof CInitializerExpression) {
      CExpression initExpression = ((CInitializerExpression) init).getExpression();
      // Use EdgeType assignment for initializer expression to avoid mistakes related to expressions
      // "int CPACHECKER_TMP_0 = global;"
      visitStatement(pState, initExpression, Access.READ);

      // We do not add usage for currently declared variable
      // It can not cause a race
    }
  }

  private void handleFunctionCallExpression(
      AbstractState pState, final CFunctionCallExpression fcExpression) {

    String functionCallName = fcExpression.getFunctionNameExpression().toASTString();

    if (binderFunctionInfo.containsKey(functionCallName)) {
      BinderFunctionInfo currentInfo = binderFunctionInfo.get(functionCallName);
      List<CExpression> params = fcExpression.getParameterExpressions();

      AbstractIdentifier id;

      for (int i = 0; i < params.size(); i++) {
        id = currentInfo.createParamenterIdentifier(params.get(i), i, getCurrentFunction(pState));
        createUsageAndAdd(id, pState, currentInfo.getBindedAccess(i));
      }

    } else {
      fcExpression.getParameterExpressions().forEach(p -> visitStatement(pState, p, Access.READ));
    }
  }

  private void handleStatement(AbstractState pState, final CStatement pStatement) {

    if (pStatement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) pStatement;
      CExpression left = assignment.getLeftHandSide();
      CRightHandSide right = assignment.getRightHandSide();

      if (right instanceof CExpression) {
        visitStatement(pState, (CExpression) right, Access.READ);

      } else if (right instanceof CFunctionCallExpression) {
        handleFunctionCallExpression(pState, (CFunctionCallExpression) right);
      }
      visitStatement(pState, left, Access.WRITE);

    } else if (pStatement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          pState, ((CFunctionCallStatement) pStatement).getFunctionCallExpression());

    } else if (pStatement instanceof CExpressionStatement) {
      visitStatement(pState, ((CExpressionStatement) pStatement).getExpression(), Access.WRITE);
    }
  }

  private void visitStatement(
      AbstractState pState, final CExpression expression, final Access access) {
    ExpressionHandler handler = new ExpressionHandler(access, getCurrentFunction(pState));
    expression.accept(handler);

    for (Pair<AbstractIdentifier, Access> pair : handler.getProcessedExpressions()) {
      AbstractIdentifier id = pair.getFirst();
      createUsageAndAdd(id, pState, pair.getSecond());
    }
  }

  private void createUsageAndAdd(AbstractIdentifier pId, AbstractState pState, Access pAccess) {

    UsageState uState = UsageState.get(pState);
    pId = uState.getLinksIfNecessary(pId);
    UsageInfo usage = UsageInfo.createUsageInfo(pAccess, pState, pId);

    // Precise information, using results of shared analysis
    if (!usage.isRelevant()) {
      return;
    }

    SingleIdentifier singleId = usage.getId();

    CFANode node = AbstractStates.extractLocation(pState);
    Map<GeneralIdentifier, DataType> localInfo = precision.get(node);

    if (localInfo != null) {
      GeneralIdentifier gId = singleId.getGeneralId();
      if (localInfo.get(gId) == DataType.LOCAL) {
        logger.log(
            Level.FINER, singleId + " is considered to be local, so it wasn't add to statistics");
        return;
      } else {
        FluentIterable<GeneralIdentifier> composedIds =
            from(singleId.getComposedIdentifiers())
                .filter(SingleIdentifier.class)
                .transform(SingleIdentifier::getGeneralId);

        boolean isLocal = composedIds.anyMatch(i -> localInfo.get(i) == DataType.LOCAL);
        boolean isGlobal = composedIds.anyMatch(i -> localInfo.get(i) == DataType.GLOBAL);
        if (isLocal && !isGlobal) {
          logger.log(
              Level.FINER, singleId + " is supposed to be local, so it wasn't add to statistics");
          return;
        }
      }
    }

    if (varSkipper.shouldBeSkipped(singleId, usage.getCFANode().getFunctionName())) {
      return;
    }

    if (singleId instanceof LocalVariableIdentifier && singleId.getDereference() <= 0) {
      // we don't save in statistics ordinary local variables
      return;
    }
    if (singleId instanceof StructureIdentifier
        && !singleId.isGlobal()
        && !singleId.isDereferenced()) {
      // skips such cases, as 'a.b'
      return;
    }

    logger.log(Level.FINER, "Add " + usage + " to unsafe statistics");

    result.add(usage);
  }

  private String getCurrentFunction(AbstractState pState) {
    return AbstractStates.extractStateByType(pState, CallstackState.class).getCurrentFunction();
  }
}
