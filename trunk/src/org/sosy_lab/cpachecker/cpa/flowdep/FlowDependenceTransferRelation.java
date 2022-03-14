// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.flowdep;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
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
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.flowdep.FlowDependenceState.FlowDependence;
import org.sosy_lab.cpachecker.cpa.flowdep.FlowDependenceState.UnknownPointerDependence;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/** Transfer relation of {@link FlowDependenceCPA}. */
class FlowDependenceTransferRelation extends SingleEdgeTransferRelation {

  private final TransferRelation delegate;
  private final Optional<VariableClassification> varClassification;

  private final LogManagerWithoutDuplicates logger;

  FlowDependenceTransferRelation(
      final TransferRelation pDelegate,
      final Optional<VariableClassification> pVarClassification,
      final LogManager pLogger) {
    delegate = pDelegate;
    varClassification = pVarClassification;

    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  private Multimap<MemoryLocation, ProgramDefinitionPoint> normalizeReachingDefinitions(
      ReachingDefState pState) {

    Multimap<MemoryLocation, ProgramDefinitionPoint> normalized = HashMultimap.create();

    normalized.putAll(normalize(pState.getLocalReachingDefinitions()));
    normalized.putAll(normalize(pState.getGlobalReachingDefinitions()));
    return normalized;
  }

  private Multimap<MemoryLocation, ProgramDefinitionPoint> normalize(
      Map<MemoryLocation, Set<DefinitionPoint>> pDefs) {

    Multimap<MemoryLocation, ProgramDefinitionPoint> normalized = HashMultimap.create();
    for (Map.Entry<MemoryLocation, Set<DefinitionPoint>> e : pDefs.entrySet()) {
      MemoryLocation varName = e.getKey();
      Set<DefinitionPoint> points = e.getValue();

      FluentIterable<ProgramDefinitionPoint> defPoints =
          FluentIterable.from(points).filter(ProgramDefinitionPoint.class);

      normalized.putAll(varName, defPoints);
    }
    return normalized;
  }

  /**
   * Returns a new FlowDependenceState for the declaration represented by the given {@link
   * CVariableDeclaration} object. Since the wrapped {@link
   * org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefCPA ReachingDefCPA} tracks new definitions of
   * variables, we only have to consider the use of variables in the initializer that may exist.
   */
  private FlowDependenceState handleDeclarationEdge(
      CDeclarationEdge pCfaEdge,
      CVariableDeclaration pDecl,
      FlowDependenceState pNextFlowState,
      ReachingDefState pReachDefState,
      PointerState pPointerState)
      throws CPATransferException {

    CInitializer maybeInitializer = pDecl.getInitializer();

    if (maybeInitializer instanceof CInitializerExpression) {
      // If the declaration contains an initializer, create the corresponding flow dependences
      // for its variable uses
      CExpression initializerExp = ((CInitializerExpression) maybeInitializer).getExpression();
      MemoryLocation def = MemoryLocation.forDeclaration(pDecl);
      return handleOperation(
          pCfaEdge,
          Optional.of(def),
          getUsedVars(initializerExp, pPointerState),
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
   * <p>If no reaching definition exists for a program variable used in the expression, a flow
   * dependence to the declaration of the variable is added.
   */
  private FlowDependenceState handleOperation(
      CFAEdge pCfaEdge,
      Optional<MemoryLocation> pNewDeclaration,
      Set<MemoryLocation> pUses,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState) {

    Multimap<MemoryLocation, ProgramDefinitionPoint> defs =
        normalizeReachingDefinitions(pReachDefState);

    FlowDependence dependences;
    if (pUses == null) {
      dependences = UnknownPointerDependence.getInstance();
    } else {
      // Keep only definitions of uses in the currently considered CFA edge
      defs.keySet().retainAll(pUses);
      if (defs.keySet().size() != pUses.size()) {
        logger.log(
            Level.WARNING,
            "No definition point for at least one use in edge %s: %s",
            pCfaEdge,
            pReachDefState);
      }
      dependences = FlowDependence.create(defs);
    }
    if (dependences.isUnknownPointerDependence() || !dependences.isEmpty()) {
      pNextState.addDependence(pCfaEdge, pNewDeclaration, dependences);
    }

    return pNextState;
  }

  private Set<MemoryLocation> getUsedVars(CAstNode pExpression, PointerState pPointerState)
      throws CPATransferException {
    UsesCollector usesCollector = new UsesCollector(pPointerState, varClassification);
    return pExpression.accept(usesCollector);
  }

  private FlowDependenceState handleReturnStatementEdge(
      CReturnStatementEdge pCfaEdge,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState,
      PointerState pPointerState)
      throws CPATransferException {
    Optional<CAssignment> asAssignment = pCfaEdge.asAssignment();

    if (asAssignment.isPresent()) {
      CAssignment returnAssignment = asAssignment.orElseThrow();
      CRightHandSide rhs = returnAssignment.getRightHandSide();
      Set<MemoryLocation> defs = getDef(returnAssignment.getLeftHandSide(), pPointerState);

      FlowDependenceState nextState = pNextState;
      for (MemoryLocation d : defs) {
        nextState =
            handleOperation(
                pCfaEdge,
                Optional.of(d),
                getUsedVars(rhs, pPointerState),
                nextState,
                pReachDefState);
      }
      return nextState;

    } else {
      return pNextState;
    }
  }

  private Set<MemoryLocation> getDef(CLeftHandSide pLeftHandSide, PointerState pPointerState)
      throws CPATransferException {
    Set<MemoryLocation> decls;
    UsesCollector collector = new UsesCollector(pPointerState, varClassification);
    if (pLeftHandSide instanceof CPointerExpression) {
      return getPossibePointees(
          (CPointerExpression) pLeftHandSide, pPointerState, varClassification);

    } else if (pLeftHandSide instanceof CArraySubscriptExpression) {
      decls = ((CArraySubscriptExpression) pLeftHandSide).getArrayExpression().accept(collector);
    } else {
      decls = pLeftHandSide.accept(collector);
    }
    return decls;
  }

  private static @Nullable Set<MemoryLocation> getPossibePointees(
      CPointerExpression pExp,
      PointerState pPointerState,
      Optional<VariableClassification> pVarClassification) {
    Set<MemoryLocation> pointees = ReachingDefUtils.possiblePointees(pExp, pPointerState);
    if (pointees == null) {
      pointees = new HashSet<>();
      if (pVarClassification.isPresent()) {
        Set<String> addressedVars = pVarClassification.orElseThrow().getAddressedVariables();
        for (String v : addressedVars) {
          MemoryLocation m = MemoryLocation.fromQualifiedName(v);
          pointees.add(m);
        }
      } else {
        // if pointees are unknown and we can't derive them through the variable classification,
        // any variable could be used.
        return null;
      }
    }
    return pointees;
  }

  protected FlowDependenceState handleAssumption(
      CAssumeEdge cfaEdge,
      CExpression expression,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState,
      PointerState pPointerState)
      throws CPATransferException {

    return handleOperation(
        cfaEdge,
        Optional.empty(),
        getUsedVars(expression, pPointerState),
        pNextState,
        pReachDefState);
  }

  protected FlowDependenceState handleFunctionCallEdge(
      CFunctionCallEdge pFunctionCallEdge,
      List<CExpression> pArguments,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState,
      PointerState pPointerState)
      throws CPATransferException {

    FlowDependenceState nextState = pNextState;
    List<CParameterDeclaration> params = pFunctionCallEdge.getSuccessor().getFunctionParameters();
    for (int i = 0; i < pArguments.size(); i++) {
      MemoryLocation def;
      if (i < params.size()) {
        def = MemoryLocation.forDeclaration(params.get(i));
      } else {
        assert pFunctionCallEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs();
        // TODO support var args
        break;
      }
      CExpression argument = pArguments.get(i);
      nextState =
          handleOperation(
              pFunctionCallEdge,
              Optional.of(def),
              getUsedVars(argument, pPointerState),
              nextState,
              pReachDefState);
    }
    return nextState;
  }

  protected FlowDependenceState handleStatementEdge(
      CStatementEdge pCfaEdge,
      CStatement pStatement,
      FlowDependenceState pNextState,
      ReachingDefState pReachDefState,
      PointerState pPointerState)
      throws CPATransferException {

    FlowDependenceState nextState = pNextState;
    Set<MemoryLocation> possibleDefs;
    if (pStatement instanceof CAssignment) {
      possibleDefs = getDef(((CAssignment) pStatement).getLeftHandSide(), pPointerState);

      if (possibleDefs != null) {
        for (MemoryLocation def : possibleDefs) {
          nextState =
              handleOperation(
                  pCfaEdge,
                  Optional.ofNullable(def),
                  getUsedVars(pStatement, pPointerState),
                  nextState,
                  pReachDefState);
        }
      } else {
        nextState =
            handleOperation(
                pCfaEdge,
                Optional.empty(),
                getUsedVars(pStatement, pPointerState),
                nextState,
                pReachDefState);
      }
    }

    return nextState;
  }

  @Override
  public Collection<FlowDependenceState> getAbstractSuccessorsForEdge(
      final AbstractState pState, final Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException {

    assert pState instanceof FlowDependenceState
        : "Expected state of type " + FlowDependenceState.class.getSimpleName();

    FlowDependenceState oldState = (FlowDependenceState) pState;
    CompositeState oldComposite = oldState.getReachDefState();
    Optional<CompositeState> nextComposite =
        computeReachDefState(oldComposite, pPrecision, pCfaEdge);

    if (nextComposite.isPresent()) {
      CompositeState newReachDefState = nextComposite.orElseThrow();
      Pair<ReachingDefState, PointerState> oldReachDefAndPointerState = oldState.unwrap();
      ReachingDefState oldReachDefState = oldReachDefAndPointerState.getFirst();
      PointerState oldPointerState = oldReachDefAndPointerState.getSecond();

      FlowDependenceState nextState = new FlowDependenceState(newReachDefState);
      switch (pCfaEdge.getEdgeType()) {
        case DeclarationEdge:
          CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
          if (declEdge.getDeclaration() instanceof CVariableDeclaration) {
            CVariableDeclaration declaration = (CVariableDeclaration) declEdge.getDeclaration();
            nextState =
                handleDeclarationEdge(
                    declEdge, declaration, nextState, oldReachDefState, oldPointerState);
          } // else {
          // Function declarations don't introduce any flow dependencies
          // }
          break;

        case StatementEdge:
          CStatementEdge stmtEdge = (CStatementEdge) pCfaEdge;
          nextState =
              handleStatementEdge(
                  stmtEdge, stmtEdge.getStatement(), nextState, oldReachDefState, oldPointerState);
          break;

        case AssumeEdge:
          CAssumeEdge assumeEdge = (CAssumeEdge) pCfaEdge;
          nextState =
              handleAssumption(
                  assumeEdge,
                  assumeEdge.getExpression(),
                  nextState,
                  oldReachDefState,
                  oldPointerState);
          break;

        case ReturnStatementEdge:
          CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pCfaEdge;
          nextState =
              handleReturnStatementEdge(
                  returnStatementEdge, nextState, oldReachDefState, oldPointerState);
          break;

        case FunctionCallEdge:
          CFunctionCallEdge callEdge = (CFunctionCallEdge) pCfaEdge;
          nextState =
              handleFunctionCallEdge(
                  callEdge, callEdge.getArguments(), nextState, oldReachDefState, oldPointerState);
          break;

        case FunctionReturnEdge:
          CFunctionReturnEdge returnEdge = (CFunctionReturnEdge) pCfaEdge;
          nextState =
              handleFunctionReturnEdge(returnEdge, nextState, oldReachDefState, oldPointerState);
          break;

        default:
          break;
      }

      assert nextState != null;
      return ImmutableSet.of(nextState);

    } else {
      return ImmutableSet.of();
    }
  }

  private FlowDependenceState handleFunctionReturnEdge(
      final CFunctionReturnEdge pReturnEdge,
      final FlowDependenceState pNewState,
      final ReachingDefState pReachDefState,
      final PointerState pPointerState)
      throws CPATransferException {

    FlowDependenceState nextState = pNewState;
    CFunctionSummaryEdge summaryEdge = pReturnEdge.getSummaryEdge();
    CFunctionCallExpression functionCall = summaryEdge.getExpression().getFunctionCallExpression();

    List<CExpression> outFunctionParams = functionCall.getParameterExpressions();
    List<CParameterDeclaration> inFunctionParams = functionCall.getDeclaration().getParameters();

    // TODO support varargs
    for (int i = 0; i < inFunctionParams.size(); i++) {
      CParameterDeclaration inParam = inFunctionParams.get(i);
      CType parameterType = inParam.getType();

      if (parameterType instanceof CArrayType) {
        CExpression outParam = outFunctionParams.get(i);
        Set<MemoryLocation> possibleDefs;
        if (outParam instanceof CLeftHandSide) {
          possibleDefs = getDef((CLeftHandSide) outParam, pPointerState);
        } else {
          throw new AssertionError("Unhandled: " + outParam);
        }

        if (possibleDefs != null) {
          for (MemoryLocation def : possibleDefs) {
            nextState =
                handleOperation(
                    pReturnEdge,
                    Optional.ofNullable(def),
                    ImmutableSet.of(MemoryLocation.forDeclaration(inParam)),
                    nextState,
                    pReachDefState);
          }
        } else {
          nextState =
              handleOperation(
                  pReturnEdge,
                  Optional.empty(),
                  ImmutableSet.of(MemoryLocation.forDeclaration(inParam)),
                  nextState,
                  pReachDefState);
        }
      }
    }

    Optional<CVariableDeclaration> maybeReturnVar =
        summaryEdge.getFunctionEntry().getReturnVariable();
    if (maybeReturnVar.isPresent()) {
      Set<MemoryLocation> possibleDefs = null;
      CFunctionCall call = summaryEdge.getExpression();
      if (call instanceof CFunctionCallAssignmentStatement) {
        possibleDefs =
            getDef(((CFunctionCallAssignmentStatement) call).getLeftHandSide(), pPointerState);
      }
      if (possibleDefs != null) {
        for (MemoryLocation def : possibleDefs) {
          nextState =
              handleOperation(
                  pReturnEdge,
                  Optional.ofNullable(def),
                  ImmutableSet.of(MemoryLocation.forDeclaration(maybeReturnVar.orElseThrow())),
                  nextState,
                  pReachDefState);
        }
      } else {
        nextState =
            handleOperation(
                pReturnEdge,
                Optional.empty(),
                ImmutableSet.of(MemoryLocation.forDeclaration(maybeReturnVar.orElseThrow())),
                nextState,
                pReachDefState);
      }
    }
    return nextState;
  }

  private Optional<CompositeState> computeReachDefState(
      CompositeState pOldState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    Collection<? extends AbstractState> computedReachDefStates;
    try {
      computedReachDefStates =
          delegate.getAbstractSuccessorsForEdge(pOldState, pPrecision, pCfaEdge);

    } catch (InterruptedException pE) {
      throw new CPATransferException("Exception in reaching definitions transfer", pE);
    }

    if (computedReachDefStates.isEmpty()) {
      return Optional.empty();
    } else {
      CompositeState composite = (CompositeState) Iterables.getOnlyElement(computedReachDefStates);
      return Optional.of(composite);
    }
  }

  /**
   * Visitor that collects the variables used in a {@link CAstNode}. Variables are represented by
   * their declaration.
   */
  private static class UsesCollector
      implements CAstNodeVisitor<Set<MemoryLocation>, CPATransferException> {

    private final PointerState pointerState;

    private final Optional<VariableClassification> varClassification;

    public UsesCollector(
        final PointerState pPointerState,
        final Optional<VariableClassification> pVarClassification) {
      pointerState = pPointerState;
      varClassification = pVarClassification;
    }

    private Set<MemoryLocation> combine(
        final Set<MemoryLocation> pLhs, final Set<MemoryLocation> pRhs) {
      if (pLhs == null || pRhs == null) {
        return null;

      } else {
        // FIXME: Change to immutable sets for performance
        Set<MemoryLocation> combined = new HashSet<>(pLhs);
        combined.addAll(pRhs);
        return combined;
      }
    }

    @Override
    public Set<MemoryLocation> visit(CExpressionStatement pStmt) throws CPATransferException {
      return pStmt.getExpression().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CExpressionAssignmentStatement pStmt)
        throws CPATransferException {
      Set<MemoryLocation> lhs = handleLeftHandSide(pStmt.getLeftHandSide());
      Set<MemoryLocation> rhs = pStmt.getRightHandSide().accept(this);
      return combine(lhs, rhs);
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallAssignmentStatement pStmt)
        throws CPATransferException {
      Set<MemoryLocation> lhs = handleLeftHandSide(pStmt.getLeftHandSide());
      Set<MemoryLocation> rhs = pStmt.getRightHandSide().accept(this);
      return combine(lhs, rhs);
    }

    private Set<MemoryLocation> handleLeftHandSide(final CLeftHandSide pLhs)
        throws CPATransferException {
      if (pLhs instanceof CPointerExpression) {
        return ((CPointerExpression) pLhs).getOperand().accept(this);
      } else if (pLhs instanceof CArraySubscriptExpression) {
        return ((CArraySubscriptExpression) pLhs).getSubscriptExpression().accept(this);
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallStatement pStmt) throws CPATransferException {
      Set<MemoryLocation> paramDecls = new HashSet<>();
      for (CExpression p : pStmt.getFunctionCallExpression().getParameterExpressions()) {
        paramDecls = combine(paramDecls, p.accept(this));
      }
      return paramDecls;
    }

    @Override
    public Set<MemoryLocation> visit(CArrayDesignator pArrayDesignator)
        throws CPATransferException {
      return pArrayDesignator.getSubscriptExpression().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CArrayRangeDesignator pArrayRangeDesignator)
        throws CPATransferException {
      Set<MemoryLocation> fst = pArrayRangeDesignator.getCeilExpression().accept(this);
      Set<MemoryLocation> snd = pArrayRangeDesignator.getFloorExpression().accept(this);
      return combine(fst, snd);
    }

    @Override
    public Set<MemoryLocation> visit(CFieldDesignator pFieldDesignator)
        throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CArraySubscriptExpression pExp) throws CPATransferException {
      Set<MemoryLocation> fst = pExp.getArrayExpression().accept(this);
      Set<MemoryLocation> snd = pExp.getSubscriptExpression().accept(this);

      return combine(fst, snd);
    }

    @Override
    public Set<MemoryLocation> visit(CFieldReference pExp) throws CPATransferException {
      return pExp.getFieldOwner().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CIdExpression pExp) throws CPATransferException {
      CSimpleDeclaration idDeclaration = pExp.getDeclaration();
      if (idDeclaration instanceof CVariableDeclaration
          || idDeclaration instanceof CParameterDeclaration) {
        return ImmutableSet.of(MemoryLocation.forDeclaration(idDeclaration));
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CPointerExpression pExp) throws CPATransferException {
      Set<MemoryLocation> uses = pExp.getOperand().accept(this);
      Set<MemoryLocation> pointees = getPossibePointees(pExp, pointerState, varClassification);
      return combine(uses, pointees);
    }

    @Override
    public Set<MemoryLocation> visit(CComplexCastExpression pExp) throws CPATransferException {
      return pExp.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CInitializerExpression pExp) throws CPATransferException {
      return pExp.accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CInitializerList pInitializerList)
        throws CPATransferException {
      Set<MemoryLocation> uses = new HashSet<>();
      for (CInitializer i : pInitializerList.getInitializers()) {
        uses = combine(uses, i.accept(this));
      }
      return uses;
    }

    @Override
    public Set<MemoryLocation> visit(CDesignatedInitializer pExp) throws CPATransferException {
      Set<MemoryLocation> used = pExp.getRightHandSide().accept(this);
      for (CDesignator d : pExp.getDesignators()) {
        used = combine(used, d.accept(this));
      }

      return used;
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionCallExpression pExp) throws CPATransferException {
      Set<MemoryLocation> uses = pExp.getFunctionNameExpression().accept(this);
      for (CExpression p : pExp.getParameterExpressions()) {
        uses = combine(uses, p.accept(this));
      }
      return uses;
    }

    @Override
    public Set<MemoryLocation> visit(CBinaryExpression pExp) throws CPATransferException {
      return combine(pExp.getOperand1().accept(this), pExp.getOperand2().accept(this));
    }

    @Override
    public Set<MemoryLocation> visit(CCastExpression pExp) throws CPATransferException {
      return pExp.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CCharLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CFloatLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CIntegerLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CStringLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CTypeIdExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CUnaryExpression pExp) throws CPATransferException {
      return pExp.getOperand().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CImaginaryLiteralExpression pExp) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CAddressOfLabelExpression pExp) throws CPATransferException {
      return pExp.accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CFunctionDeclaration pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CComplexTypeDeclaration pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CTypeDefDeclaration pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CVariableDeclaration pDecl) throws CPATransferException {
      CInitializer init = pDecl.getInitializer();
      if (init != null) {
        return init.accept(this);
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> visit(CParameterDeclaration pDecl) throws CPATransferException {
      return pDecl.asVariableDeclaration().accept(this);
    }

    @Override
    public Set<MemoryLocation> visit(CEnumerator pDecl) throws CPATransferException {
      return ImmutableSet.of();
    }

    @Override
    public Set<MemoryLocation> visit(CReturnStatement pNode) throws CPATransferException {
      Optional<CExpression> ret = pNode.getReturnValue();

      if (ret.isPresent()) {
        return ret.orElseThrow().accept(this);
      } else {
        return ImmutableSet.of();
      }
    }
  }
}
