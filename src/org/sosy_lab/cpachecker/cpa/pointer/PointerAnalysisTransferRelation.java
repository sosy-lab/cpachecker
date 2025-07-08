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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;

import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisTransferRelation.PointerTransferOptions.StructHandlingStrategy;
import org.sosy_lab.cpachecker.cpa.pointer.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.HeapLocation;
import org.sosy_lab.cpachecker.cpa.pointer.util.InvalidationReason;
import org.sosy_lab.cpachecker.cpa.pointer.util.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSetTop;
import org.sosy_lab.cpachecker.cpa.pointer.util.MemoryLocationPointer;
import org.sosy_lab.cpachecker.cpa.pointer.util.PointerTarget;
import org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils;
import org.sosy_lab.cpachecker.cpa.pointer.util.StructLocation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
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

    public enum StructHandlingStrategy {
      JUST_STRUCT,
      STRUCT_INSTANCE,
      ALL_FIELDS
    }

    @Option(
        secure = true,
        description =
            "Strategy for mapping heap allocations to symbolic heap locations: SINGLE, PER_CALL, PER_LINE")
    private HeapAllocationStrategy heapAllocationStrategy = HeapAllocationStrategy.SINGLE;

    @Option(secure = true, description = "Strategy for handling structs in pointer analysis")
    private StructHandlingStrategy structHandlingStrategy = StructHandlingStrategy.STRUCT_INSTANCE;

    public PointerTransferOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }
  }

  private static PointerTransferOptions getOptions() {
    if (options == null) {
      throw new IllegalStateException("PointerTransferOptions must be initialized before use");
    }
    return options;
  }

  private static PointerTransferOptions options;

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

    if (!resultState.isBottom()) {
      String currentState = resultState.toString();
      logger.log(Level.INFO, currentState);
    }
    return resultState;
  }

  private PointerAnalysisState handleAssumeEdge(PointerAnalysisState pState, AssumeEdge pAssumeEdge)
      throws UnrecognizedCodeException {
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
      throws UnrecognizedCodeException {

    if (pExpression.getOperator() != CBinaryExpression.BinaryOperator.EQUALS
        && pExpression.getOperator() != CBinaryExpression.BinaryOperator.NOT_EQUALS) {
      return pState;
    }

    CExpression leftOperand = pExpression.getOperand1();
    CExpression rightOperand = pExpression.getOperand2();

    Type typeLeftOperand = leftOperand.getExpressionType().getCanonicalType();
    Type typeRightOperand = rightOperand.getExpressionType().getCanonicalType();

    boolean isEquals = pExpression.getOperator() == CBinaryExpression.BinaryOperator.EQUALS;

    boolean isNullComparison = isNullPointer(leftOperand) || isNullPointer(rightOperand);

    boolean leftNotPointer = !(typeLeftOperand instanceof CPointerType);
    boolean rightNotPointer = !(typeRightOperand instanceof CPointerType);

    if ((!isNullComparison && (leftNotPointer || rightNotPointer))
        || (isNullComparison && leftNotPointer && rightNotPointer)) {
      return pState;
    }

    if (isNullComparison) {
      CExpression pointerExpr = isNullPointer(leftOperand) ? rightOperand : leftOperand;
      int derefCounter = determineDerefCounter(pointerExpr, false);
      LocationSet pointsTo = getReferencedLocations(pointerExpr, pState, derefCounter, pCFAEdge);

      if (pointsTo.isTop()) {
        return pState;
      }
      if (pointsTo instanceof ExplicitLocationSet explicitPointsTo) {
        boolean mustBeEqualToNull = (isEquals == pTruthAssumption);

        if ((mustBeEqualToNull && explicitPointsTo.containsNull())
            || (!mustBeEqualToNull && (explicitPointsTo.getSizeWithoutNull() > 0))) {
          return pState;
        }
      }
      return PointerAnalysisState.BOTTOM_STATE;
    } else {
      int leftDerefCounter = determineDerefCounter(leftOperand, false);
      int rightDerefCounter = determineDerefCounter(rightOperand, false);
      LocationSet leftPointsTo =
          getReferencedLocations(leftOperand, pState, leftDerefCounter, pCFAEdge);
      LocationSet rightPointsTo =
          getReferencedLocations(rightOperand, pState, rightDerefCounter, pCFAEdge);

      if (leftPointsTo.isTop() || rightPointsTo.isTop()) {
        return pState;
      }

      boolean mustBeEqual = (isEquals == pTruthAssumption);

      if (mustBeEqual) {
        if (leftPointsTo instanceof ExplicitLocationSet explicitLeftPointsTo
            && rightPointsTo instanceof ExplicitLocationSet explicitRightPointsTo) {

          if (explicitLeftPointsTo.equals(explicitRightPointsTo)) {
            return pState;
          }
          if (!hasCommonLocation(explicitLeftPointsTo, explicitRightPointsTo)) {
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

  private boolean hasCommonLocation(ExplicitLocationSet pSet1, ExplicitLocationSet pSet2) {
    if (pSet1.containsNull() && pSet2.containsNull()) {
      return true;
    }
    for (PointerTarget loc : pSet1.getExplicitLocations()) {
      if (pSet2.mayPointTo(loc)) {
        return true;
      }
    }
    return false;
  }

  private PointerAnalysisState handleFunctionCallEdge(
      PointerAnalysisState pState, CFunctionCallEdge pCFunctionCallEdge)
      throws UnrecognizedCodeException {

    PointerAnalysisState newState = pState;

    List<CParameterDeclaration> formalParams =
        pCFunctionCallEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pCFunctionCallEdge.getArguments();

    int limit = Math.min(formalParams.size(), actualParams.size());
    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    for (Pair<CParameterDeclaration, CExpression> param :
        Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration formalParam = param.getFirst();

      MemoryLocationPointer paramLocationPointer =
          new MemoryLocationPointer(getMemoryLocation(formalParam));
      int derefCounter = determineDerefCounter(actualParam, false);
      LocationSet referencedLocations =
          getReferencedLocations(actualParam, pState, derefCounter, pCFunctionCallEdge);

      if (!referencedLocations.isBot()) {
        newState =
            new PointerAnalysisState(
                newState.getPointsToMap().putAndCopy(paramLocationPointer, referencedLocations));
      }
    }

    for (CParameterDeclaration formalParam : FluentIterable.from(formalParams).skip(limit)) {
      MemoryLocationPointer paramLocationPointer =
          new MemoryLocationPointer(getMemoryLocation(formalParam));
      newState =
          new PointerAnalysisState(
              newState.getPointsToMap().putAndCopy(paramLocationPointer, LocationSetBot.INSTANCE));
    }

    return newState;
  }

  private MemoryLocation getMemoryLocation(AbstractSimpleDeclaration pDeclaration) {
    return MemoryLocation.parseExtendedQualifiedName(pDeclaration.getQualifiedName());
  }

  private PointerAnalysisState handleFunctionReturnEdge(
      PointerAnalysisState pState, CFunctionReturnEdge pCfaEdge) throws UnrecognizedCodeException {
    CFunctionCall callEdge = pCfaEdge.getFunctionCall();
    if (callEdge instanceof CFunctionCallAssignmentStatement callAssignment) {
      Optional<MemoryLocation> returnVar = getFunctionReturnVariable(pCfaEdge.getFunctionEntry());
      if (returnVar.isEmpty()) {
        logger.log(Level.INFO, "Return edge with assignment, but no return variable: " + pCfaEdge);
        return pState;
      }
      MemoryLocationPointer returnVarPointer = new MemoryLocationPointer(returnVar.orElseThrow());
      CExpression lhs = callAssignment.getLeftHandSide();
      if (!(lhs.getExpressionType() instanceof CPointerType)) {
        return pState;
      }
      int lhsDeref = determineDerefCounter(lhs, true);
      LocationSet rhsTargets = pState.getPointsToSet(returnVarPointer);
      if (rhsTargets instanceof ExplicitLocationSet explicitSet) {
        Set<PointerTarget> newTargets = new HashSet<>();
        boolean containsNull = explicitSet.containsNull();

        for (PointerTarget target : explicitSet.getExplicitLocations()) {
          if (PointerUtils.isValidFunctionReturn(target)) {
            newTargets.add(target);
          } else {
            newTargets.add(InvalidLocation.forInvalidation(InvalidationReason.LOCAL_SCOPE_EXPIRED));
          }
        }

        rhsTargets = ExplicitLocationSet.from(newTargets, containsNull);
      }

      LocationSet lhsLocations = getReferencedLocations(lhs, pState, lhsDeref, pCfaEdge);
      return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
    }
    return pState;
  }

  private Optional<MemoryLocation> getFunctionReturnVariable(FunctionEntryNode pFunctionEntryNode) {
    Optional<? extends AVariableDeclaration> returnVariable =
        pFunctionEntryNode.getReturnVariable();
    return returnVariable.map(MemoryLocation::forDeclaration);
  }

  private PointerAnalysisState handleReturnStatementEdge(
      PointerAnalysisState pState, CReturnStatementEdge pCfaEdge) throws UnrecognizedCodeException {
    Optional<CExpression> expression = pCfaEdge.getExpression();
    if (expression.isEmpty()) {
      return pState;
    }
    CExpression returnExpression = expression.get();
    Type returnType = returnExpression.getExpressionType();
    if (!(returnType instanceof CPointerType)) {
      return pState;
    }
    int derefCounter = determineDerefCounter(returnExpression, false);
    LocationSet returnLocations =
        getReferencedLocations(returnExpression, pState, derefCounter, pCfaEdge);
    Optional<MemoryLocation> returnVariable =
        getFunctionReturnVariable(pCfaEdge.getSuccessor().getEntryNode());
    return returnVariable
        .map(
            memoryLocation ->
                new PointerAnalysisState(
                    pState
                        .getPointsToMap()
                        .putAndCopy(new MemoryLocationPointer(memoryLocation), returnLocations)))
        .orElse(pState);
  }

  private PointerAnalysisState handleStatementEdge(
      PointerAnalysisState pState, CStatementEdge pCfaEdge) throws UnrecognizedCodeException {

    if (pCfaEdge.getStatement() instanceof CFunctionCallStatement callStatement) {
      CFunctionCallExpression callExpr = callStatement.getFunctionCallExpression();

      if (PointerUtils.isFreeFunction(callExpr.getFunctionNameExpression())) {

        CExpression freedExpr = callExpr.getParameterExpressions().get(0);

        int derefCounter = determineDerefCounter(freedExpr, false);
        LocationSet targets = getReferencedLocations(freedExpr, pState, derefCounter, pCfaEdge);

        if (targets instanceof ExplicitLocationSet explicitTargets) {
          Set<PointerTarget> updatedTargets = new HashSet<>();
          for (PointerTarget pt : explicitTargets.getExplicitLocations()) {
            if (pt instanceof HeapLocation) {
              updatedTargets.add(InvalidLocation.forInvalidation(InvalidationReason.FREED));
            } else {
              updatedTargets.add(pt);
            }
          }
          ExplicitLocationSet newSet =
              (ExplicitLocationSet)
                  ExplicitLocationSet.from(updatedTargets, explicitTargets.containsNull());

          if (freedExpr instanceof CIdExpression idExpr) {
            PointerTarget target =
                new MemoryLocationPointer(MemoryLocation.forDeclaration(idExpr.getDeclaration()));
            return new PointerAnalysisState(pState.getPointsToMap().putAndCopy(target, newSet));
          } else {
            logger.logf(
                Level.INFO,
                "PointerAnalysis: free() called on non-CIdExpression at %s: %s",
                pCfaEdge.getFileLocation(),
                freedExpr.toASTString());
          }
        }
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

          int lhsDeref = determineDerefCounter(lhs, true);
          LocationSet lhsLocations = getReferencedLocations(lhs, pState, lhsDeref, pCfaEdge);
          LocationSet rhsSet = ExplicitLocationSet.from(heapLocation);

          return handleAssignment(pState, lhsLocations, rhsSet, pCfaEdge);
        }
      }

      return handleAssignment(
          pState, assignment.getLeftHandSide(), assignment.getRightHandSide(), pCfaEdge);
    }
    return pState;
  }

  @Nonnull
  private HeapLocation createHeapLocation(CCfaEdge pCfaEdge) {
    String functionName = pCfaEdge.getPredecessor().getFunctionName();
    HeapLocation heapLocation;
    switch (getOptions().heapAllocationStrategy) {
      case SINGLE -> heapLocation = HeapLocation.forAllocation(functionName, -1);
      case PER_CALL -> heapLocation = HeapLocation.forAllocation(functionName, allocationCounter++);
      case PER_LINE -> {
        int line = pCfaEdge.getFileLocation().getStartingLineInOrigin();
        heapLocation = HeapLocation.forLineBasedAllocation(functionName, line);
      }
      default ->
          throw new AssertionError(
              "Unhandled heap allocation strategy: " + options.heapAllocationStrategy);
    }
    return heapLocation;
  }

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState, CExpression pLhs, CRightHandSide pRhs, CCfaEdge pCfaEdge)
      throws UnrecognizedCodeException {

    int lhsDerefCounter = determineDerefCounter(pLhs, true);
    int rhsDerefCounter = determineDerefCounter(pRhs, false);

    LocationSet lhsLocations = getReferencedLocations(pLhs, pState, lhsDerefCounter, pCfaEdge);
    LocationSet rhsTargets = getReferencedLocations(pRhs, pState, rhsDerefCounter, pCfaEdge);
    // TODO: Handle the case pLhs is CFieldReference
    return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
  }

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState,
      LocationSet lhsLocations,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge) {
    if (lhsLocations instanceof ExplicitLocationSet explicitLhsLocations) {
      if (explicitLhsLocations.getSize() == 1) {
        PointerTarget lhsLocation = explicitLhsLocations.getExplicitLocations().iterator().next();
        if (lhsLocation instanceof StructLocation structLocation) {
          return handleStructAssignment(pState, structLocation, rhsTargets, pCfaEdge);
        }
        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
      } else {
        return addElementsToAmbiguousLocations(pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }

  private PointerAnalysisState handleStructAssignment(
      PointerAnalysisState pState,
      StructLocation lhsLocation,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge) {
    switch (getOptions().structHandlingStrategy) {
      case JUST_STRUCT -> {
        return pState;
      }
      case STRUCT_INSTANCE, ALL_FIELDS -> {
        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
      }
      default ->
          throw new AssertionError(
              "Unknown struct handling strategy: " + getOptions().structHandlingStrategy);
    }
  }

  private PointerAnalysisState addElementsToAmbiguousLocations(
      PointerAnalysisState pState, ExplicitLocationSet pLhsLocations, LocationSet pRhsTargets) {
    Set<PointerTarget> locations = pLhsLocations.getExplicitLocations();
    PointerAnalysisState updatedState = pState;
    for (PointerTarget loc : locations) {
      LocationSet existingSet = updatedState.getPointsToSet(loc);
      LocationSet mergedSet = existingSet.addElements(pRhsTargets);
      updatedState =
          new PointerAnalysisState(updatedState.getPointsToMap().putAndCopy(loc, mergedSet));
    }
    return updatedState;
  }

  private PointerAnalysisState handleDeclarationEdge(
      PointerAnalysisState pState, CDeclarationEdge pCfaEdge) throws UnrecognizedCodeException {

    if (pCfaEdge.getDeclaration() instanceof CVariableDeclaration declaration) {

      Type type = declaration.getType().getCanonicalType();
      if (!(type instanceof CPointerType)) {
        return pState;
      }

      CInitializer initializer = declaration.getInitializer();

      if (initializer != null) {
        LocationSet pointsToSet =
            initializer.accept(
                new CInitializerVisitor<LocationSet, UnrecognizedCodeException>() {

                  @Override
                  public LocationSet visit(CInitializerExpression pInitializerExpression)
                      throws UnrecognizedCodeException {
                    int derefCounter =
                        determineDerefCounter(pInitializerExpression.getExpression(), false);
                    return getReferencedLocations(
                        pInitializerExpression.getExpression(), pState, derefCounter, pCfaEdge);
                  }

                  @Override
                  public LocationSet visit(CInitializerList pInitializerList)
                      throws UnrecognizedCodeException {
                    return LocationSetTop.INSTANCE;
                  }

                  @Override
                  public LocationSet visit(CDesignatedInitializer pCStructInitializerPart)
                      throws UnrecognizedCodeException {
                    return LocationSetTop.INSTANCE;
                  }
                });
        MemoryLocationPointer pointerLocation =
            new MemoryLocationPointer(MemoryLocation.forDeclaration(declaration));
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
      final int pDerefCounter,
      final CFAEdge pCfaEdge)
      throws UnrecognizedCodeException {

    return pExpression.accept(
        new CRightHandSideVisitor<LocationSet, UnrecognizedCodeException>() {

          @Override
          public LocationSet visit(CIdExpression pIdExpression) throws UnrecognizedCodeException {
            final MemoryLocation location;
            CSimpleDeclaration declaration = pIdExpression.getDeclaration();
            if (declaration != null) {
              location = MemoryLocation.forDeclaration(declaration);
            } else {
              location = MemoryLocation.forIdentifier(pIdExpression.getName());
            }
            return applyDereferences(location, pDerefCounter);
          }

          private LocationSet applyDereferences(MemoryLocation pLocation, int pDerefCount) {
            LocationSet current = toLocationSet(pLocation);

            for (int i = 0; i < pDerefCount; i++) {
              // TODO: Check condition
              if (current.isTop() || current.isBot() || current.isNull()) {
                return current;
              }

              if (!(current instanceof ExplicitLocationSet explicitCurrentSet)) {
                return LocationSetTop.INSTANCE;
              }

              LocationSet next = LocationSetBot.INSTANCE;

              for (PointerTarget location : explicitCurrentSet.getExplicitLocations()) {
                // If the location is not a memory location, we cannot dereference it
                if (!(location instanceof MemoryLocationPointer memoryLocationPointer)) {
                  return LocationSetTop.INSTANCE;
                } else {
                  LocationSet target = pState.getPointsToSet(memoryLocationPointer);
                  if (target.isTop() || target.isBot() || target.isNull()) {
                    return target;
                  }
                  if (!(target instanceof ExplicitLocationSet)) {
                    return LocationSetTop.INSTANCE;
                  }
                  next = next.addElements(target);
                }
              }
              current = next;
            }
            return current;
          }

          @Override
          public LocationSet visit(CUnaryExpression pUnaryExpression)
              throws UnrecognizedCodeException {
            return getReferencedLocations(
                pUnaryExpression.getOperand(), pState, pDerefCounter, pCfaEdge);
          }

          @Override
          public LocationSet visit(CPointerExpression pPointerExpression)
              throws UnrecognizedCodeException {
            return getReferencedLocations(
                pPointerExpression.getOperand(), pState, pDerefCounter, pCfaEdge);
          }

          @Override
          public LocationSet visit(CArraySubscriptExpression pArraySubscriptExpression)
              throws UnrecognizedCodeException {
            CExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();
            CExpression subscriptExpression = pArraySubscriptExpression.getSubscriptExpression();
            LocationSet baseLocations =
                getReferencedLocations(arrayExpression, pState, 0, pCfaEdge);
            if (baseLocations.isBot() || baseLocations.isTop()) {
              return baseLocations;
            }
            if (!(subscriptExpression instanceof CIntegerLiteralExpression indexLiteral)) {
              return LocationSetTop.INSTANCE;
            }
            long indexValue = indexLiteral.getValue().longValue();
            Set<MemoryLocation> elementLocations = new HashSet<>();
            for (PointerTarget baseLocation :
                ((ExplicitLocationSet) baseLocations).getExplicitLocations()) {
              if (baseLocation instanceof MemoryLocationPointer baseMemoryLocation) {
                MemoryLocation elementLocation =
                    baseMemoryLocation.getMemoryLocation().withAddedOffset(indexValue);
                elementLocations.add(elementLocation);
                // TODO check null pointer
              }
            }

            if (pDerefCounter > 0) {
              Set<PointerTarget> pointsToLocations = new HashSet<>();
              boolean containsNull = false;
              for (MemoryLocation location : elementLocations) {
                LocationSet targetLocation = applyDereferences(location, pDerefCounter);
                if (targetLocation instanceof ExplicitLocationSet explicitTargetLocation) {
                  pointsToLocations.addAll(explicitTargetLocation.getExplicitLocations());
                  containsNull = containsNull || explicitTargetLocation.containsNull();
                }
              }

              return (!containsNull && pointsToLocations.isEmpty())
                  ? LocationSetBot.INSTANCE
                  : ExplicitLocationSet.from(pointsToLocations, containsNull);
            } else {
              Set<PointerTarget> pointerTargets = new HashSet<>();
              for (MemoryLocation loc : elementLocations) {
                pointerTargets.add(new MemoryLocationPointer(loc));
              }
              return ExplicitLocationSet.from(pointerTargets);
            }
          }

          @Override
          public LocationSet visit(CFieldReference pFieldReference)
              throws UnrecognizedCodeException {
            CType ownerType =
                pFieldReference.getFieldOwner().getExpressionType().getCanonicalType();
            String structType = ownerType.toString();

            String fieldName = pFieldReference.getFieldName();
            StructLocation structLoc;

            StructHandlingStrategy strategy = getOptions().structHandlingStrategy;

            if (strategy == StructHandlingStrategy.JUST_STRUCT) {
              structLoc =
                  StructLocation.forStruct(pCfaEdge.getPredecessor().getFunctionName(), structType);
              return ExplicitLocationSet.from(structLoc);
            }

            // STRUCT_INSTANCE and ALL_FIELDS need instanceName
            @Nullable String instanceName = null;

            CExpression fieldOwner = pFieldReference.getFieldOwner();

            // Case 1: direct access (s.x)
            if (fieldOwner instanceof CIdExpression idExpr) {
              instanceName = idExpr.getName();
            }

            // Case 2: pointer dereference (p->x)
            if (pFieldReference.isPointerDereference() && instanceName == null) {
              LocationSet pointees = getReferencedLocations(fieldOwner, pState, 1, pCfaEdge);
              if (pointees instanceof ExplicitLocationSet explicit) {
                for (PointerTarget target : explicit.getExplicitLocations()) {
                  if (target instanceof MemoryLocationPointer memPtr) {
                    instanceName = memPtr.getMemoryLocation().getIdentifier(); // e.g. "s"
                    break;
                  }
                  if (target instanceof StructLocation structTarget) {
                    instanceName = structTarget.getInstanceScope();
                    break;
                  }
                }
              }
            }

            if (instanceName == null) {
              return LocationSetTop.INSTANCE;
            }

            return switch (strategy) {
              case STRUCT_INSTANCE ->
                  ExplicitLocationSet.from(
                      StructLocation.forStructInstance(
                          pCfaEdge.getPredecessor().getFunctionName(), structType, instanceName));
              case ALL_FIELDS ->
                  ExplicitLocationSet.from(
                      StructLocation.forField(
                          pCfaEdge.getPredecessor().getFunctionName(),
                          structType,
                          instanceName,
                          fieldName));
              default -> LocationSetTop.INSTANCE;
            };
          }

          @Override
          public LocationSet visit(CCastExpression pCastExpression) {
            if (isNullPointer(pCastExpression)) {
              return ExplicitLocationSet.fromNull();
            }
            return LocationSetTop.INSTANCE;
          }

          @Override
          public LocationSet visit(CBinaryExpression pBinaryExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CCharLiteralExpression pCharLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CFloatLiteralExpression pFloatLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CIntegerLiteralExpression pIntegerLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CStringLiteralExpression pStringLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CTypeIdExpression pTypeIdExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CImaginaryLiteralExpression PLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CFunctionCallExpression pFunctionCallExpression) {
            return LocationSetTop.INSTANCE;
          }

          @Override
          public LocationSet visit(CComplexCastExpression complexCastExpression) {
            return LocationSetBot.INSTANCE;
          }
        });
  }

  private static LocationSet toLocationSet(MemoryLocation pLocation) {
    return toLocationSet(Collections.singleton(pLocation));
  }

  private static LocationSet toLocationSet(Set<MemoryLocation> pLocations) {
    if (pLocations == null) {
      return LocationSetTop.INSTANCE;
    }
    if (pLocations.isEmpty()) {
      return LocationSetBot.INSTANCE;
    }
    Set<PointerTarget> locations = new HashSet<>();
    for (MemoryLocation loc : pLocations) {
      locations.add(new MemoryLocationPointer(loc));
    }
    return ExplicitLocationSet.from(locations);
  }

  private static boolean isNullPointer(CExpression pExpression) {
    if (pExpression instanceof CCastExpression castExpression) {
      CExpression operand = castExpression.getOperand();
      if (operand instanceof CIntegerLiteralExpression intLiteral
          && intLiteral.getValue().longValue() == 0) {
        return true;
      }
    }
    return pExpression instanceof CIntegerLiteralExpression intLiteral
        && intLiteral.getValue().longValue() == 0;
  }

  public int determineDerefCounter(CRightHandSide pExpression, boolean pIsLhs) {
    if (pExpression instanceof CExpression cExpression) {
      if (isNullPointer(cExpression)) {
        return 0;
      }
      return determineDerefCounter(cExpression, pIsLhs);
    }
    return 0;
  }

  public static int determineDerefCounter(CExpression pExpression, boolean pIsLhs) {
    int derefCounter = 0;

    if (!pIsLhs) {

      if (pExpression instanceof CArraySubscriptExpression arrayExpr) {
        CType arrayType = arrayExpr.getArrayExpression().getExpressionType().getCanonicalType();

        if (arrayType instanceof CArrayType arrayTypeC) {
          CType elementType = arrayTypeC.getType().getCanonicalType();

          if (elementType instanceof CPointerType) {
            return computeExpressionDerefCounter(arrayExpr, derefCounter) + 1;
          }
        }

        return computeExpressionDerefCounter(arrayExpr, derefCounter);
      }

      if (pExpression instanceof CIdExpression idExpr
          && idExpr.getExpressionType() instanceof CPointerType) {
        return computeExpressionDerefCounter(idExpr, derefCounter) + 1;
      }
    }
    return computeExpressionDerefCounter(pExpression, derefCounter);
  }

  public static int computeExpressionDerefCounter(CExpression pExpression, int pCounter) {
    if (pExpression == null) {
      return pCounter;
    }

    if (pExpression instanceof CPointerExpression pointerExpr) {
      return computeExpressionDerefCounter(pointerExpr.getOperand(), pCounter + 1);
    } else if (pExpression instanceof CUnaryExpression unaryExpr
        && unaryExpr.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
      return computeExpressionDerefCounter(unaryExpr.getOperand(), pCounter - 1);
    } else if (pExpression instanceof CArraySubscriptExpression arrayExpr) {
      return computeExpressionDerefCounter(arrayExpr.getArrayExpression(), pCounter);
    } else if (pExpression instanceof CCastExpression castExpr) {
      return computeExpressionDerefCounter(castExpr.getOperand(), pCounter);
    } else {
      return pCounter;
    }
  }
}
