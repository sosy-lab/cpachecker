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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.cpa.usage.BinderFunctionInfo.LinkerInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.Identifiers;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;

@Options(prefix = "cpa.usage")
public class UsageTransferRelation implements TransferRelation {

  private final TransferRelation wrappedTransfer;
  private final UsageCPAStatistics statistics;

  @Option(description = "functions, which we don't analize", secure = true)
  private Set<String> skippedfunctions = null;

  @Option(
    description =
        "functions, which are used to bind variables (like list elements are binded to list variable)",
    secure = true
  )
  private Set<String> binderFunctions = null;

  @Option(name = "abortfunctions", description = "functions, which stops analysis", secure = true)
  private Set<String> abortfunctions;

  private final CallstackTransferRelation callstackTransfer;
  private final VariableSkipper varSkipper;

  private Map<String, BinderFunctionInfo> binderFunctionInfo;
  private final LogManager logger;

  private UsageState newState;
  private UsagePrecision precision;

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
    if (binderFunctions != null) {
      binderFunctionInfo =
          from(binderFunctions).toMap(name -> new BinderFunctionInfo(name, config, logger));
      // BindedFunctions should not be analysed
      skippedfunctions =
          skippedfunctions == null
              ? binderFunctions
              : Sets.union(skippedfunctions, binderFunctions);
    }

    varSkipper = new VariableSkipper(config);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws InterruptedException, CPATransferException {

    Collection<AbstractState> results;
    assert (pPrecision instanceof UsagePrecision);

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

    /*if (oldState.isExitState()) {
      statistics.transferRelationTimer.stop();
      return Collections.emptySet();
    }*/

    boolean needToReset = false;
    if (shouldBeSkipped(pCfaEdge)) {
      callstackTransfer.enableRecursiveContext();
      needToReset = true;
      currentEdge = ((FunctionCallEdge) pCfaEdge).getSummaryEdge();
      Preconditions.checkNotNull(
          currentEdge, "Cannot find summary edge for " + pCfaEdge + " as skipped function");
      logger.log(Level.FINEST, pCfaEdge.getSuccessor().getFunctionName() + " is skipped");
    }

    AbstractState oldWrappedState = oldState.getWrappedState();
    newState = oldState.copy();
    precision = (UsagePrecision) pPrecision;
    handleEdge(pCfaEdge);

    Collection<? extends AbstractState> newWrappedStates =
        wrappedTransfer.getAbstractSuccessorsForEdge(
            oldWrappedState, precision.getWrappedPrecision(), currentEdge);

    // Do not know why, but replacing the loop into lambda greatly decreases the speed
    for (AbstractState newWrappedState : newWrappedStates) {
      UsageState resultState = newState.copy(newWrappedState);
      if (resultState != null) {
        result.add(resultState);
      }
    }

    if (needToReset) {
      callstackTransfer.disableRecursiveContext();
    }
    statistics.transferRelationTimer.stop();
    return result;
  }

  private boolean shouldBeSkipped(CFAEdge pCfaEdge) {
    if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      String functionName = ((FunctionCallEdge) pCfaEdge).getSuccessor().getFunctionName();
      if (skippedfunctions != null && skippedfunctions.contains(functionName)) {
        return true;
      }
    }
    return false;
  }

  private void handleEdge(CFAEdge pCfaEdge) throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        {
          CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
          handleDeclaration(declEdge);
          break;
        }

        // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
          handleStatement(statementEdge.getStatement());
          break;
        }

      case AssumeEdge:
        {
          visitStatement(((CAssumeEdge) pCfaEdge).getExpression(), Access.READ);
          break;
        }

      case FunctionCallEdge:
        {
          handleFunctionCall((CFunctionCallEdge) pCfaEdge);
          break;
        }

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

      visitStatement(variable, Access.WRITE);
      // expression - only name of function
      handleFunctionCallExpression(variable, right);

    } else if (statement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          null, ((CFunctionCallStatement) statement).getFunctionCallExpression());

    } else {
      throw new HandleCodeException("No function found");
    }
  }

  private void handleDeclaration(CDeclarationEdge declEdge) {

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
      visitStatement(initExpression, Access.READ);

      // We do not add usage for currently declared variable
      // It can not cause a race
    }
  }

  private void handleFunctionCallExpression(
      final CExpression left, final CFunctionCallExpression fcExpression) {

    String functionCallName = fcExpression.getFunctionNameExpression().toASTString();
    if (binderFunctions != null && binderFunctions.contains(functionCallName)) {
      BinderFunctionInfo currentInfo = binderFunctionInfo.get(functionCallName);
      List<CExpression> params = fcExpression.getParameterExpressions();

      assert params.size() == currentInfo.parameters;

      linkVariables(left, params, currentInfo.linkInfo);

      AbstractIdentifier id;

      for (int i = 0; i < params.size(); i++) {
        id =
            Identifiers.createIdentifier(
                params.get(i), currentInfo.pInfo.get(i).dereference, getCurrentFunction());
        id = newState.getLinksIfNecessary(id);
        UsageInfo usage =
            UsageInfo.createUsageInfo(
                currentInfo.pInfo.get(i).access,
                fcExpression.getFileLocation().getStartingLineNumber(),
                newState,
                id);
        addUsageIfNeccessary(usage);
      }

    } else if (abortfunctions.contains(functionCallName)) {
      newState.asExitable();
    } else {
      fcExpression.getParameterExpressions().forEach(p -> visitStatement(p, Access.READ));
    }
  }

  private void handleStatement(final CStatement pStatement) throws HandleCodeException {

    if (pStatement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) pStatement;
      CExpression left = assignment.getLeftHandSide();
      CRightHandSide right = assignment.getRightHandSide();

      visitStatement(left, Access.WRITE);

      if (right instanceof CExpression) {
        visitStatement((CExpression) right, Access.READ);

      } else if (right instanceof CFunctionCallExpression) {
        handleFunctionCallExpression(left, (CFunctionCallExpression) right);

      } else {
        throw new HandleCodeException(
            "Unrecognised type of right side of assignment: " + assignment.toASTString());
      }

    } else if (pStatement instanceof CFunctionCallStatement) {
      handleFunctionCallExpression(
          null, ((CFunctionCallStatement) pStatement).getFunctionCallExpression());

    } else if (pStatement instanceof CExpressionStatement) {
      visitStatement(((CExpressionStatement) pStatement).getExpression(), Access.WRITE);

    } else {
      throw new HandleCodeException("Unrecognized statement: " + pStatement.toASTString());
    }
  }

  private void linkVariables(
      final CExpression left,
      final List<CExpression> params,
      final Pair<LinkerInfo, LinkerInfo> linkInfo) {
    AbstractIdentifier leftId, rightId;

    if (linkInfo != null) {
      // Sometimes these functions are used not only for linkings.
      // For example, sdlGetFirst also deletes element.
      // So, if we can't link (no left side), we skip it
      leftId = getLinkedIdentifier(linkInfo.getFirst(), left, params);
      rightId = getLinkedIdentifier(linkInfo.getSecond(), left, params);
      linkId(leftId, rightId);
    }
  }

  private AbstractIdentifier getLinkedIdentifier(
      final LinkerInfo info, final CExpression left, final List<CExpression> params) {
    CExpression expr;
    if (info.num == 0 && left != null) {
      expr = left;
    } else if (info.num > 0) {
      expr = params.get(info.num - 1);
    } else {
      /* f.e. sdlGetFirst(), which is used for deleting element
       * we don't link, but it isn't an error
       */
      return null;
    }
    return Identifiers.createIdentifier(expr, info.dereference, getCurrentFunction());
  }

  private void linkId(final AbstractIdentifier idIn, AbstractIdentifier idFrom) {
    if (idIn == null || idFrom == null) {
      return;
    }
    if (newState.containsLinks(idFrom)) {
      idFrom = newState.getLinksIfNecessary(idFrom);
    }
    logger.log(Level.FINEST, "Link " + idIn + " and " + idFrom);
    newState.put(idIn, idFrom);
  }

  private void visitStatement(final CExpression expression, final Access access) {
    ExpressionHandler handler = new ExpressionHandler(access, getCurrentFunction());
    expression.accept(handler);

    for (Pair<AbstractIdentifier, Access> pair : handler.getProcessedExpressions()) {
      AbstractIdentifier id = pair.getFirst();
      id = newState.getLinksIfNecessary(id);
      UsageInfo usage =
          UsageInfo.createUsageInfo(
              pair.getSecond(), expression.getFileLocation().getStartingLineNumber(), newState, id);
      addUsageIfNeccessary(usage);
    }
  }

  private void addUsageIfNeccessary(UsageInfo usage) {

    // Precise information, using results of shared analysis
    if (!usage.isRelevant()) {
      return;
    }

    SingleIdentifier singleId = usage.getId();

    CFANode node = AbstractStates.extractLocation(newState);
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

    if (varSkipper.shouldBeSkipped(singleId, usage)) {
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
    if (singleId instanceof StructureIdentifier) {
      singleId = ((StructureIdentifier) singleId).toStructureFieldIdentifier();
    }

    logger.log(Level.FINER, "Add " + usage + " to unsafe statistics");

    newState.addUsage(singleId, usage);
  }

  private String getCurrentFunction() {
    return AbstractStates.extractStateByType(newState, CallstackState.class).getCurrentFunction();
  }
}
