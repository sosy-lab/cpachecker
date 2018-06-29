/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;

@Options(prefix = "cpa.usage")
public class UsageTransferRelation implements TransferRelation {

  private final TransferRelation wrappedTransfer;
  private final UsageCPAStatistics statistics;

  @Option(description = "functions, which we don't analize", secure = true)
  private Set<String> skippedfunctions = ImmutableSet.of();

  @Option(
    description =
        "functions, which are used to bind variables (like list elements are binded to list variable)",
    secure = true
  )
  private Set<String> binderFunctions = ImmutableSet.of();

  @Option(description = "functions, which are marked as write access",
      secure = true)
  private Set<String> writeAccessFunctions = ImmutableSet.of();

  @Option(name = "abortfunctions", description = "functions, which stops analysis", secure = true)
  private Set<String> abortFunctions = ImmutableSet.of();

  private final CallstackTransferRelation callstackTransfer;

  private final Map<String, BinderFunctionInfo> binderFunctionInfo;

  private final LogManager logger;

  private UsageState newState;

  public UsageTransferRelation(
      TransferRelation pWrappedTransfer,
      Configuration config,
      LogManager pLogger,
      UsageCPAStatistics s,
      CallstackTransferRelation transfer)
      throws InvalidConfigurationException {
    config.inject(this);
    wrappedTransfer = pWrappedTransfer;
    callstackTransfer = transfer;
    statistics = s;
    logger = pLogger;

    ImmutableMap.Builder<String, BinderFunctionInfo> binderFunctionInfoBuilder =
        ImmutableMap.builder();

    from(binderFunctions)
        .forEach(
            name ->
                binderFunctionInfoBuilder.put(name, new BinderFunctionInfo(name, config, logger)));

    BinderFunctionInfo dummy = new BinderFunctionInfo();
    from(writeAccessFunctions).forEach(name -> binderFunctionInfoBuilder.put(name, dummy));
    binderFunctionInfo = binderFunctionInfoBuilder.build();

    // BindedFunctions should not be analysed
    skippedfunctions = Sets.union(skippedfunctions, binderFunctions);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws InterruptedException, CPATransferException {

    Collection<AbstractState> results;

    CFANode node = extractLocation(pElement);
    results = new ArrayList<>(node.getNumLeavingEdges());

    for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
      CFAEdge edge = node.getLeavingEdge(edgeIdx);
      results.addAll(getAbstractSuccessorsForEdge(pElement, pPrecision, edge));
    }
    return results;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    statistics.transferRelationTimer.start();
    Collection<AbstractState> result = new ArrayList<>();
    CFAEdge currentEdge = pCfaEdge;
    UsageState oldState = (UsageState) pState;

    if (oldState.isExitState()) {
      statistics.transferRelationTimer.stop();
      return Collections.emptySet();
    }

    currentEdge = changeIfNeccessary(pCfaEdge);

    AbstractState oldWrappedState = oldState.getWrappedState();
    newState = oldState.copy();
    statistics.usagePreparationTimer.start();
    handleEdge(currentEdge);
    statistics.usagePreparationTimer.stop();

    statistics.innerAnalysisTimer.start();
    Collection<? extends AbstractState> newWrappedStates =
        wrappedTransfer.getAbstractSuccessorsForEdge(oldWrappedState, pPrecision, currentEdge);
    statistics.innerAnalysisTimer.stop();

    // Do not know why, but replacing the loop into lambda greatly decreases the speed
    for (AbstractState newWrappedState : newWrappedStates) {
      result.add(newState.copy(newWrappedState));
    }

    if (currentEdge != pCfaEdge) {
      callstackTransfer.disableRecursiveContext();
    }
    newState = null;
    statistics.transferRelationTimer.stop();
    return result;
  }

  private CFAEdge changeIfNeccessary(CFAEdge pCfaEdge) {
    if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      String functionName = pCfaEdge.getSuccessor().getFunctionName();
      if (skippedfunctions.contains(functionName)) {
        CFAEdge newEdge = ((FunctionCallEdge) pCfaEdge).getSummaryEdge();
        Preconditions.checkNotNull(
            newEdge, "Cannot find summary edge for " + pCfaEdge + " as skipped function");
        logger.log(Level.FINEST, pCfaEdge.getSuccessor().getFunctionName() + " is skipped");
        callstackTransfer.enableRecursiveContext();
        return newEdge;
      }
    }
    return pCfaEdge;
  }

  private void handleEdge(CFAEdge pCfaEdge) throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {

        // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
          handleStatement(statementEdge.getStatement());
          break;
        }

      case FunctionCallEdge:
        {
          handleFunctionCall((CFunctionCallEdge) pCfaEdge);
          break;
        }

      case AssumeEdge:
      case DeclarationEdge:
      case FunctionReturnEdge:
      case ReturnStatementEdge:
      case BlankEdge:
      case CallToReturnEdge:
        {
          break;
        }

      default:
        throw new UnrecognizedCFAEdgeException(pCfaEdge);
    }
  }

  private void handleFunctionCall(CFunctionCallEdge edge) throws HandleCodeException {
    CStatement statement = edge.getRawAST().get();

    if (statement instanceof CFunctionCallAssignmentStatement) {
      /*
       * a = f(b)
       */
      CFunctionCallExpression right =
          ((CFunctionCallAssignmentStatement) statement).getRightHandSide();
      CExpression variable = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();

      // expression - only name of function
      handleFunctionCallExpression(variable, right);

    } else if (statement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          null, ((CFunctionCallStatement) statement).getFunctionCallExpression());

    } else {
      throw new HandleCodeException("No function found");
    }
  }

  private void handleFunctionCallExpression(
      final CExpression left, final CFunctionCallExpression fcExpression) {

    String functionCallName = fcExpression.getFunctionNameExpression().toASTString();
    if (binderFunctionInfo.containsKey(functionCallName)) {
      BinderFunctionInfo currentInfo = binderFunctionInfo.get(functionCallName);
      List<CExpression> params = fcExpression.getParameterExpressions();

      linkVariables(left, params, currentInfo);

    } else if (abortFunctions.contains(functionCallName)) {
      newState.asExitable();
    }
  }

  private void handleStatement(final CStatement pStatement) {

    if (pStatement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) pStatement;
      CExpression left = assignment.getLeftHandSide();
      CRightHandSide right = assignment.getRightHandSide();

      if (right instanceof CFunctionCallExpression) {
        handleFunctionCallExpression(left, (CFunctionCallExpression) right);
      }

    } else if (pStatement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          null, ((CFunctionCallStatement) pStatement).getFunctionCallExpression());

    }
  }

  private void linkVariables(
      final CExpression left, final List<CExpression> params, final BinderFunctionInfo bInfo) {

    if (bInfo.shouldBeLinked()) {
      // Sometimes these functions are used not only for linkings.
      // For example, sdlGetFirst also deletes element.
      // So, if we can't link (no left side), we skip it
      AbstractIdentifier idIn, idFrom;
      idIn = bInfo.constructFirstIdentifier(left, params, getCurrentFunction());
      idFrom = bInfo.constructSecondIdentifier(left, params, getCurrentFunction());
      if (idIn == null || idFrom == null) {
        return;
      }
      if (newState.containsLinks(idFrom)) {
        idFrom = newState.getLinksIfNecessary(idFrom);
      }
      logger.log(Level.FINEST, "Link " + idIn + " and " + idFrom);
      newState.put(idIn, idFrom);
    }
  }

  private String getCurrentFunction() {
    return AbstractStates.extractStateByType(newState, CallstackState.class).getCurrentFunction();
  }

  public Map<String, BinderFunctionInfo> getBinderFunctionInfo() {
    return binderFunctionInfo;
  }
}
