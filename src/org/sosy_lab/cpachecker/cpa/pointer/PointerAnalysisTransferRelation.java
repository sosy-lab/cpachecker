// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.HeapLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidationReason;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.StructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.cpa.pointer.util.PointerArithmeticUtils;
import org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils;
import org.sosy_lab.cpachecker.cpa.pointer.util.StructUnionHandler;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PointerAnalysisTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private int allocationCounter = 0;

  @Options(prefix = "cpa.pointer")
  public static class PointerTransferOptions {

    public enum HeapAllocationStrategy {
      SINGLE,
      PER_CALL,
      PER_LINE
    }

    @Option(
        secure = true,
        description =
            "Strategy for mapping heap allocations to symbolic heap locations: SINGLE, PER_CALL,"
                + " PER_LINE")
    private HeapAllocationStrategy heapAllocationStrategy = HeapAllocationStrategy.PER_CALL;

    @Option(secure = true, description = "Strategy for handling structs in pointer analysis")
    private StructHandlingStrategy structHandlingStrategy = StructHandlingStrategy.ALL_FIELDS;

    @Option(
        secure = true,
        description =
            "Enable or disable offset-sensitive pointer analysis. "
                + "When false, offsets in pointer arithmetic are ignored.")
    private boolean isOffsetSensitive = true;

    public PointerTransferOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  private PointerTransferOptions options;

  public PointerAnalysisTransferRelation(LogManager pLogger, PointerTransferOptions pOptions) {
    logger = pLogger;
    options = pOptions;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    PointerAnalysisState pointerState = (PointerAnalysisState) pState;
    PointerAnalysisState resultState = getAbstractSuccessor(pointerState, pCfaEdge);
    if (resultState == PointerAnalysisState.BOTTOM_STATE) {
      return ImmutableSet.of();
    }
    return Collections.<AbstractState>singleton(resultState);
  }

  private PointerAnalysisState getAbstractSuccessor(PointerAnalysisState pState, CFAEdge pCfaEdge)
      throws CPATransferException {
    PointerAnalysisState resultState = pState;
    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge ->
          resultState = handleDeclarationEdge(pState, (CDeclarationEdge) pCfaEdge);
      case StatementEdge -> resultState = handleStatementEdge(pState, (CStatementEdge) pCfaEdge);
      case CallToReturnEdge -> {}
      case AssumeEdge -> resultState = handleAssumeEdge(pState, (AssumeEdge) pCfaEdge);
      case BlankEdge -> {}
      case FunctionCallEdge ->
          resultState = handleFunctionCallEdge(pState, (CFunctionCallEdge) pCfaEdge);
      case FunctionReturnEdge ->
          resultState = handleFunctionReturnEdge(pState, (CFunctionReturnEdge) pCfaEdge);
      case ReturnStatementEdge ->
          resultState = handleReturnStatementEdge(pState, (CReturnStatementEdge) pCfaEdge);
    }
    return resultState;
  }

  private PointerAnalysisState handleAssumeEdge(PointerAnalysisState pState, AssumeEdge pAssumeEdge)
      throws CPATransferException {
    if (!(pAssumeEdge.getExpression() instanceof CExpression condition)) {
      return pState;
    }

    boolean truthAssumption = pAssumeEdge.getTruthAssumption();

    if (condition instanceof CBinaryExpression binaryExpression) {
      return handleBasicBinaryCondition(pState, binaryExpression, truthAssumption, pAssumeEdge);
    }

    return pState;
  }

  private PointerAnalysisState handleBasicBinaryCondition(
      PointerAnalysisState pState,
      CBinaryExpression pExpression,
      boolean pTruthAssumption,
      AssumeEdge pCFAEdge)
      throws CPATransferException {

    boolean isEqualsOperator = pExpression.getOperator() == CBinaryExpression.BinaryOperator.EQUALS;
    boolean isNotEqualsOperator =
        pExpression.getOperator() == CBinaryExpression.BinaryOperator.NOT_EQUALS;

    if (isEqualsOperator || isNotEqualsOperator) {

      CExpression leftOperand = pExpression.getOperand1();
      CExpression rightOperand = pExpression.getOperand2();

      Type typeLeftOperand = leftOperand.getExpressionType().getCanonicalType();
      Type typeRightOperand = rightOperand.getExpressionType().getCanonicalType();

      boolean isNullComparison =
          PointerUtils.isNullPointer(leftOperand) || PointerUtils.isNullPointer(rightOperand);

      boolean leftNotPointer = !(typeLeftOperand instanceof CPointerType);
      boolean rightNotPointer = !(typeRightOperand instanceof CPointerType);
      boolean noPointersInExpr = leftNotPointer && rightNotPointer;
      boolean bothOperandsShouldBePointers = !isNullComparison;
      boolean atLeastOneOperandIsNotPointer = leftNotPointer || rightNotPointer;

      if ((bothOperandsShouldBePointers && atLeastOneOperandIsNotPointer)
          || (isNullComparison && noPointersInExpr)) {
        return pState;
      }

      if (isNullComparison) {
        CExpression pointerExpr =
            PointerUtils.isNullPointer(leftOperand) ? rightOperand : leftOperand;
        LocationSet pointsTo = getReferencedLocations(pointerExpr, pState, true, pCFAEdge, options);

        if (pointsTo.isTop()) {
          return pState;
        }
        if (pointsTo instanceof ExplicitLocationSet explicitPointsTo) {
          boolean mustBeEqualToNull = (isEqualsOperator == pTruthAssumption);

          if ((mustBeEqualToNull && explicitPointsTo.containsAnyNull())
              || (!mustBeEqualToNull && !explicitPointsTo.containsAllNulls())) {
            return pState;
          }
        }
        return PointerAnalysisState.BOTTOM_STATE;
      } else {
        LocationSet leftPointsTo =
            getReferencedLocations(leftOperand, pState, true, pCFAEdge, options);
        LocationSet rightPointsTo =
            getReferencedLocations(rightOperand, pState, true, pCFAEdge, options);

        if (leftPointsTo.isTop() || rightPointsTo.isTop()) {
          return pState;
        }

        if (leftPointsTo.isBot() && rightPointsTo.isBot()) {
          return PointerAnalysisState.BOTTOM_STATE;
        }

        if (leftPointsTo.isBot() || rightPointsTo.isBot()) {
          boolean mustBeEqual = (isEqualsOperator == pTruthAssumption);
          if (mustBeEqual) {
            return PointerAnalysisState.BOTTOM_STATE;
          } else {
            return pState;
          }
        }

        boolean mustBeEqual = (isEqualsOperator == pTruthAssumption);

        if (mustBeEqual) {
          if (leftPointsTo instanceof ExplicitLocationSet explicitLeftPointsTo
              && rightPointsTo instanceof ExplicitLocationSet explicitRightPointsTo) {

            if (explicitLeftPointsTo.equals(explicitRightPointsTo)) {
              return pState;
            }
            if (!explicitLeftPointsTo.hasCommonLocation(explicitRightPointsTo)) {
              return PointerAnalysisState.BOTTOM_STATE;
            }
          }
        } else {
          if (leftPointsTo instanceof ExplicitLocationSet explicitLeftPointsTo
              && rightPointsTo instanceof ExplicitLocationSet explicitRightPointsTo) {

            if (explicitLeftPointsTo.equals(explicitRightPointsTo)) {
              return PointerAnalysisState.BOTTOM_STATE;
            }
          }
        }
        return pState;
      }
    }
    return pState;
  }

  private PointerAnalysisState handleFunctionCallEdge(
      PointerAnalysisState pState, CFunctionCallEdge pCFunctionCallEdge)
      throws CPATransferException {

    PointerAnalysisState newState = pState;

    List<CParameterDeclaration> formalParameters =
        pCFunctionCallEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pCFunctionCallEdge.getArguments();

    int limit = Math.min(formalParameters.size(), actualParams.size());
    formalParameters = FluentIterable.from(formalParameters).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    for (Pair<CParameterDeclaration, CExpression> param :
        Pair.zipList(formalParameters, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration formalParam = param.getFirst();

      if (!(Objects.requireNonNull(formalParam).getType().getCanonicalType()
          instanceof CPointerType)) {
        continue;
      }

      DeclaredVariableLocation paramLocationPointer =
          new DeclaredVariableLocation(DeclaredVariableLocation.getMemoryLocation(formalParam));
      LocationSet referencedLocations =
          getReferencedLocations(
              Objects.requireNonNull(actualParam), pState, true, pCFunctionCallEdge, options);

      newState =
          new PointerAnalysisState(
              newState.getPointsToMap().putAndCopy(paramLocationPointer, referencedLocations));
    }

    for (CParameterDeclaration formalParam : FluentIterable.from(formalParameters).skip(limit)) {
      DeclaredVariableLocation paramLocationPointer =
          new DeclaredVariableLocation(DeclaredVariableLocation.getMemoryLocation(formalParam));
      newState =
          new PointerAnalysisState(
              newState
                  .getPointsToMap()
                  .putAndCopy(paramLocationPointer, LocationSetFactory.withBot()));
    }

    return newState;
  }

  private PointerAnalysisState handleFunctionReturnEdge(
      PointerAnalysisState pState, CFunctionReturnEdge pCfaEdge) throws CPATransferException {
    CFunctionCall callEdge = pCfaEdge.getFunctionCall();
    if (callEdge instanceof CFunctionCallAssignmentStatement callAssignment) {
      Optional<MemoryLocation> returnVar =
          PointerUtils.getFunctionReturnVariable(pCfaEdge.getFunctionEntry());
      if (returnVar.isEmpty()) {
        logger.log(Level.INFO, "Return edge with assignment, but no return variable: " + pCfaEdge);
        return pState;
      }
      DeclaredVariableLocation returnVarPointer =
          new DeclaredVariableLocation(returnVar.orElseThrow());
      CExpression lhs = callAssignment.getLeftHandSide();
      if (!(lhs.getExpressionType() instanceof CPointerType)) {
        return pState;
      }
      LocationSet rhsTargets = pState.getPointsToSet(returnVarPointer);
      if (rhsTargets instanceof ExplicitLocationSet explicitSet) {
        Set<PointerLocation> newTargets = new HashSet<>();

        String callerFunctionName = pCfaEdge.getSummaryEdge().getPredecessor().getFunctionName();
        for (PointerLocation target : explicitSet.sortedPointerLocations()) {
          if (target.isValidFunctionReturn(callerFunctionName)) {
            newTargets.add(target);
          } else {
            newTargets.add(new InvalidLocation(InvalidationReason.LOCAL_SCOPE_EXPIRED));
          }
        }

        rhsTargets = LocationSetFactory.withPointerTargets(newTargets);
      }

      LocationSet lhsLocations = getReferencedLocations(lhs, pState, false, pCfaEdge, options);
      return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
    }
    return pState;
  }

  private PointerAnalysisState handleReturnStatementEdge(
      PointerAnalysisState pState, CReturnStatementEdge pCfaEdge) throws CPATransferException {
    Optional<CExpression> expression = pCfaEdge.getExpression();
    if (expression.isEmpty()) {
      return pState;
    }
    CExpression returnExpression = expression.orElseThrow();
    Type returnType = returnExpression.getExpressionType();
    if (!(returnType instanceof CPointerType)) {
      return pState;
    }
    LocationSet returnLocations =
        getReferencedLocations(returnExpression, pState, true, pCfaEdge, options);
    if (returnLocations.isTop()) {
      return pState;
    }
    Optional<MemoryLocation> returnVariable =
        PointerUtils.getFunctionReturnVariable(pCfaEdge.getSuccessor().getEntryNode());

    return returnVariable
        .map(
            memoryLocation ->
                new PointerAnalysisState(
                    pState
                        .getPointsToMap()
                        .putAndCopy(new DeclaredVariableLocation(memoryLocation), returnLocations)))
        .orElse(pState);
  }

  private PointerAnalysisState handleStatementEdge(
      PointerAnalysisState pState, CStatementEdge pCfaEdge) throws CPATransferException {

    if (pCfaEdge.getStatement() instanceof CFunctionCallStatement callStatement) {
      CFunctionCallExpression callExpr = callStatement.getFunctionCallExpression();

      if (PointerUtils.isFreeFunction(callExpr.getFunctionNameExpression())) {
        return handleDeallocation(pState, pCfaEdge, callExpr);
      }
    }

    if (pCfaEdge.getStatement() instanceof CAssignment assignment) {
      Type type = assignment.getLeftHandSide().getExpressionType().getCanonicalType();
      if (!(type instanceof CPointerType)) {
        return pState;
      }
      if (assignment instanceof CFunctionCallAssignmentStatement callAssignment) {
        CExpression lhs = callAssignment.getLeftHandSide();
        if (PointerUtils.isNondetPointerReturn(
            callAssignment.getFunctionCallExpression().getFunctionNameExpression())) {
          // if (isNondetPointerReturn(rhs.getFunctionNameExpression())) {
          // We don't consider summary edges, so if we encounter a function call assignment edge,
          // this means that the called function is not defined.
          // If the function returns a non-deterministic pointer,
          // handle it that way.
          // Do not add to pointsToMap, since ⊤ is not explicitly tracked in this implementation.
          // By default, all pointers that are not present in pointsToMap are assumed to point to
          // Top.
          return pState;
        }
        if (PointerUtils.isMallocFunction(
            callAssignment.getFunctionCallExpression().getFunctionNameExpression())) {
          HeapLocation heapLocation;

          heapLocation = createHeapLocation(pCfaEdge);

          LocationSet lhsLocations = getReferencedLocations(lhs, pState, false, pCfaEdge, options);
          LocationSet rhsSet = LocationSetFactory.withPointerLocation(heapLocation);

          return handleAssignment(pState, lhsLocations, rhsSet, pCfaEdge);
        }
      }

      return handleAssignment(
          pState, assignment.getLeftHandSide(), assignment.getRightHandSide(), pCfaEdge);
    }
    return pState;
  }

  public PointerAnalysisState handleDeallocation(
      PointerAnalysisState pState, CStatementEdge pCfaEdge, CFunctionCallExpression callExpr)
      throws CPATransferException {
    CExpression freedExpr = callExpr.getParameterExpressions().get(0);

    LocationSet targets = getReferencedLocations(freedExpr, pState, true, pCfaEdge, options);

    if (targets instanceof ExplicitLocationSet explicitTargets) {
      PersistentMap<PointerLocation, LocationSet> newPointsToMap = pState.getPointsToMap();
      for (PointerLocation pt : explicitTargets.sortedPointerLocations()) {
        if (pt instanceof HeapLocation) {
          PointerLocation invalid = new InvalidLocation(InvalidationReason.FREED);
          newPointsToMap =
              newPointsToMap.putAndCopy(pt, LocationSetFactory.withPointerLocation(invalid));
        } else {
          logger.logf(
              Level.WARNING,
              "free() called on non-heap object: %s at %s",
              pt,
              pCfaEdge.getFileLocation());
        }
      }
      return new PointerAnalysisState(newPointsToMap);
    }
    return pState;
  }

  private HeapLocation createHeapLocation(CCfaEdge pCfaEdge) {
    String functionName = pCfaEdge.getPredecessor().getFunctionName();
    HeapLocation heapLocation;
    switch (options.heapAllocationStrategy) {
      case SINGLE -> heapLocation = HeapLocation.forSingleAllocation(functionName, null);
      case PER_CALL ->
          heapLocation = HeapLocation.forIndexedAllocation(functionName, allocationCounter++, 0L);
      case PER_LINE -> {
        int line = pCfaEdge.getFileLocation().getStartingLineInOrigin();
        heapLocation = HeapLocation.forLineBasedAllocation(functionName, line, 0L);
      }
      default ->
          throw new AssertionError(
              "Unhandled heap allocation strategy: " + options.heapAllocationStrategy);
    }
    return heapLocation;
  }

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState, CExpression pLhs, CRightHandSide pRhs, CCfaEdge pCfaEdge)
      throws CPATransferException {

    LocationSet lhsLocations = getReferencedLocations(pLhs, pState, false, pCfaEdge, options);
    LocationSet rhsTargets = getReferencedLocations(pRhs, pState, true, pCfaEdge, options);
    if (pLhs instanceof CFieldReference pCFieldReference) {
      CType baseType = pCFieldReference.getFieldOwner().getExpressionType().getCanonicalType();

      while (baseType instanceof CPointerType ptrType) {
        baseType = ptrType.getType().getCanonicalType();

        if (StructUnionHandler.isUnion(baseType) || StructUnionHandler.isStruct(baseType)) {
          return StructUnionHandler.handleAssignmentForStructOrUnionType(
              pState,
              baseType,
              lhsLocations,
              rhsTargets,
              pCfaEdge,
              options.structHandlingStrategy,
              logger);
        }
      }
    }
    return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
  }

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState,
      LocationSet lhsLocations,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge) {

    if (lhsLocations.isBot()) {
      return PointerAnalysisState.BOTTOM_STATE;
    }

    if (lhsLocations instanceof ExplicitLocationSet explicitLhsLocations) {
      if (explicitLhsLocations.getSize() == 1) {
        Optional<PointerAnalysisState> specialCase =
            PointerUtils.handleSpecialCasesForExplicitLocation(
                pState, explicitLhsLocations, rhsTargets, pCfaEdge, logger);

        if (specialCase.isPresent()) {
          return specialCase.orElseThrow();
        }

        PointerLocation lhsLocation =
            explicitLhsLocations.sortedPointerLocations().iterator().next();

        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
      } else {
        return StructUnionHandler.addElementsToAmbiguousLocations(
            pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }

  private PointerAnalysisState handleDeclarationEdge(
      PointerAnalysisState pState, CDeclarationEdge pCfaEdge) throws CPATransferException {

    if (pCfaEdge.getDeclaration() instanceof CVariableDeclaration declaration) {

      Type type = declaration.getType().getCanonicalType();
      if (!(type instanceof CPointerType)) {
        return pState;
      }

      CInitializer initializer = declaration.getInitializer();

      if (initializer != null) {
        LocationSet pointsToSet =
            initializer.accept(
                new CInitializerVisitor<LocationSet, CPATransferException>() {

                  @Override
                  public LocationSet visit(CInitializerExpression pInitializerExpression)
                      throws CPATransferException {
                    if (pInitializerExpression.getExpression()
                        instanceof CIntegerLiteralExpression) {
                      return LocationSetFactory.withTop();
                    }

                    return getReferencedLocations(
                        pInitializerExpression.getExpression(), pState, true, pCfaEdge, options);
                  }

                  @Override
                  public LocationSet visit(CInitializerList pInitializerList) {
                    return LocationSetFactory.withTop();
                  }

                  @Override
                  public LocationSet visit(CDesignatedInitializer pCStructInitializerPart) {
                    return LocationSetFactory.withTop();
                  }
                });
        if (pointsToSet.isTop()) {
          return pState;
        }
        DeclaredVariableLocation pointerLocation =
            new DeclaredVariableLocation(MemoryLocation.forDeclaration(declaration));
        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(pointerLocation, pointsToSet));
      }
      return pState;
    }
    return pState;
  }

  public static LocationSet getReferencedLocations(
      final CRightHandSide pExpression,
      final PointerAnalysisState pState,
      final boolean shouldDereference,
      final CFAEdge pCfaEdge,
      PointerTransferOptions pointerTransferOptions)
      throws CPATransferException {

    return pExpression.accept(
        new CRightHandSideVisitor<LocationSet, CPATransferException>() {

          @Override
          public LocationSet visit(CIdExpression pIdExpression) {
            final MemoryLocation location;
            CSimpleDeclaration declaration = pIdExpression.getDeclaration();
            if (pIdExpression.getExpressionType().getCanonicalType() instanceof CArrayType) {
              MemoryLocation arrayBase = MemoryLocation.forDeclaration(declaration);
              return LocationSetFactory.withPointerLocation(
                  new DeclaredVariableLocation(arrayBase.withAddedOffset(0)));
            }
            if (declaration != null) {
              location = MemoryLocation.forDeclaration(declaration);
            } else {
              location = MemoryLocation.forIdentifier(pIdExpression.getName());
            }

            LocationSet base =
                LocationSetFactory.withPointerTarget(new DeclaredVariableLocation(location));

            if (shouldDereference) {
              CType type = pIdExpression.getExpressionType().getCanonicalType();
              if (type instanceof CPointerType) {
                return dereference(base);
              }
            }

            return base;
          }

          @Override
          public LocationSet visit(CPointerExpression pPointerExpression)
              throws CPATransferException {
            LocationSet operand =
                getReferencedLocations(
                    pPointerExpression.getOperand(),
                    pState,
                    shouldDereference,
                    pCfaEdge,
                    pointerTransferOptions);
            if (shouldDereference) {
              return dereference(operand);
            }
            return operand;
          }

          @Override
          public LocationSet visit(CUnaryExpression pUnaryExpression) throws CPATransferException {
            if (pUnaryExpression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
              LocationSet operand =
                  getReferencedLocations(
                      pUnaryExpression.getOperand(),
                      pState,
                      false,
                      pCfaEdge,
                      pointerTransferOptions);
              return addressOf(operand);
            }
            return getReferencedLocations(
                pUnaryExpression.getOperand(),
                pState,
                shouldDereference,
                pCfaEdge,
                pointerTransferOptions);
          }

          @Override
          public LocationSet visit(CFieldReference pFieldReference) throws CPATransferException {
            CExpression owner = pFieldReference.getFieldOwner();
            String fieldName = pFieldReference.getFieldName();
            CType baseType = owner.getExpressionType().getCanonicalType();

            while (baseType instanceof CPointerType ptrType) {
              baseType = ptrType.getType().getCanonicalType();
            }

            StructHandlingStrategy strategy = pointerTransferOptions.structHandlingStrategy;
            String instanceName = null;

            if (pFieldReference.isPointerDereference()) {
              LocationSet pointees =
                  getReferencedLocations(owner, pState, true, pCfaEdge, pointerTransferOptions);
              if (pointees instanceof ExplicitLocationSet explicit && explicit.getSize() == 1) {
                for (PointerLocation target : explicit.sortedPointerLocations()) {
                  if (target instanceof DeclaredVariableLocation memPtr) {
                    instanceName = memPtr.memoryLocation().getIdentifier();
                    break;
                  } else if (target instanceof StructLocation structLoc) {
                    instanceName = structLoc.getQualifiedName();
                    break;
                  }
                }
                if (explicit.containsAllNulls()) {
                  return LocationSetFactory.withPointerLocation(
                      new InvalidLocation(InvalidationReason.NULL_DEREFERENCE));
                }
              }
            } else {
              if (owner instanceof CIdExpression idExpr) {
                instanceName = idExpr.getName();
              }
            }

            if (instanceName == null) {
              return LocationSetFactory.withTop();
            }

            LocationSet baseLocation = LocationSetFactory.withTop();
            if (StructUnionHandler.isUnion(baseType)) {
              baseLocation = strategy.getUnionLocation(baseType, instanceName, pCfaEdge);

            } else if (StructUnionHandler.isStruct(baseType)) {
              baseLocation =
                  strategy.getStructLocation(baseType, instanceName, fieldName, pCfaEdge);
            }
            if (shouldDereference
                && (strategy == StructHandlingStrategy.STRUCT_INSTANCE
                    || strategy == StructHandlingStrategy.ALL_FIELDS)) {
              return dereference(baseLocation);
            } else {
              return baseLocation;
            }
          }

          @Override
          public LocationSet visit(CArraySubscriptExpression expr) throws CPATransferException {
            LocationSet base =
                getReferencedLocations(
                    expr.getArrayExpression(),
                    pState,
                    shouldDereference,
                    pCfaEdge,
                    pointerTransferOptions);

            if (base.isTop() || base.isBot()) {
              return base;
            }
            if (!(expr.getSubscriptExpression() instanceof CIntegerLiteralExpression idxExpr)) {
              return LocationSetFactory.withTop();
            }

            long offset = idxExpr.getValue().longValue();
            if (base instanceof ExplicitLocationSet explicitBase) {
              LocationSet locationSet =
                  PointerArithmeticUtils.applyPointerArithmetic(
                      explicitBase, offset, pointerTransferOptions.isOffsetSensitive);

              if (shouldDereference) {
                return dereference(locationSet);
              } else {
                return locationSet;
              }
            }
            return LocationSetFactory.withTop();
          }

          @Override
          public LocationSet visit(CCastExpression expr) throws CPATransferException {
            if (PointerUtils.isNullPointer(expr)) {
              return LocationSetFactory.withNullLocation();
            }
            return getReferencedLocations(
                expr.getOperand(), pState, shouldDereference, pCfaEdge, pointerTransferOptions);
          }

          @Override
          public LocationSet visit(CBinaryExpression pBinaryExpression)
              throws CPATransferException {
            CBinaryExpression.BinaryOperator operator = pBinaryExpression.getOperator();
            if (operator != CBinaryExpression.BinaryOperator.PLUS
                && operator != CBinaryExpression.BinaryOperator.MINUS) {
              return LocationSetFactory.withBot();
            }

            CExpression operand1 = pBinaryExpression.getOperand1();
            CExpression operand2 = pBinaryExpression.getOperand2();

            boolean operand1IsPtr =
                (operand1.getExpressionType().getCanonicalType() instanceof CPointerType)
                    || (operand1.getExpressionType().getCanonicalType() instanceof CArrayType);
            boolean operand2IsPtr =
                (operand2.getExpressionType().getCanonicalType() instanceof CPointerType)
                    || (operand2.getExpressionType().getCanonicalType() instanceof CArrayType);

            boolean operand1IsInteger = operand1 instanceof CIntegerLiteralExpression;
            boolean operand2IsInteger = operand2 instanceof CIntegerLiteralExpression;

            if ((operand1IsPtr && operand2IsInteger) || (operand2IsPtr && operand1IsInteger)) {

              CExpression pointerExpr = operand1IsPtr ? operand1 : operand2;
              CExpression offsetExpr = operand1IsPtr ? operand2 : operand1;

              LocationSet base =
                  getReferencedLocations(
                      pointerExpr, pState, true, pCfaEdge, pointerTransferOptions);

              if (offsetExpr instanceof CIntegerLiteralExpression intLit) {
                long offset =
                    operator == CBinaryExpression.BinaryOperator.MINUS
                        ? -intLit.getValue().longValue()
                        : intLit.getValue().longValue();
                return PointerArithmeticUtils.applyPointerArithmetic(
                    base, offset, pointerTransferOptions.isOffsetSensitive);
              }

              return LocationSetFactory.withTop();
            }
            if (operand1IsPtr || operand2IsPtr) {
              return LocationSetFactory.withPointerLocation(
                  new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC));
            }
            return LocationSetFactory.withBot();
          }

          @Override
          public LocationSet visit(CCharLiteralExpression pCharLiteralExpression) {
            return LocationSetFactory.withBot();
          }

          @Override
          public LocationSet visit(CFloatLiteralExpression pFloatLiteralExpression) {
            return LocationSetFactory.withBot();
          }

          @Override
          public LocationSet visit(CIntegerLiteralExpression pIntegerLiteralExpression) {
            return LocationSetFactory.withBot();
          }

          @Override
          public LocationSet visit(CStringLiteralExpression pStringLiteralExpression) {
            // TODO create StringLiteralLocation
            String literal = pStringLiteralExpression.getContentWithoutNullTerminator();
            MemoryLocation stringLoc = MemoryLocation.forIdentifier("__string_literal_" + literal);
            return LocationSetFactory.withPointerLocation(new DeclaredVariableLocation(stringLoc));
          }

          @Override
          public LocationSet visit(CTypeIdExpression pTypeIdExpression) {
            return LocationSetFactory.withBot();
          }

          @Override
          public LocationSet visit(CImaginaryLiteralExpression PLiteralExpression) {
            return LocationSetFactory.withBot();
          }

          @Override
          public LocationSet visit(CAddressOfLabelExpression pAddressOfLabelExpression)
              throws CPATransferException {
            throw new CPATransferException("Address of labels not supported by pointer analysis");
          }

          @Override
          public LocationSet visit(CFunctionCallExpression pFunctionCallExpression) {
            CFunctionDeclaration decl = pFunctionCallExpression.getDeclaration();
            if (decl != null) {
              CType returnType = decl.getType().getReturnType().getCanonicalType();

              if (!(returnType instanceof CPointerType)) {
                return LocationSetFactory.withBot();
              }
              MemoryLocation functionReturnLocation = MemoryLocation.forDeclaration(decl);
              DeclaredVariableLocation returnPtr =
                  new DeclaredVariableLocation(functionReturnLocation);
              return pState.getPointsToSet(returnPtr);
            } else {
              return LocationSetFactory.withTop();
            }
          }

          @Override
          public LocationSet visit(CComplexCastExpression complexCastExpression)
              throws CPATransferException {
            CRightHandSide operand = complexCastExpression.getOperand();
            if (PointerUtils.isNullPointer(operand)) {
              return LocationSetFactory.withNullLocation();
            }
            return getReferencedLocations(
                operand, pState, shouldDereference, pCfaEdge, pointerTransferOptions);
          }

          private LocationSet dereference(LocationSet set) {
            if (set.isTop() || set.isBot()) {
              return set;
            }
            if (set.containsAllNulls()) {
              return LocationSetFactory.withPointerLocation(
                  new InvalidLocation(InvalidationReason.NULL_DEREFERENCE));
            }
            if (!(set instanceof ExplicitLocationSet explicitSet)) {
              return LocationSetFactory.withTop();
            }

            LocationSet result = LocationSetFactory.withBot();
            for (PointerLocation pt : explicitSet.sortedPointerLocations()) {
              LocationSet target = pState.getPointsToSet(pt);
              if (target.isTop() || target.isBot()) {
                return target;
              }
              result = result.withPointerTargets(target);
            }
            return result;
          }

          private LocationSet addressOf(LocationSet set) {
            if (!(set instanceof ExplicitLocationSet explicit)) {
              return LocationSetFactory.withTop();
            }
            return explicit;
          }
        });
  }
}
