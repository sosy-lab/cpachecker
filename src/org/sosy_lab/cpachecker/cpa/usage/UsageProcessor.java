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

import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import org.sosy_lab.cpachecker.cpa.rcucpa.RCUState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class UsageProcessor {

  private final Map<String, BinderFunctionInfo> binderFunctionInfo;

  private final LogManager logger;
  private final VariableSkipper varSkipper;

  private Map<CFANode, Map<GeneralIdentifier, DataType>> precision;
  private Map<CFAEdge, Collection<Pair<AbstractIdentifier, Access>>> usages;
  // Not a set, as usage.equals do not consider id
  private List<UsageInfo> result;

  private Collection<CFANode> uselessNodes;
  private Collection<SingleIdentifier> redundantIds;

  private final IdentifierCreator creator;

  StatTimer totalTimer = new StatTimer("Total time for usage processing");
  StatTimer localTimer = new StatTimer("Time for sharedness check");
  StatTimer usagePreparationTimer = new StatTimer("Time for usage preparation");
  StatTimer usageCreationTimer = new StatTimer("Time for usage creation");
  StatTimer searchingCacheTimer = new StatTimer("Time for searching in cache");

  public UsageProcessor(
      Configuration config,
      LogManager pLogger,
      Map<CFANode, Map<GeneralIdentifier, DataType>> pPrecision,
      Map<String, BinderFunctionInfo> pBinderFunctionInfo,
      IdentifierCreator pCreator)
      throws InvalidConfigurationException {
    logger = pLogger;
    binderFunctionInfo = pBinderFunctionInfo;

    varSkipper = new VariableSkipper(config);
    precision = pPrecision;
    uselessNodes = new IdentityHashSet<>();
    usages = new IdentityHashMap<>();
    creator = pCreator;
  }

  public void updateRedundantUnsafes(Set<SingleIdentifier> set) {
    redundantIds = set;
  }

  public List<UsageInfo> getUsagesForState(AbstractState pState) {

    totalTimer.start();
    result = new ArrayList<>();
    boolean resultComputed = false;

    ARGState argState = (ARGState) pState;
    CFANode node = AbstractStates.extractLocation(argState);

    if (uselessNodes.contains(node)) {
      totalTimer.stop();
      return result;
    }

    for (ARGState child : argState.getChildren()) {
      CFANode childNode = AbstractStates.extractLocation(child);

      if (node.hasEdgeTo(childNode)) {
        // Need the flag to avoid missed cases with applied edges: it may be traversed first,
        // so put to useless nodes ONLY if there is a normal CFA edge.
        resultComputed = true;
        CFAEdge edge = node.getEdgeTo(childNode);
        Collection<Pair<AbstractIdentifier, Access>> ids;

        if (usages.containsKey(edge)) {
          ids = usages.get(edge);
        } else {
          ids = getUsagesForEdge(child, edge);
          if (!ids.isEmpty()) {
            usages.put(edge, ids);
          }
        }

        usagePreparationTimer.start();
        for (Pair<AbstractIdentifier, Access> pair : ids) {
          AbstractIdentifier id = pair.getFirst();
          UsageState uState = UsageState.get(pState);
          id = uState.getLinksIfNecessary(id);

          searchingCacheTimer.start();
          boolean b = redundantIds.contains(id);
          searchingCacheTimer.stop();
          if (b) {
            continue;
          }
          createUsages(id, node, child, pair.getSecond());
        }
        usagePreparationTimer.stop();

      } else {
        // No edge, for example, due to BAM
        // Note, function call edge was already handled, we do not miss it
        continue;
      }
    }

    if (resultComputed && result.isEmpty()) {
      uselessNodes.add(node);
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge e = node.getLeavingEdge(i);
        usages.remove(e);
      }
    }
    totalTimer.stop();
    return result;
  }

  public Collection<Pair<AbstractIdentifier, Access>>
      getUsagesForEdge(AbstractState pChild, CFAEdge pCfaEdge) {

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        {
          CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
        return handleDeclaration(pChild, declEdge);
        }

        // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
        return handleStatement(pChild, statementEdge.getStatement());
        }

      case AssumeEdge:
        {
        return visitStatement(
            pChild,
            ((CAssumeEdge) pCfaEdge).getExpression(),
            Access.READ);
        }

      case FunctionCallEdge:
        {
        return handleFunctionCall(pChild, (CFunctionCallEdge) pCfaEdge);
        }

      default:
        {
        return Collections.emptySet();
        }
    }
  }

  private Collection<Pair<AbstractIdentifier, Access>>
      handleFunctionCall(AbstractState pChild, CFunctionCallEdge edge) {
    CStatement statement = edge.getRawAST().get();

    if (statement instanceof CFunctionCallAssignmentStatement) {
      /*
       * a = f(b)
       */
      CFunctionCallExpression right =
          ((CFunctionCallAssignmentStatement) statement).getRightHandSide();
      CExpression variable = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();

      List<Pair<AbstractIdentifier, Access>> internalIds = new ArrayList<>();
      internalIds.addAll(handleFunctionCallExpression(pChild, right));
      internalIds.addAll(visitStatement(pChild, variable, Access.WRITE));
      return internalIds;

    } else if (statement instanceof CFunctionCallStatement) {
      return handleFunctionCallExpression(
          pChild,
          ((CFunctionCallStatement) statement).getFunctionCallExpression());
    }
    return Collections.emptySet();
  }

  private Collection<Pair<AbstractIdentifier, Access>>
      handleDeclaration(AbstractState pChild, CDeclarationEdge declEdge) {

    if (declEdge.getDeclaration().getClass() != CVariableDeclaration.class) {
      // not a variable declaration
      return Collections.emptySet();
    }
    CVariableDeclaration decl = (CVariableDeclaration) declEdge.getDeclaration();

    if (decl.isGlobal()) {
      return Collections.emptySet();
    }

    CInitializer init = decl.getInitializer();

    if (init == null) {
      // no assignment
      return Collections.emptySet();
    }

    if (init instanceof CInitializerExpression) {
      CExpression initExpression = ((CInitializerExpression) init).getExpression();
      // Use EdgeType assignment for initializer expression to avoid mistakes related to expressions
      // "int CPACHECKER_TMP_0 = global;"
      return visitStatement(pChild, initExpression, Access.READ);

      // We do not add usage for currently declared variable
      // It can not cause a race
    }
    return Collections.emptySet();
  }

  private Collection<Pair<AbstractIdentifier, Access>> handleFunctionCallExpression(
      AbstractState pChild,
      final CFunctionCallExpression fcExpression) {

    String functionCallName = fcExpression.getFunctionNameExpression().toASTString();

    List<Pair<AbstractIdentifier, Access>> internalIds = new ArrayList<>();

    if (binderFunctionInfo.containsKey(functionCallName)) {
      BinderFunctionInfo currentInfo = binderFunctionInfo.get(functionCallName);
      List<CExpression> params = fcExpression.getParameterExpressions();

      AbstractIdentifier id;

      for (int i = 0; i < params.size(); i++) {
        id = currentInfo.createParamenterIdentifier(params.get(i), i, getCurrentFunction(pChild));
        internalIds.add(Pair.of(id, currentInfo.getBindedAccess(i)));
      }
    } else {

      fcExpression.getParameterExpressions()
          .forEach(p -> internalIds.addAll(visitStatement(pChild, p, Access.READ)));
      internalIds
          .addAll(visitStatement(pChild, fcExpression.getFunctionNameExpression(), Access.READ));
    }
    return internalIds;
  }

  private Collection<Pair<AbstractIdentifier, Access>>
      handleStatement(AbstractState pChild, final CStatement pStatement) {

    if (pStatement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) pStatement;
      CExpression left = assignment.getLeftHandSide();
      CRightHandSide right = assignment.getRightHandSide();
      List<Pair<AbstractIdentifier, Access>> internalIds = new ArrayList<>();

      if (right instanceof CExpression) {
        internalIds.addAll(visitStatement(pChild, (CExpression) right, Access.READ));

      } else if (right instanceof CFunctionCallExpression) {
        internalIds.addAll(handleFunctionCallExpression(pChild, (CFunctionCallExpression) right));
      }
      internalIds.addAll(visitStatement(pChild, left, Access.WRITE));
      return internalIds;

    } else if (pStatement instanceof CFunctionCallStatement) {
      return handleFunctionCallExpression(
          pChild,
          ((CFunctionCallStatement) pStatement).getFunctionCallExpression());

    } else if (pStatement instanceof CExpressionStatement) {
      return visitStatement(
          pChild,
          ((CExpressionStatement) pStatement).getExpression(),
          Access.WRITE);
    }
    return Collections.emptySet();
  }

  private Collection<Pair<AbstractIdentifier, Access>>
      visitStatement(
      AbstractState pChild,
      final CExpression expression,
      final Access access) {
    String fName = getCurrentFunction(pChild);
    ExpressionHandler handler =
        new ExpressionHandler(access, fName, varSkipper, getIdentifierCreator(fName));
    expression.accept(handler);

    return handler.getProcessedExpressions();
  }

  private void
      createUsages(AbstractIdentifier pId, CFANode pNode, AbstractState pChild, Access pAccess) {

    // TODO looks like a hack
    if (pId instanceof SingleIdentifier) {
      SingleIdentifier singleId = (SingleIdentifier) pId;
      if (singleId.getName().contains("CPAchecker_TMP")) {
        RCUState rcuState = AbstractStates.extractStateByType(pChild, RCUState.class);
        if (rcuState != null) {
          singleId = (SingleIdentifier) rcuState.getNonTemporaryId(singleId);
        }
      }
    }

    Iterable<AliasInfoProvider> providers =
        AbstractStates.asIterable(pChild).filter(AliasInfoProvider.class);
    Set<AbstractIdentifier> aliases = new TreeSet<>();

    aliases.add(pId);
    for (AliasInfoProvider provider : providers) {
      aliases.addAll(provider.getAllPossibleAliases(pId));
    }

    for (AliasInfoProvider provider : providers) {
      provider.filterAliases(pId, aliases);
    }

    for (AbstractIdentifier aliasId : aliases) {
      createUsageAndAdd(aliasId, pNode, pChild, pAccess);
    }
  }

  private void createUsageAndAdd(
      AbstractIdentifier pId,
      CFANode pNode,
      AbstractState pChild,
      Access pAccess) {

    if (pId instanceof LocalVariableIdentifier && !pId.isDereferenced()) {
      return;
    }

    usageCreationTimer.start();
    UsageInfo usage = UsageInfo.createUsageInfo(pAccess, pChild, pId);
    usageCreationTimer.stop();

    // Precise information, using results of shared analysis
    if (!usage.isRelevant()) {
      return;
    }

    localTimer.start();
    Map<GeneralIdentifier, DataType> localInfo = precision.get(pNode);

    if (localInfo != null && localInfo.containsValue(DataType.LOCAL)) {
      SingleIdentifier singleId = usage.getId();
      GeneralIdentifier gId = singleId.getGeneralId();
      if (localInfo.get(gId) == DataType.LOCAL) {
        logger.log(
            Level.FINER, singleId + " is considered to be local, so it wasn't add to statistics");
        localTimer.stop();
        return;
      } else {

        Iterable<LocalInfoProvider> itStates =
            AbstractStates.asIterable(pChild).filter(LocalInfoProvider.class);
        boolean isLocal = false;
        boolean isGlobal = false;

        for (AbstractIdentifier id : singleId.getComposedIdentifiers()) {
          if (id instanceof SingleIdentifier) {
            GeneralIdentifier gcId = ((SingleIdentifier) id).getGeneralId();
            DataType type = localInfo.get(gcId);
            if (type == DataType.GLOBAL) {
              // Add global var to statistics in any case
              isGlobal = true;
              break;
            } else if (type == DataType.LOCAL) {
              isLocal = true;
            }
            for (LocalInfoProvider state : itStates) {
              isLocal |= state.isLocal(gcId);
            }
          }
        }
        for (LocalInfoProvider state : itStates) {
          isLocal |= state.isLocal(gId);
        }
        if (isLocal && !isGlobal) {
          logger.log(
              Level.FINER, singleId + " is supposed to be local, so it wasn't add to statistics");
          localTimer.stop();
          return;
        }
      }
    }

    localTimer.stop();
    logger.log(Level.FINER, "Add " + usage + " to unsafe statistics");

    result.add(usage);
  }

  private String getCurrentFunction(AbstractState pState) {
    return AbstractStates.extractStateByType(pState, CallstackState.class).getCurrentFunction();
  }

  public void printStatistics(StatisticsWriter pWriter) {
    pWriter.put(totalTimer)
        .beginLevel()
        .put(usagePreparationTimer)
        .beginLevel()
        .put(searchingCacheTimer)
        .put(usageCreationTimer)
        .put(localTimer)
        .endLevel()
        .put("Number of useless nodes", uselessNodes.size())
        .put("Number of cached edges", usages.size());
  }

  private IdentifierCreator getIdentifierCreator(String functionName) {
    creator.setCurrentFunction(functionName);
    return creator;
  }
}
