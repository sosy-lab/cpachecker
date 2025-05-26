// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import java.util.Optional;

public class PointerAnalysisTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  public PointerAnalysisTransferRelation(LogManager pLogger) {
    logger = pLogger;
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
      case FunctionCallEdge -> {}
      case FunctionReturnEdge -> {}
      case ReturnStatementEdge ->
          resultState = handleReturnStatementEdge(pState, (CReturnStatementEdge) pCfaEdge);
    }

    if (!resultState.isBottom()) {
      String string = resultState.toString();
      logger.log(Level.INFO, string);
    } else {
      logger.log(Level.INFO, "Impossible path detected, returning null state");
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
      return handleBasicBinaryCondition(pState, binaryExpression, truthAssumption);
    }

    return pState;
  }

  private PointerAnalysisState handleBasicBinaryCondition(
      PointerAnalysisState pState, CBinaryExpression pExpression, boolean pTruthAssumption)
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
      LocationSet pointsTo = getReferencedLocations(pointerExpr, pState, derefCounter);

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
      LocationSet leftPointsTo = getReferencedLocations(leftOperand, pState, leftDerefCounter);
      LocationSet rightPointsTo = getReferencedLocations(rightOperand, pState, rightDerefCounter);

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
    for (MemoryLocation loc : pSet1.getExplicitLocations()) {
      if (pSet2.mayPointTo(loc)) {
        return true;
      }
    }
    return false;
  }

  private PointerAnalysisState handleReturnStatementEdge(
      PointerAnalysisState pState, CReturnStatementEdge pCfaEdge) throws UnrecognizedCodeException {
    var expression = pCfaEdge.getExpression();
    if (expression.isEmpty()) {
      return pState;
    }

    CExpression returnExpression = expression.get();

    int derefCounter = determineDerefCounter(returnExpression, false);

    LocationSet returnLocations = getReferencedLocations(returnExpression, pState, derefCounter);

    if (returnLocations.isBot()) {
      return pState;
    }

    Optional<? extends AVariableDeclaration> returnVariable =
        pCfaEdge.getSuccessor().getEntryNode().getReturnVariable();

    if (returnVariable.isPresent()) {
      MemoryLocation returnLocation = MemoryLocation.forDeclaration(returnVariable.get());
      LocationSet returnLocationSet = toLocationSet(returnLocation);
      // TODO replace with new state
      return pState.addPointsToInformation(returnLocationSet, returnLocations);
    }

    return pState;
  }

  private PointerAnalysisState handleStatementEdge(
      PointerAnalysisState pState, CStatementEdge pCfaEdge) throws UnrecognizedCodeException {
    if (pCfaEdge.getStatement() instanceof CAssignment assignment) {
      // TODO: Handle the case of function call assignments (CFunctionCallAssignmentStatement)
      Type type = assignment.getLeftHandSide().getExpressionType().getCanonicalType();
      if (!(type instanceof CPointerType)) {
        return pState;
      }
      return handleAssignment(pState, assignment.getLeftHandSide(), assignment.getRightHandSide());
    }
    return pState;
  }

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState, CExpression pLhs, CRightHandSide pRhs)
      throws UnrecognizedCodeException {

    int lhsDerefCounter = determineDerefCounter(pLhs, true);
    int rhsDerefCounter = determineDerefCounter(pRhs, false);

    LocationSet lhsLocations = getReferencedLocations(pLhs, pState, lhsDerefCounter);
    LocationSet rhsTargets = getReferencedLocations(pRhs, pState, rhsDerefCounter);

    // TODO: Handle the case pLhs is CFieldReference

    if (lhsLocations instanceof ExplicitLocationSet explicitLhsLocations) {
      if (explicitLhsLocations.getSize() == 1) {
        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(lhsLocations, rhsTargets));
      } else {
        return addElementsToAmbiguousLocations(pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }

  private PointerAnalysisState addElementsToAmbiguousLocations(
      PointerAnalysisState pState, ExplicitLocationSet pLhsLocations, LocationSet pRhsTargets) {
    Set<MemoryLocation> locations = pLhsLocations.getExplicitLocations();
    PointerAnalysisState updatedState = pState;

    for (MemoryLocation loc : locations) {
      LocationSet targetSet = ExplicitLocationSet.from(loc);
      LocationSet existingSet = updatedState.getPointsToSet(targetSet);
      LocationSet mergedSet = existingSet.addElements(pRhsTargets);

      updatedState =
          new PointerAnalysisState(updatedState.getPointsToMap().putAndCopy(targetSet, mergedSet));
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
                        pInitializerExpression.getExpression(), pState, derefCounter);
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
        MemoryLocation pointerLocation = MemoryLocation.forDeclaration(declaration);
        LocationSet pointerLocationSet = toLocationSet(pointerLocation);
        return new PointerAnalysisState(
            pState
                .getPointsToMap()
                .putAndCopy(
                    pointerLocationSet,
                    pointsToSet)); // handleAssignment(pState, pointerLocationSet, pointsToSet);
      }
      return pState;
    }
    return pState;
  }

  private static LocationSet getReferencedLocations(
      final CRightHandSide pExpression, final PointerAnalysisState pState, final int pDerefCounter)
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
            return applyDereferences(toLocationSet(location), pDerefCounter);
          }

          private LocationSet applyDereferences(LocationSet pLocationSet, int pDerefCount) {
            if (pDerefCount <= 0 || pLocationSet.isTop() || pLocationSet.isBot()) {
              return pLocationSet;
            }
            LocationSet result = pLocationSet;
            for (int i = 0; i < pDerefCount; i++) {
              // TODO check the correctness of dereferencing
              result = pState.getPointsToSet(result);
              if (result.isTop() || result.isBot() || result.isNull()) {
                return result;
              }
              if (!(result instanceof ExplicitLocationSet)) {
                return LocationSetTop.INSTANCE;
              }
            }
            return result;
          }

          @Override
          public LocationSet visit(CUnaryExpression pUnaryExpression)
              throws UnrecognizedCodeException {
            return getReferencedLocations(pUnaryExpression.getOperand(), pState, pDerefCounter);
          }

          @Override
          public LocationSet visit(CPointerExpression pPointerExpression)
              throws UnrecognizedCodeException {
            return getReferencedLocations(pPointerExpression.getOperand(), pState, pDerefCounter);
          }

          @Override
          public LocationSet visit(CArraySubscriptExpression pArraySubscriptExpression)
              throws UnrecognizedCodeException {
            CExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();
            CExpression subscriptExpression = pArraySubscriptExpression.getSubscriptExpression();
            LocationSet baseLocations = getReferencedLocations(arrayExpression, pState, 0);
            if (baseLocations.isBot() || baseLocations.isTop()) {
              return baseLocations;
            }
            if (!(subscriptExpression instanceof CIntegerLiteralExpression indexLiteral)) {
              return LocationSetTop.INSTANCE;
            }
            long indexValue = indexLiteral.getValue().longValue();
            Set<MemoryLocation> elementLocations = new HashSet<>();
            for (MemoryLocation baseLocation :
                ((ExplicitLocationSet) baseLocations).getExplicitLocations()) {
              MemoryLocation elementLocation = baseLocation.withAddedOffset(indexValue);
              elementLocations.add(elementLocation);
              // TODO check null pointer
            }

            if (pDerefCounter > 0) {
              Set<MemoryLocation> pointsToLocations = new HashSet<>();
              boolean containsNull = false;
              for (MemoryLocation location : elementLocations) {
                LocationSet targetLocation =
                    applyDereferences(toLocationSet(location), pDerefCounter);
                if (targetLocation instanceof ExplicitLocationSet explicitTargetLocation) {
                  pointsToLocations.addAll(explicitTargetLocation.getExplicitLocations());
                  containsNull = containsNull || explicitTargetLocation.containsNull();
                }
              }

              return (!containsNull && pointsToLocations.isEmpty())
                  ? LocationSetBot.INSTANCE
                  : ExplicitLocationSet.from(pointsToLocations, containsNull);
            } else {
              return ExplicitLocationSet.from(elementLocations);
            }
          }

          @Override
          public LocationSet visit(CFieldReference pFieldReference) {
            return LocationSetBot.INSTANCE;
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
            return LocationSetBot.INSTANCE;
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
    if (!pLocations.iterator().hasNext()) {
      return LocationSetBot.INSTANCE;
    }
    return ExplicitLocationSet.from(pLocations);
  }

  private static boolean isNullPointer(CExpression pExpression) {
    if (pExpression instanceof CCastExpression castExpression) {
      CExpression operand = castExpression.getOperand();
      if (operand instanceof CIntegerLiteralExpression intLiteral
          && intLiteral.getValue().longValue() == 0) {
        return true;
      }
    }
    if (pExpression instanceof CIntegerLiteralExpression intLiteral
        && intLiteral.getValue().longValue() == 0) {
      return true;
    }

    return false;
  }

  private int determineDerefCounter(CRightHandSide pExpression, boolean pIsLhs) {
    if (pExpression instanceof CExpression cExpression) {
      if (isNullPointer(cExpression)) {
        return 0;
      }
      return determineDerefCounter(cExpression, pIsLhs);
    }
    return 0;
  }

  private int determineDerefCounter(CExpression pExpression, boolean pIsLhs) {
    int derefCounter = 0;

    if (pIsLhs) {
      return computeExpressionDerefCounter(pExpression, derefCounter);
    } else {

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

      return computeExpressionDerefCounter(pExpression, derefCounter);
    }
  }

  private int computeExpressionDerefCounter(CExpression pExpression, int pCounter) {
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
