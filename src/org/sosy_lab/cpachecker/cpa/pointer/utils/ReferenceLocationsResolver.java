// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.utils;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.transfer.PointerTransferOptions;
import org.sosy_lab.cpachecker.cpa.pointer.StructHandlingStrategy;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidationReason;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.StructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class ReferenceLocationsResolver {
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

            StructHandlingStrategy strategy = pointerTransferOptions.getStructHandlingStrategy();
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
            if (StructUnionAssignmentHandler.isUnion(baseType)) {
              baseLocation = strategy.getUnionLocation(baseType, instanceName, pCfaEdge);

            } else if (StructUnionAssignmentHandler.isStruct(baseType)) {
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
                  PointerArithmetic.applyOffset(
                      explicitBase, offset, pointerTransferOptions.isOffsetSensitive());

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
            if (PointerAnalysisChecks.isNullPointer(expr)) {
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
                return PointerArithmetic.applyOffset(
                    base, offset, pointerTransferOptions.isOffsetSensitive());
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
            if (PointerAnalysisChecks.isNullPointer(operand)) {
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
