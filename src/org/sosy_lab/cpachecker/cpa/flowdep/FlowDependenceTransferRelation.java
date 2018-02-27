/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.flowdep;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.UsedIdsCollector;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Transfer relation of {@link FlowDependenceCPA}.
 */
class FlowDependenceTransferRelation
    extends SingleEdgeTransferRelation {

  private final ReachingDefTransferRelation reachDefRelation;
  private final UsesCollector usesCollector;

  private final LogManager logger;

  FlowDependenceTransferRelation(
      ReachingDefTransferRelation pReachDefRelation,
      LogManager pLogger) {
    reachDefRelation = pReachDefRelation;
    usesCollector = new UsesCollector();

    logger = pLogger;
  }

  private Map<MemoryLocation, Collection<ProgramDefinitionPoint>>
      normalizeReachingDefinitions(ReachingDefState pState, String pFunctionName) {

    Map<MemoryLocation, Collection<ProgramDefinitionPoint>> normalized = new HashMap<>();

    normalized.putAll(normalize(pState.getLocalReachingDefinitions(), pFunctionName));
    normalized.putAll(normalize(pState.getGlobalReachingDefinitions(), null));
    return normalized;
  }

  private Map<MemoryLocation, Collection<ProgramDefinitionPoint>>
      normalize(Map<String, Set<DefinitionPoint>> pDefs, @Nullable String pFunctionName) {

    Map<MemoryLocation, Collection<ProgramDefinitionPoint>> normalized = new HashMap<>();
    for (Map.Entry<String, Set<DefinitionPoint>> e : pDefs.entrySet()) {
      String varName = e.getKey();
      Set<DefinitionPoint> points = e.getValue();

      MemoryLocation var;
      if (pFunctionName != null) {
        var = MemoryLocation.valueOf(pFunctionName, varName);
      } else {
        var = MemoryLocation.valueOf(varName);
      }

      Collection<ProgramDefinitionPoint> defPoints =
          points.stream()
              .filter((DefinitionPoint x) -> (x instanceof ProgramDefinitionPoint))
              .map((DefinitionPoint p) -> (ProgramDefinitionPoint) p)
              .collect(Collectors.toList());

      normalized.put(var, defPoints);
    }
    return normalized;
  }

  /**
   * Returns a new FlowDependenceState for the declaration represented by the given
   * {@link CVariableDeclaration} object. Since the wrapped
   * {@link org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefCPA ReachingDefCPA} tracks new
   * definitions of variables, we only have to consider the use of variables in the initializer that
   * may exist.
   */
  private FlowDependenceState handleDeclarationEdge(
      CDeclarationEdge pCfaEdge,
      CVariableDeclaration pDecl,
      FlowDependenceState pNextFlowState,
      ReachingDefState pReachDefState
  ) throws CPATransferException {

    CInitializer maybeInitializer = pDecl.getInitializer();

    if (maybeInitializer != null && maybeInitializer instanceof CInitializerExpression) {
      // If the declaration contains an initializer, create the corresponding flow dependences
      // for its variable uses
      CExpression initializerExp = ((CInitializerExpression) maybeInitializer).getExpression();
      return handleOperation(
          pCfaEdge,
          initializerExp,
          pNextFlowState,
          pReachDefState);

    } else {
      // If the declaration contains no initializer, there are no variable uses and ergo
      // no new flow dependences.
      return pNextFlowState;
    }
  }

  /**
   * Adds the flow dependences based on the given {@link CAstNode} and the {@link ReachingDefState}
   * to the given {@link FlowDependenceState}.
   *
   * <p>
   * If no reaching definition exists for a program variable used in the expression, a flow
   * dependence to the declaration of the variable is added.
   */
  private FlowDependenceState handleOperation(
      CFAEdge pCfaEdge,
      CAstNode pExpression,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState)
      throws CPATransferException {

    String functionName = pCfaEdge.getPredecessor().getFunctionName();
    Map<MemoryLocation, Collection<ProgramDefinitionPoint>> defs =
        normalizeReachingDefinitions(pReachDefState, functionName);

    Set<CSimpleDeclaration> usedVariables = getUsedVars(pExpression);

    for (CSimpleDeclaration use : usedVariables) {
      MemoryLocation memLoc = MemoryLocation.valueOf(use.getQualifiedName());
      Collection<ProgramDefinitionPoint> definitionPoints = defs.get(memLoc);
      if (definitionPoints != null && !definitionPoints.isEmpty()) {
        pNextState.addDependences(memLoc, definitionPoints);
      } else {
        logger.log(Level.WARNING, "No definition point for use ", memLoc, " at ", pCfaEdge);
      }
    }

    return pNextState;
  }

  private Set<CSimpleDeclaration> getUsedVars(CAstNode pExpression)
      throws CPATransferException {
    return pExpression.accept(usesCollector);
  }

  private FlowDependenceState handleReturnStatementEdge(
      CReturnStatementEdge pCfaEdge,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState)
      throws CPATransferException {
    com.google.common.base.Optional<CAssignment> returnAssignment = pCfaEdge.asAssignment();

    if (returnAssignment.isPresent()) {
      CRightHandSide rhs = returnAssignment.get().getRightHandSide();
      return handleOperation(pCfaEdge, rhs, pNextState, pReachDefState);
    } else {
      return pNextState;
    }
  }

  protected FlowDependenceState handleAssumption(
      CAssumeEdge cfaEdge,
      CExpression expression,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState)
      throws CPATransferException {
    return handleOperation(cfaEdge, expression, pNextState, pReachDefState);
  }

  protected FlowDependenceState handleFunctionCallEdge(
      CFunctionCallEdge pFunctionCallEdge,
      List<CExpression> pArguments,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState
  ) throws CPATransferException {

    FlowDependenceState nextState = pNextState;
    for (CExpression argument : pArguments) {
      nextState = handleOperation(pFunctionCallEdge, argument, nextState, pReachDefState);
    }
    return nextState;
  }

  protected FlowDependenceState handleStatementEdge(
      CStatementEdge pCfaEdge,
      CStatement pStatement,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState)
      throws CPATransferException {

    return handleOperation(pCfaEdge, pStatement, pNextState, pReachDefState);
  }

  @Override
  public Collection<FlowDependenceState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    assert pState instanceof FlowDependenceState
        : "Expected state of type " + FlowDependenceState.class.getSimpleName();

    FlowDependenceState oldState = (FlowDependenceState) pState;
    ReachingDefState oldReachDefState = oldState.getReachDefState();
    Optional<ReachingDefState> nextReachDefState =
        computeReachDefState(oldReachDefState, pPrecision, pCfaEdge);

    if (nextReachDefState.isPresent()) {

      ReachingDefState newReachDefState = nextReachDefState.get();

      FlowDependenceState nextState = new FlowDependenceState(newReachDefState);
      switch (pCfaEdge.getEdgeType()) {
        case DeclarationEdge:
          CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
          if (declEdge.getDeclaration() instanceof CVariableDeclaration) {
            CVariableDeclaration declaration = (CVariableDeclaration) declEdge.getDeclaration();
            nextState = handleDeclarationEdge(declEdge, declaration, nextState, oldReachDefState);
          } // else {
            // Function declarations don't introduce any flow dependencies
            // }
          break;

        case StatementEdge:
          CStatementEdge stmtEdge = (CStatementEdge) pCfaEdge;
          nextState =
              handleStatementEdge(stmtEdge, stmtEdge.getStatement(), nextState, oldReachDefState);
          break;

        case AssumeEdge:
          CAssumeEdge assumeEdge = (CAssumeEdge) pCfaEdge;
          nextState =
              handleAssumption(assumeEdge, assumeEdge.getExpression(), nextState, oldReachDefState);
          break;

        case ReturnStatementEdge:
          CReturnStatementEdge returnEdge = (CReturnStatementEdge) pCfaEdge;
          nextState = handleReturnStatementEdge(returnEdge, nextState, oldReachDefState);
          break;

        case FunctionCallEdge:
          CFunctionCallEdge callEdge = (CFunctionCallEdge) pCfaEdge;
          nextState =
              handleFunctionCallEdge(
                  callEdge,
                  callEdge.getArguments(),
                  nextState,
                  oldReachDefState);
          break;

        default:
          break;
      }

      assert nextState != null;
      return ImmutableSet.of(nextState);

    } else {
      return Collections.emptySet();
    }
  }

  private Optional<ReachingDefState>
      computeReachDefState(ReachingDefState pOldState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException {

    Collection<? extends AbstractState> computedReachDefStates;
    try {
      computedReachDefStates =
          reachDefRelation
              .getAbstractSuccessorsForEdge(pOldState, pPrecision, pCfaEdge);

    } catch (InterruptedException pE) {
      throw new CPATransferException("Exception in reaching definitions transfer", pE);
    }

    if (computedReachDefStates.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of((ReachingDefState) Iterables.getOnlyElement(computedReachDefStates));
    }
  }

  /**
   * Visitor that collects the variables used in a {@link CAstNode}. Variables are represented by
   * their declaration.
   */
  private static class UsesCollector
      implements CAstNodeVisitor<Set<CSimpleDeclaration>, CPATransferException> {

    private final UsedIdsCollector idCollector = new UsedIdsCollector();

    @Override
    public Set<CSimpleDeclaration> visit(CExpressionStatement pStmt)
        throws CPATransferException {
      return pStmt.getExpression().accept(new UsedIdsCollector());
    }

    @Override
    public Set<CSimpleDeclaration> visit(CExpressionAssignmentStatement pStmt)
        throws CPATransferException {
      Set<CSimpleDeclaration> used = new HashSet<>();
      used.addAll(pStmt.getRightHandSide().accept(this));
      if (!(pStmt.getLeftHandSide() instanceof CIdExpression)) {
        used.addAll(pStmt.getLeftHandSide().accept(this));
      }

      return used;
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFunctionCallAssignmentStatement pStmt)
        throws CPATransferException {
      Set<CSimpleDeclaration> used = new HashSet<>();
      used.addAll(pStmt.getRightHandSide().accept(this));
      if (!(pStmt.getLeftHandSide() instanceof CIdExpression)) {
        used.addAll(pStmt.getLeftHandSide().accept(this));
      }

      return used;
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFunctionCallStatement pStmt)
        throws CPATransferException {
      Set<CSimpleDeclaration> paramDecls = new HashSet<>();
     for (CExpression p : pStmt.getFunctionCallExpression().getParameterExpressions()) {
       paramDecls.addAll(p.accept(this));
     }
     return paramDecls;
    }

    @Override
    public Set<CSimpleDeclaration> visit(CArrayDesignator pArrayDesignator)
        throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CArrayRangeDesignator pArrayRangeDesignator)
        throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFieldDesignator pFieldDesignator)
        throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CArraySubscriptExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFieldReference pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CIdExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CPointerExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CComplexCastExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CInitializerExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CInitializerList pInitializerList)
        throws CPATransferException {
      Set<CSimpleDeclaration> uses = new HashSet<>();
      for (CInitializer i : pInitializerList.getInitializers()) {
        uses.addAll(i.accept(this));
      }
      return uses;
    }

    @Override
    public Set<CSimpleDeclaration> visit(CDesignatedInitializer pExp)
        throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFunctionCallExpression pExp)
        throws CPATransferException {
      Set<CSimpleDeclaration> useds = new HashSet<>();
      for (CExpression p : pExp.getParameterExpressions()) {
        useds.addAll(p.accept(idCollector));
      }
      return useds;
    }

    @Override
    public Set<CSimpleDeclaration> visit(CBinaryExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CCastExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CCharLiteralExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFloatLiteralExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CIntegerLiteralExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CStringLiteralExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CTypeIdExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CUnaryExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CImaginaryLiteralExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CAddressOfLabelExpression pExp)
        throws CPATransferException {
      return pExp.accept(idCollector);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CFunctionDeclaration pDecl) throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CComplexTypeDeclaration pDecl)
        throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CTypeDefDeclaration pDecl) throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CVariableDeclaration pDecl) throws CPATransferException {
      CInitializer init = pDecl.getInitializer();
      if (init != null) {
        return init.accept(this);
      } else {
        return Collections.emptySet();
      }
    }

    @Override
    public Set<CSimpleDeclaration> visit(CParameterDeclaration pDecl) throws CPATransferException {
      return pDecl.asVariableDeclaration().accept(this);
    }

    @Override
    public Set<CSimpleDeclaration> visit(CEnumerator pDecl) throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<CSimpleDeclaration> visit(CReturnStatement pNode) throws CPATransferException {
      com.google.common.base.Optional<CExpression> ret = pNode.getReturnValue();

      if (ret.isPresent()) {
        return ret.get().accept(idCollector);
      } else {
        return Collections.emptySet();
      }
    }
  }
}
