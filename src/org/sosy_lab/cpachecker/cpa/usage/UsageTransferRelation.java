// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperTransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;

@Options(prefix = "cpa.usage")
public class UsageTransferRelation extends AbstractSingleWrapperTransferRelation {

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
  private final IdentifierCreator creator;

  private final LogManager logger;

  public UsageTransferRelation(
      TransferRelation pWrappedTransfer,
      Configuration config,
      LogManager pLogger,
      UsageCPAStatistics s,
      IdentifierCreator c)
      throws InvalidConfigurationException {
    super(pWrappedTransfer);
    config.inject(this, UsageTransferRelation.class);
    callstackTransfer =
        ((WrapperTransferRelation) transferRelation)
            .retrieveWrappedTransferRelation(CallstackTransferRelation.class);
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
    skippedfunctions = new TreeSet<>(Sets.union(skippedfunctions, binderFunctions));
    creator = c;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws InterruptedException, CPATransferException {

    Collection<AbstractState> results;

    statistics.transferRelationTimer.start();
    CFANode node = extractLocation(pElement);
    results = new ArrayList<>(node.getNumLeavingEdges());

    AbstractStateWithLocations locState =
        extractStateByType(pElement, AbstractStateWithLocations.class);

    if (locState instanceof AbstractStateWithEdge) {
      AbstractEdge edge = ((AbstractStateWithEdge) locState).getAbstractEdge();
      if (edge instanceof WrapperCFAEdge) {
        results.addAll(
            getAbstractSuccessorsForEdge(
                pElement,
                pPrecision,
                ((WrapperCFAEdge) edge).getCFAEdge()));
      } else {
        results.addAll(getAbstractSuccessorForAbstractEdge(pElement, pPrecision));
      }
    } else {
      for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
        CFAEdge edge = node.getLeavingEdge(edgeIdx);
        results.addAll(getAbstractSuccessorsForEdge(pElement, pPrecision, edge));
      }
    }

    for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
      CFAEdge edge = node.getLeavingEdge(edgeIdx);
      results.addAll(getAbstractSuccessorsForEdge(pElement, pPrecision, edge));
    }

    statistics.transferRelationTimer.stop();
    return results;
  }

  private Collection<? extends AbstractState>
      getAbstractSuccessorForAbstractEdge(AbstractState pElement, Precision pPrecision)
          throws CPATransferException, InterruptedException {

    UsageState oldState = (UsageState) pElement;
    AbstractState oldWrappedState = oldState.getWrappedState();
    statistics.innerAnalysisTimer.start();
    Collection<? extends AbstractState> newWrappedStates =
        transferRelation.getAbstractSuccessors(oldWrappedState, pPrecision);
    statistics.innerAnalysisTimer.stop();

    Collection<AbstractState> result = new ArrayList<>();
    for (AbstractState newWrappedState : newWrappedStates) {
      UsageState newState = oldState.copy(newWrappedState);
      result.add(newState);
    }
    return result;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    statistics.transferForEdgeTimer.start();
    Collection<AbstractState> result = new ArrayList<>();

    UsageState oldState = (UsageState) pState;

    statistics.checkForSkipTimer.start();
    CFAEdge currentEdge = changeIfNeccessary(pCfaEdge);
    statistics.checkForSkipTimer.stop();

    if (currentEdge == null) {
      // Abort function
      statistics.transferForEdgeTimer.stop();
      return Collections.emptySet();
    }

    statistics.bindingTimer.start();
    creator.setCurrentFunction(getCurrentFunction(oldState));
    Optional<Pair<AbstractIdentifier, AbstractIdentifier>> newLinks =
        handleEdge(currentEdge);
    statistics.bindingTimer.stop();

    statistics.innerAnalysisTimer.start();
    Collection<? extends AbstractState> newWrappedStates =
        transferRelation
            .getAbstractSuccessorsForEdge(oldState.getWrappedState(), pPrecision, currentEdge);
    statistics.innerAnalysisTimer.stop();

    // Do not know why, but replacing the loop into lambda greatly decreases the speed
    for (AbstractState newWrappedState : newWrappedStates) {
      UsageState newState = oldState.copy(newWrappedState);
      if (newLinks.isPresent()) {
        Pair<AbstractIdentifier, AbstractIdentifier> pair = newLinks.get();
        logger.log(Level.FINEST, "Link " + pair.getFirst() + " and " + pair.getSecond());
        newState = newState.put(pair.getFirst(), pair.getSecond());
      }
      result.add(newState);
    }

    if (currentEdge != pCfaEdge) {
      callstackTransfer.disableRecursiveContext();
    }
    statistics.transferForEdgeTimer.stop();
    return ImmutableList.copyOf(result);
  }

  private CFAEdge changeIfNeccessary(CFAEdge pCfaEdge) {
    if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      String functionName = pCfaEdge.getSuccessor().getFunctionName();

      if (abortFunctions.contains(functionName)) {
        return null;
      } else if (skippedfunctions.contains(functionName)) {
        CFAEdge newEdge = ((FunctionCallEdge) pCfaEdge).getSummaryEdge();
        logger.log(Level.FINEST, functionName + " will be skipped");
        callstackTransfer.enableRecursiveContext();
        return newEdge;
      }
    }
    return pCfaEdge;
  }

  private Optional<Pair<AbstractIdentifier, AbstractIdentifier>>
      handleEdge(CFAEdge pCfaEdge) throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
        return handleStatement(statementEdge.getStatement());
        }

      case FunctionCallEdge:
        {
        CFunctionCall statement = ((CFunctionCallEdge) pCfaEdge).getRawAST().get();
        return handleStatement(statement);
        }

      case AssumeEdge:
      case DeclarationEdge:
      case FunctionReturnEdge:
      case ReturnStatementEdge:
      case BlankEdge:
      case CallToReturnEdge:
        {
        return Optional.empty();
        }

      default:
        throw new UnrecognizedCFAEdgeException(pCfaEdge);
    }
  }

  private Optional<Pair<AbstractIdentifier, AbstractIdentifier>>
      handleStatement(final CStatement pStatement) {

    if (pStatement instanceof CFunctionCallAssignmentStatement) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) pStatement;
      CFunctionCallExpression right =
          ((CFunctionCallAssignmentStatement) pStatement).getRightHandSide();
      CExpression left = assignment.getLeftHandSide();
      return handleFunctionCallExpression(left, right);

    } else if (pStatement instanceof CFunctionCallStatement) {
      return handleFunctionCallExpression(
          null, ((CFunctionCallStatement) pStatement).getFunctionCallExpression());

    }
    return Optional.empty();
  }

  private Optional<Pair<AbstractIdentifier, AbstractIdentifier>> handleFunctionCallExpression(
      final CExpression left,
      final CFunctionCallExpression fcExpression) {

    String functionCallName = fcExpression.getFunctionNameExpression().toASTString();
    if (binderFunctionInfo.containsKey(functionCallName)) {
      BinderFunctionInfo bInfo = binderFunctionInfo.get(functionCallName);

      if (bInfo.shouldBeLinked()) {
        List<CExpression> params = fcExpression.getParameterExpressions();
        // Sometimes these functions are used not only for linkings.
        // For example, getListElement also deletes element.
        // So, if we can't link (no left side), we skip it
        return bInfo.constructIdentifiers(left, params, creator);
      }
    }
    return Optional.empty();
  }

  private String getCurrentFunction(UsageState newState) {
    return AbstractStates.extractStateByType(newState, CallstackState.class).getCurrentFunction();
  }

  Map<String, BinderFunctionInfo> getBinderFunctionInfo() {
    return binderFunctionInfo;
  }
}
