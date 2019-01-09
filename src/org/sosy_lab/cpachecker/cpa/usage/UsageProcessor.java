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
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.util.identifiers.FunctionIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class UsageProcessor {
  private final Map<String, BinderFunctionInfo> binderFunctionInfo;

  private final LogManager logger;
  private final VariableSkipper varSkipper;

  private Map<CFANode, Map<GeneralIdentifier, DataType>> precision;
  // Not a set, as usage.equals do not consider id
  private List<UsageInfo> result;

  private Collection<CFANode> uselessNodes;
  private Collection<SingleIdentifier> redundantIds;

  StatTimer totalTimer = new StatTimer("Total time for usage processing");
  StatTimer usageTimer = new StatTimer("Time for usage extraction");
  StatTimer expressionTimer = new StatTimer("Time for expression parsing");
  StatTimer localTimer = new StatTimer("Time for sharedness check");
  StatTimer usageCreationTimer = new StatTimer("Time for usage creation");

  public UsageProcessor(
      Configuration config,
      LogManager pLogger,
      Map<CFANode, Map<GeneralIdentifier, DataType>> pPrecision,
      Map<String, BinderFunctionInfo> pBinderFunctionInfo)
      throws InvalidConfigurationException {
    logger = pLogger;
    binderFunctionInfo = pBinderFunctionInfo;

    varSkipper = new VariableSkipper(config);
    precision = pPrecision;
    uselessNodes = new IdentityHashSet<>();
  }

  public void updateRedundantUnsafes(Set<SingleIdentifier> set) {
    redundantIds = set;
  }

  public List<UsageInfo> getUsagesForState(AbstractState pState) {

    totalTimer.start();
    result = new ArrayList<>();

    ARGState argState = (ARGState) pState;
    CFANode node = AbstractStates.extractLocation(argState);

    if (uselessNodes.contains(node)) {
      totalTimer.stop();
      return result;
    }

    for (ARGState child : argState.getChildren()) {
      CFAEdge edge = argState.getEdgeToChild(child);
      if (edge != null) {
        usageTimer.start();
        getUsagesForEdge(pState, child, edge);
        usageTimer.stop();
      }
    }

    if (result.isEmpty()) {
      uselessNodes.add(node);
    }
    totalTimer.stop();
    return result;
  }

  public void getUsagesForEdge(AbstractState pParent, AbstractState pChild, CFAEdge pCfaEdge) {

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        {
          CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
        handleDeclaration(pParent, pChild, declEdge);
          break;
        }

        // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
        handleStatement(pParent, pChild, statementEdge.getStatement());
          break;
        }

      case AssumeEdge:
        {
        visitStatement(pParent, pChild, ((CAssumeEdge) pCfaEdge).getExpression(), Access.READ);
          break;
        }

      case FunctionCallEdge:
        {
        handleFunctionCall(pParent, pChild, (CFunctionCallEdge) pCfaEdge);
          break;
        }

      default:
        {
          break;
        }
    }
  }

  private void
      handleFunctionCall(AbstractState pParent, AbstractState pChild, CFunctionCallEdge edge) {
    CStatement statement = edge.getRawAST().get();

    if (statement instanceof CFunctionCallAssignmentStatement) {
      /*
       * a = f(b)
       */
      CFunctionCallExpression right =
          ((CFunctionCallAssignmentStatement) statement).getRightHandSide();
      CExpression variable = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();

      // expression - only name of function
      handleFunctionCallExpression(pParent, pChild, right);
      visitStatement(pParent, pChild, variable, Access.WRITE);

    } else if (statement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          pParent,
          pChild,
          ((CFunctionCallStatement) statement).getFunctionCallExpression());
    }
  }

  private void
      handleDeclaration(AbstractState pParent, AbstractState pChild, CDeclarationEdge declEdge) {

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
      visitStatement(pParent, pChild, initExpression, Access.READ);

      // We do not add usage for currently declared variable
      // It can not cause a race
    }
  }

  private void handleFunctionCallExpression(
      AbstractState pParent,
      AbstractState pChild,
      final CFunctionCallExpression fcExpression) {

    String functionCallName = fcExpression.getFunctionNameExpression().toASTString();

    if (binderFunctionInfo.containsKey(functionCallName)) {
      BinderFunctionInfo currentInfo = binderFunctionInfo.get(functionCallName);
      List<CExpression> params = fcExpression.getParameterExpressions();

      AbstractIdentifier id;

      for (int i = 0; i < params.size(); i++) {
        id = currentInfo.createParamenterIdentifier(params.get(i), i, getCurrentFunction(pChild));
        createUsageAndAdd(id, pParent, pChild, currentInfo.getBindedAccess(i));
      }

    } else {
      fcExpression.getParameterExpressions()
          .forEach(p -> visitStatement(pParent, pChild, p, Access.READ));
      visitStatement(pParent, pChild, fcExpression.getFunctionNameExpression(), Access.READ);
    }
  }

  private void
      handleStatement(AbstractState pParent, AbstractState pChild, final CStatement pStatement) {

    if (pStatement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) pStatement;
      CExpression left = assignment.getLeftHandSide();
      CRightHandSide right = assignment.getRightHandSide();

      if (right instanceof CExpression) {
        visitStatement(pParent, pChild, (CExpression) right, Access.READ);

      } else if (right instanceof CFunctionCallExpression) {
        handleFunctionCallExpression(pParent, pChild, (CFunctionCallExpression) right);
      }
      visitStatement(pParent, pChild, left, Access.WRITE);

    } else if (pStatement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          pParent,
          pChild,
          ((CFunctionCallStatement) pStatement).getFunctionCallExpression());

    } else if (pStatement instanceof CExpressionStatement) {
      visitStatement(
          pParent,
          pChild,
          ((CExpressionStatement) pStatement).getExpression(),
          Access.WRITE);
    }
  }

  private void visitStatement(
      AbstractState pParent,
      AbstractState pChild,
      final CExpression expression,
      final Access access) {
    expressionTimer.start();
    ExpressionHandler handler = new ExpressionHandler(access, getCurrentFunction(pChild));
    expression.accept(handler);
    expressionTimer.stop();

    for (Pair<AbstractIdentifier, Access> pair : handler.getProcessedExpressions()) {
      AbstractIdentifier id = pair.getFirst();
      createUsageAndAdd(id, pParent, pChild, pair.getSecond());
    }
  }

  private void createUsageAndAdd(
      AbstractIdentifier pId,
      AbstractState pParent,
      AbstractState pChild,
      Access pAccess) {

    usageCreationTimer.start();
    UsageState uState = UsageState.get(pParent);
    pId = uState.getLinksIfNecessary(pId);
    UsageInfo usage = UsageInfo.createUsageInfo(pAccess, pChild, pId);
    usageCreationTimer.stop();

    // Precise information, using results of shared analysis
    if (!usage.isRelevant()) {
      return;
    }

    SingleIdentifier singleId = usage.getId();

    if (redundantIds.contains(singleId)) {
      return;
    }

    CFANode node = AbstractStates.extractLocation(pParent);
    Map<GeneralIdentifier, DataType> localInfo = precision.get(node);

    if (localInfo != null) {
      localTimer.start();
      GeneralIdentifier gId = singleId.getGeneralId();
      if (localInfo.get(gId) == DataType.LOCAL) {
        logger.log(
            Level.FINER, singleId + " is considered to be local, so it wasn't add to statistics");
        localTimer.stop();
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
          localTimer.stop();
          return;
        }
      }
      localTimer.stop();
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

    if (singleId instanceof FunctionIdentifier) {
      return;
    }

    logger.log(Level.FINER, "Add " + usage + " to unsafe statistics");

    result.add(usage);
  }

  private String getCurrentFunction(AbstractState pState) {
    return AbstractStates.extractStateByType(pState, CallstackState.class).getCurrentFunction();
  }

  public void printStatistics(StatisticsWriter pWriter) {
    pWriter.put(totalTimer)
        .beginLevel()
        .put(usageTimer)
        .beginLevel()
        .put(expressionTimer)
        .put(usageCreationTimer)
        .put(localTimer)
        .endLevel()
        .endLevel();
  }
}
