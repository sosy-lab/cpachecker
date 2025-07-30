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
      LocationSet pointsTo = getReferencedLocations(pointerExpr, pState, true, pCFAEdge);

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
      LocationSet leftPointsTo = getReferencedLocations(leftOperand, pState, true, pCFAEdge);
      LocationSet rightPointsTo = getReferencedLocations(rightOperand, pState, true, pCFAEdge);

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
      throws CPATransferException {

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
      LocationSet referencedLocations =
          getReferencedLocations(actualParam, pState, true, pCFunctionCallEdge);

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
      PointerAnalysisState pState, CFunctionReturnEdge pCfaEdge) throws CPATransferException {
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
      LocationSet rhsTargets = pState.getPointsToSet(returnVarPointer);
      if (rhsTargets instanceof ExplicitLocationSet explicitSet) {
        Set<PointerTarget> newTargets = new HashSet<>();
        boolean containsNull = explicitSet.containsNull();

        String callerFunctionName = pCfaEdge.getSummaryEdge().getPredecessor().getFunctionName();
        for (PointerTarget target : explicitSet.getExplicitLocations()) {
          if (PointerUtils.isValidFunctionReturn(target, callerFunctionName)) {
            newTargets.add(target);
          } else {
            newTargets.add(InvalidLocation.forInvalidation(InvalidationReason.LOCAL_SCOPE_EXPIRED));
          }
        }

        rhsTargets = ExplicitLocationSet.from(newTargets, containsNull);
      }

      LocationSet lhsLocations = getReferencedLocations(lhs, pState, false, pCfaEdge);
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
      PointerAnalysisState pState, CReturnStatementEdge pCfaEdge) throws CPATransferException {
    Optional<CExpression> expression = pCfaEdge.getExpression();
    if (expression.isEmpty()) {
      return pState;
    }
    CExpression returnExpression = expression.get();
    Type returnType = returnExpression.getExpressionType();
    if (!(returnType instanceof CPointerType)) {
      return pState;
    }
    LocationSet returnLocations = getReferencedLocations(returnExpression, pState, true, pCfaEdge);
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
      PointerAnalysisState pState, CStatementEdge pCfaEdge) throws CPATransferException {

    if (pCfaEdge.getStatement() instanceof CFunctionCallStatement callStatement) {
      CFunctionCallExpression callExpr = callStatement.getFunctionCallExpression();

      if (PointerUtils.isFreeFunction(callExpr.getFunctionNameExpression())) {

        CExpression freedExpr = callExpr.getParameterExpressions().get(0);

        LocationSet targets = getReferencedLocations(freedExpr, pState, true, pCfaEdge);

        if (targets instanceof ExplicitLocationSet explicitTargets) {
          Set<PointerTarget> updatedTargets = new HashSet<>();
          for (PointerTarget pt : explicitTargets.getExplicitLocations()) {
            if (pt instanceof HeapLocation) {
              updatedTargets.add(InvalidLocation.forInvalidation(InvalidationReason.FREED));
            } else {
              logger.logf(
                  Level.WARNING,
                  "PointerAnalysis: free() called on non-heap object at %s: %s",
                  pCfaEdge.getFileLocation(),
                  freedExpr.toASTString());
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

          LocationSet lhsLocations = getReferencedLocations(lhs, pState, false, pCfaEdge);
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
      case SINGLE -> heapLocation = HeapLocation.forAllocation(functionName, -1, null);
      case PER_CALL ->
          heapLocation = HeapLocation.forAllocation(functionName, allocationCounter++, 0L);
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

    LocationSet lhsLocations = getReferencedLocations(pLhs, pState, false, pCfaEdge);
    LocationSet rhsTargets = getReferencedLocations(pRhs, pState, true, pCfaEdge);
    if (pLhs instanceof CFieldReference pCFieldReference) {
      CType baseType = pCFieldReference.getFieldOwner().getExpressionType().getCanonicalType();

      while (baseType instanceof CPointerType ptrType) {
        baseType = ptrType.getType().getCanonicalType();
      }
      if (PointerUtils.isUnion(baseType)) {
        // TODO change assignment for union
        return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
      }
    }
    // TODO: Handle the case pLhs is CFieldReference
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
        if (explicitLhsLocations.isNull()) {
          logger.logf(
              Level.WARNING,
              "PointerAnalysis: Assignment to null at %s",
              pCfaEdge.getFileLocation());
          return PointerAnalysisState.BOTTOM_STATE;
        }
        PointerTarget lhsLocation = explicitLhsLocations.getExplicitLocations().iterator().next();
        if (lhsLocation instanceof InvalidLocation) {
          logger.logf(
              Level.WARNING,
              "PointerAnalysis: Assignment to invalid location %s at %s",
              lhsLocation,
              pCfaEdge.getFileLocation());
          return PointerAnalysisState.BOTTOM_STATE;
        }
        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
      } else {
        return addElementsToAmbiguousLocations(pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
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
                      return LocationSetTop.INSTANCE;
                    }

                    return getReferencedLocations(
                        pInitializerExpression.getExpression(), pState, true, pCfaEdge);
                  }

                  @Override
                  public LocationSet visit(CInitializerList pInitializerList)
                      throws CPATransferException {
                    return LocationSetTop.INSTANCE;
                  }

                  @Override
                  public LocationSet visit(CDesignatedInitializer pCStructInitializerPart)
                      throws CPATransferException {
                    return LocationSetTop.INSTANCE;
                  }
                });
        if (pointsToSet.isTop()) {
          return pState;
        }
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
      final boolean shouldDereference,
      final CFAEdge pCfaEdge)
      throws CPATransferException {

    return pExpression.accept(
        new CRightHandSideVisitor<LocationSet, CPATransferException>() {

          @Override
          public LocationSet visit(CIdExpression pIdExpression) {
            final MemoryLocation location;
            CSimpleDeclaration declaration = pIdExpression.getDeclaration();
            if (pIdExpression.getExpressionType().getCanonicalType() instanceof CArrayType) {
              MemoryLocation arrayBase = MemoryLocation.forDeclaration(declaration);
              return ExplicitLocationSet.from(
                  new MemoryLocationPointer(arrayBase.withAddedOffset(0)));
            }
            if (declaration != null) {
              location = MemoryLocation.forDeclaration(declaration);
            } else {
              location = MemoryLocation.forIdentifier(pIdExpression.getName());
            }
            LocationSet base = toLocationSet(location);

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
                    pPointerExpression.getOperand(), pState, shouldDereference, pCfaEdge);
            return dereference(operand);
          }

          @Override
          public LocationSet visit(CUnaryExpression pUnaryExpression) throws CPATransferException {
            if (pUnaryExpression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
              LocationSet operand =
                  getReferencedLocations(pUnaryExpression.getOperand(), pState, false, pCfaEdge);
              return addressOf(operand);
            }
            return getReferencedLocations(
                pUnaryExpression.getOperand(), pState, shouldDereference, pCfaEdge);
          }

          @Override
          public LocationSet visit(CFieldReference pFieldReference) throws CPATransferException {
            CExpression owner = pFieldReference.getFieldOwner();
            String fieldName = pFieldReference.getFieldName();
            CType ownerType = owner.getExpressionType().getCanonicalType();
            CType baseType = ownerType;

            while (baseType instanceof CPointerType ptrType) {
              baseType = ptrType.getType().getCanonicalType();
            }

            String structType = baseType.toString();

            StructHandlingStrategy strategy = getOptions().structHandlingStrategy;
            String instanceName = null;

            if (pFieldReference.isPointerDereference()) {
              // owner is always Rhs
              LocationSet pointees = getReferencedLocations(owner, pState, true, pCfaEdge);
              // TODO explicit.getSize() >1?
              if (pointees instanceof ExplicitLocationSet explicit && explicit.getSize() == 1) {
                for (PointerTarget target : explicit.getExplicitLocations()) {
                  if (target instanceof MemoryLocationPointer memPtr) {
                    instanceName = memPtr.getMemoryLocation().getIdentifier();
                    break;
                  } else if (target instanceof StructLocation structLoc) {
                    instanceName = structLoc.getInstanceScope();
                    break;
                  }
                }
              }
            } else {
              if (owner instanceof CIdExpression idExpr) {
                instanceName = idExpr.getName();
              }
            }

            if (instanceName == null) {
              return LocationSetTop.INSTANCE;
            }

            LocationSet baseLocation =
                switch (strategy) {
                  case STRUCT_INSTANCE ->
                      ExplicitLocationSet.from(
                          StructLocation.forStructInstance(
                              pCfaEdge.getPredecessor().getFunctionName(),
                              structType,
                              instanceName));
                  case ALL_FIELDS ->
                      ExplicitLocationSet.from(
                          StructLocation.forField(
                              pCfaEdge.getPredecessor().getFunctionName(),
                              structType,
                              instanceName,
                              fieldName));
                  case JUST_STRUCT ->
                      ExplicitLocationSet.from(
                          StructLocation.forStruct(
                              pCfaEdge.getPredecessor().getFunctionName(), structType));
                  default -> LocationSetTop.INSTANCE;
                };

            if (shouldDereference
                && (strategy == StructHandlingStrategy.STRUCT_INSTANCE
                    || strategy == StructHandlingStrategy.ALL_FIELDS)) {
              return dereference(baseLocation);
            } else {
              return baseLocation;
            }

            //            if (shouldDereference) {
            //              return dereference(baseLocation);
            //            } else {
            //              return baseLocation;
            //            }
          }

          @Override
          public LocationSet visit(CArraySubscriptExpression expr) throws CPATransferException {
            LocationSet base =
                getReferencedLocations(
                    expr.getArrayExpression(), pState, shouldDereference, pCfaEdge);

            if (base.isTop() || base.isBot()) return base;
            if (!(expr.getSubscriptExpression() instanceof CIntegerLiteralExpression idxExpr)) {
              return LocationSetTop.INSTANCE;
            }

            long offset = idxExpr.getValue().longValue();
            Set<PointerTarget> targets = new HashSet<>();
            for (PointerTarget baseLoc : ((ExplicitLocationSet) base).getExplicitLocations()) {
              if (baseLoc instanceof MemoryLocationPointer memPtr) {
                MemoryLocation withOffset = memPtr.getMemoryLocation().withAddedOffset(offset);
                targets.add(new MemoryLocationPointer(withOffset));
              }
            }

            LocationSet locationSet = ExplicitLocationSet.from(targets);

            if (shouldDereference) {
              return dereference(locationSet);
            } else {
              return locationSet;
            }
          }

          @Override
          public LocationSet visit(CCastExpression expr) throws CPATransferException {
            if (isNullPointer(expr)) {
              return ExplicitLocationSet.fromNull();
            }
            return getReferencedLocations(expr.getOperand(), pState, shouldDereference, pCfaEdge);
          }

          @Override
          public LocationSet visit(CBinaryExpression pBinaryExpression)
              throws CPATransferException {
            CBinaryExpression.BinaryOperator operator = pBinaryExpression.getOperator();
            if (operator != CBinaryExpression.BinaryOperator.PLUS
                && operator != CBinaryExpression.BinaryOperator.MINUS) {
              return LocationSetBot.INSTANCE;
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

              LocationSet base = getReferencedLocations(pointerExpr, pState, true, pCfaEdge);

              if (offsetExpr instanceof CIntegerLiteralExpression intLit) {
                long offset =
                    operator == CBinaryExpression.BinaryOperator.MINUS
                        ? -intLit.getValue().longValue()
                        : intLit.getValue().longValue();
                return pointerArithmetic(base, offset);
              }

              return LocationSetTop.INSTANCE;
            }
            if (operand1IsPtr || operand2IsPtr) {
              return ExplicitLocationSet.from(
                  InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
            }
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
            // TODO create StringLiteralLocation
            String literal = pStringLiteralExpression.getContentWithoutNullTerminator();
            MemoryLocation stringLoc = MemoryLocation.forIdentifier("__string_literal_" + literal);
            return ExplicitLocationSet.from(new MemoryLocationPointer(stringLoc));
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
          public LocationSet visit(CAddressOfLabelExpression pAddressOfLabelExpression)
              throws CPATransferException {
            throw new CPATransferException("Address of labels not supported by pointer analysis");
          }

          @Override
          public LocationSet visit(CFunctionCallExpression pFunctionCallExpression) {
            // TODO
            return LocationSetTop.INSTANCE;
          }

          @Override
          public LocationSet visit(CComplexCastExpression complexCastExpression) {
            // TODO
            return LocationSetBot.INSTANCE;
          }

          private LocationSet dereference(LocationSet set) {
            if (set.isTop() || set.isBot() || set.isNull()) return set;
            if (!(set instanceof ExplicitLocationSet explicitSet)) return LocationSetTop.INSTANCE;

            LocationSet result = LocationSetBot.INSTANCE;
            for (PointerTarget pt : explicitSet.getExplicitLocations()) {
              LocationSet target = pState.getPointsToSet(pt);
              // if (target.isTop() || target.isBot() || target.isNull()) return target;
              if (target.isTop() || target.isBot()) return target;
              result = result.addElements(target);
            }
            return result;
          }

          private LocationSet addressOf(LocationSet set) {
            if (!(set instanceof ExplicitLocationSet explicit)) return LocationSetTop.INSTANCE;
            //            Set<PointerTarget> result = new HashSet<>();
            //            for (PointerTarget pt : explicit.getExplicitLocations()) {
            //              if (pt instanceof MemoryLocationPointer mem) {
            //                result.add(new MemoryLocationPointer(mem.getMemoryLocation()));
            //              }
            //            }
            //            return ExplicitLocationSet.from(result, explicit.containsNull());
            return explicit;
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

  private static LocationSet pointerArithmetic(LocationSet baseLocations, long offset) {
    if (baseLocations.isTop() || baseLocations.isBot()) {
      return baseLocations;
    }

    if (baseLocations.isNull()) {
      return ExplicitLocationSet.from(
          InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
    }

    if (baseLocations instanceof ExplicitLocationSet explicitSet) {
      Set<PointerTarget> targets = new HashSet<>();
      for (PointerTarget pt : explicitSet.getExplicitLocations()) {
        if (pt instanceof InvalidLocation || pt instanceof StructLocation) {
          targets.add(InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
        }
        if (pt instanceof MemoryLocationPointer memPtrTarget) {
          if (offset == 0) {
            targets.add(memPtrTarget);
            continue;
          }
          if (memPtrTarget.getMemoryLocation().isReference()) {
            long currentOffset = memPtrTarget.getMemoryLocation().getOffset();
            long newOffset = currentOffset + offset;

            if (newOffset < 0) {
              targets.add(InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
              continue;
            }

            // TODO: Check if newOffset >= arraySize when array size is known
            MemoryLocation targetWithOffset =
                memPtrTarget.getMemoryLocation().withAddedOffset(offset);
            targets.add(new MemoryLocationPointer(targetWithOffset));
          } else {
            targets.add(InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
          }
        }
        if (pt instanceof HeapLocation heapTarget) {
          if (offset == 0) {
            targets.add(heapTarget);
            continue;
          }
          if (heapTarget.isReference()) {
            long currentOffset = heapTarget.getOffset();
            long newOffset = currentOffset + offset;

            if (newOffset < 0) {
              targets.add(InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
              continue;
            }

            // TODO: Check if newOffset >= arraySize when array size is known
            HeapLocation targetWithOffset = heapTarget.withAddedOffset(offset);
            targets.add(targetWithOffset);
          } else {
            targets.add(heapTarget);
          }
        }
      }
      return ExplicitLocationSet.from(targets);
    } else {
      return LocationSetTop.INSTANCE;
    }
  }
}
