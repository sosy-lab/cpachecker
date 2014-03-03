/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.RealPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.pointerTarget.PointerTargetPattern;

/**
 * This class is responsible for handling everything related to dynamic memory,
 * e.g. calls to malloc() and free(),
 * and for handling deferred allocations (calls to malloc() where the assumed
 * type of the memory is not yet known).
 */
class DynamicMemoryHandler {

  private static final String CALLOC_FUNCTION = "calloc";

  private final CToFormulaConverterWithPointerAliasing conv;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;

  DynamicMemoryHandler(CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge, SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts, Constraints pConstraints,
      ErrorConditions pErrorConditions) {
    conv = pConv;
    edge = pEdge;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
  }

  Value handleDynamicMemoryFunction(final CFunctionCallExpression e, final String functionName,
      final CExpressionVisitorWithPointerAliasing expressionVisitor) throws UnrecognizedCCodeException {

    if ((conv.options.isSuccessfulAllocFunctionName(functionName) ||
        conv.options.isSuccessfulZallocFunctionName(functionName))) {
      return Value.ofValue(handleSucessfulMemoryAllocation(functionName, e.getParameterExpressions(), e));

    } else if ((conv.options.isMemoryAllocationFunction(functionName) ||
            conv.options.isMemoryAllocationFunctionWithZeroing(functionName))) {
      return Value.ofValue(handleMemoryAllocation(e, functionName));

    } else if (conv.options.isMemoryFreeFunction(functionName)) {
      return handleMemoryFree(e, expressionVisitor);
    } else {
      throw new AssertionError("Unknown memory allocation function " + functionName);
    }
  }

  /**
   * Handle memory allocation functions that may fail (i.e., return null)
   * and that may or may not zero the memory.
   */
  private Formula handleMemoryAllocation(final CFunctionCallExpression e,
      final String functionName) throws UnrecognizedCCodeException {
    final boolean isZeroing = conv.options.isMemoryAllocationFunctionWithZeroing(functionName);
    List<CExpression> parameters = e.getParameterExpressions();

    if (functionName.equals(CALLOC_FUNCTION) && parameters.size() == 2) {
      CExpression param0 = parameters.get(0);
      CExpression param1 = parameters.get(1);

      // Build expression for param0 * param1 as new parameter.
      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(conv.machineModel, conv.logger);
      CBinaryExpression multiplication = builder.buildBinaryExpression(
          param0, param1, BinaryOperator.MULTIPLY);

      // Try to evaluate the multiplication if possible.
      Integer value0 = tryEvaluateExpression(param0);
      Integer value1 = tryEvaluateExpression(param1);
      if (value0 != null && value1 != null) {
        long result = ExpressionValueVisitor.calculateBinaryOperation(
            new NumericValue(value0.longValue()), new NumericValue(value1.longValue()), multiplication,
            conv.machineModel, conv.logger, edge).asLong(multiplication.getExpressionType());

        CExpression newParam = new CIntegerLiteralExpression(param0.getFileLocation(),
                                                 multiplication.getExpressionType(),
                                                 BigInteger.valueOf(result));
        parameters = Collections.singletonList(newParam);
      } else {
        parameters = Collections.<CExpression>singletonList(multiplication);
      }

    } else if (parameters.size() != 1) {
      if (parameters.size() > 1 && conv.options.hasSuperfluousParameters(functionName)) {
        parameters = Collections.singletonList(parameters.get(0));
      } else {
        throw new UnrecognizedCCodeException(
            String.format("Memory allocation function %s() called with %d parameters instead of 1",
                          functionName, parameters.size()), edge, e);
      }
    }

    final String delegateFunctionName = !isZeroing ?
                                          conv.options.getSuccessfulAllocFunctionName() :
                                          conv.options.getSuccessfulZallocFunctionName();

    if (!conv.options.makeMemoryAllocationsAlwaysSucceed()) {
      final Formula nondet = conv.makeFreshVariable(functionName,
                                                    CPointerType.POINTER_TO_VOID,
                                                    ssa);
      return conv.bfmgr.ifThenElse(conv.bfmgr.not(conv.fmgr.makeEqual(nondet, conv.nullPointer)),
                                    handleSucessfulMemoryAllocation(delegateFunctionName, parameters, e),
                                    conv.nullPointer);
    } else {
      return handleSucessfulMemoryAllocation(delegateFunctionName, parameters, e);
    }
  }

  /**
   * Handle memory allocation functions that cannot fail
   * (i.e., do not return NULL) and do not zero the memory.
   */
  private Formula handleSucessfulMemoryAllocation(final String functionName,
      List<CExpression> parameters,
      final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    // e.getFunctionNameExpression() should not be used
    // as it might refer to another function if this method is called from handleMemoryAllocation()
    if (parameters.size() != 1) {
      if (parameters.size() > 1 && conv.options.hasSuperfluousParameters(functionName)) {
        parameters = Collections.singletonList(parameters.get(0));
      } else {
        throw new UnrecognizedCCodeException(
            String.format("Memory allocation function %s() called with %d parameters instead of 1",
                          functionName, parameters.size()), edge, e);
      }
    }

    final CExpression parameter = parameters.get(0);
    Integer size = null;
    final CType newType;
    if (isSizeof(parameter)) {
      newType = getSizeofType(parameter);
    } else if (isSizeofMultilple(parameter)) {
      final CBinaryExpression product = (CBinaryExpression) parameter;
      final CType operand1Type = getSizeofType(product.getOperand1());
      final CType operand2Type = getSizeofType(product.getOperand2());
      if (operand1Type != null) {
        newType = new CArrayType(false, false, operand1Type, product.getOperand2());
      } else if (operand2Type != null) {
        newType = new CArrayType(false, false, operand2Type, product.getOperand1());
      } else {
        throw new UnrecognizedCCodeException("Can't determine type for internal memory allocation", edge, e);
      }
    } else {
      size = tryEvaluateExpression(parameter);
      if (!conv.options.revealAllocationTypeFromLHS() && !conv.options.deferUntypedAllocations()) {
        final CExpression length;
        if (size == null) {
          size = conv.options.defaultAllocationSize();
          length = new CIntegerLiteralExpression(parameter.getFileLocation(),
                                                 parameter.getExpressionType(),
                                                 BigInteger.valueOf(size));
        } else {
          length = parameter;
        }
        newType = new CArrayType(false, false, CNumericTypes.VOID, length);
      } else {
        newType = null;
      }
    }
    Formula address;
    if (newType != null) {
      final CType newBaseType = CTypeUtils.getBaseType(newType);
      final String newBase = makeAllocVariableName(functionName, newType, newBaseType);
      address =  makeAllocation(conv.options.isSuccessfulZallocFunctionName(functionName),
                                 newType,
                                 newBase);
    } else {
      final String newBase = makeAllocVariableName(functionName,
                                                            CNumericTypes.VOID,
                                                            CPointerType.POINTER_TO_VOID);
      pts.addTemporaryDeferredAllocation(conv.options.isSuccessfulZallocFunctionName(functionName),
                                         size != null ? new CIntegerLiteralExpression(parameter.getFileLocation(),
                                                                                      parameter.getExpressionType(),
                                                                                      BigInteger.valueOf(size)) :
                                                        null,
                                         newBase);
      address = conv.makeConstant(PointerTargetSet.getBaseName(newBase), CPointerType.POINTER_TO_VOID);
    }

    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(address), address));
    }
    return address;
  }

  /**
   * Handle calls to free()
   */
  private Value handleMemoryFree(final CFunctionCallExpression e,
      final CExpressionVisitorWithPointerAliasing expressionVisitor) throws UnrecognizedCCodeException {
    final List<CExpression> parameters = e.getParameterExpressions();
    if (parameters.size() != 1) {
      throw new UnrecognizedCCodeException(
          String.format("free() called with %d parameters", parameters.size()), edge, e);
    }

    if (errorConditions.isEnabled()) {
      final Formula operand = expressionVisitor.asValueFormula(parameters.get(0).accept(expressionVisitor),
                                                 CTypeUtils.simplifyType(parameters.get(0).getExpressionType()));
      BooleanFormula validFree = conv.fmgr.makeEqual(operand, conv.nullPointer);

      for (String base : pts.getAllBases()) {
        Formula baseF = conv.makeConstant(PointerTargetSet.getBaseName(base), CPointerType.POINTER_TO_VOID);
        validFree = conv.bfmgr.or(validFree, conv.fmgr.makeEqual(operand, baseF));
      }
      errorConditions.addInvalidFreeCondition(conv.bfmgr.not(validFree));
    }

    return Value.nondetValue(); // free does not return anything, so nondet is ok
  }

  private Formula makeAllocation(final boolean isZeroing, final CType type, final String base)
      throws UnrecognizedCCodeException {
    final CType baseType = CTypeUtils.getBaseType(type);
    final Formula result = conv.makeConstant(PointerTargetSet.getBaseName(base), baseType);
    if (isZeroing) {
      AssignmentHandler assignmentHandler = new AssignmentHandler(conv, edge, base, ssa, pts, constraints, errorConditions);
      final BooleanFormula initialization = assignmentHandler.makeAssignment(
        type,
        CNumericTypes.SIGNED_CHAR,
        AliasedLocation.ofAddress(result),
        Value.ofValue(conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(CNumericTypes.SIGNED_CHAR), 0)),
        PointerTargetPattern.forBase(base),
        true,
        null);
      constraints.addConstraint(initialization);
    }
    conv.addPreFilledBase(base, type, false, isZeroing, constraints, pts);
    return result;
  }

  private String makeAllocVariableName(final String functionName,
                               final CType type,
                               final CType baseType) {
    final String allocVariableName = functionName + "_" + CToFormulaConverterWithPointerAliasing.getUFName(type);
    return  allocVariableName + CToFormulaConverterWithPointerAliasing.FRESH_INDEX_SEPARATOR + RealPointerTargetSetBuilder.getNextDynamicAllocationIndex();
  }

  private static Integer tryEvaluateExpression(CExpression e) {
    if (e instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)e).getValue().intValue();
    }
    return null;
  }

  private static boolean isSizeof(final CExpression e) {
    return e instanceof CUnaryExpression && ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF ||
           e instanceof CTypeIdExpression && ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF;
  }

  private static boolean isSizeofMultilple(final CExpression e) {
    return e instanceof CBinaryExpression &&
           ((CBinaryExpression) e).getOperator() == BinaryOperator.MULTIPLY &&
           (isSizeof(((CBinaryExpression) e).getOperand1()) ||
            isSizeof(((CBinaryExpression) e).getOperand2()));
  }

  private static CType getSizeofType(CExpression e) {
    if (e instanceof CUnaryExpression &&
        ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF) {
      return CTypeUtils.simplifyType(((CUnaryExpression) e).getOperand().getExpressionType());
    } else if (e instanceof CTypeIdExpression &&
               ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF) {
      return CTypeUtils.simplifyType(((CTypeIdExpression) e).getType());
    } else {
      return null;
    }
  }


  // Handling of deferred allocations

  private CType refineType(final @Nonnull CType type, final @Nonnull CIntegerLiteralExpression sizeLiteral) {
    if (sizeLiteral.getValue() != null) {
      final int size = sizeLiteral.getValue().intValue();
      final int typeSize = conv.getSizeof(type);
      if (type instanceof CArrayType) {
        if (typeSize != size) {
          conv.logger.logf(Level.WARNING,
                           "Array size of the revealed type differs form the allocation size: %s : %d != %d",
                           type,
                           typeSize,
                           size);
        }
        return type;
      } else {
        final int n = size / typeSize;
        final int remainder = size % typeSize;
        if (n == 0 || remainder != 0) {
          conv.logger.logf(Level.WARNING,
                           "Can't refine allocation type, but the sizes differ: %s : %d != %d",
                           type,
                           typeSize,
                           size);
          return type;
        }
        return new CArrayType(false, false, type, new CIntegerLiteralExpression(sizeLiteral.getFileLocation(),
                                                                                sizeLiteral.getExpressionType(),
                                                                                BigInteger.valueOf(n)));
      }
    } else {
      return type;
    }
  }

  private CType getAllocationType(final @Nonnull CType type, final @Nullable CIntegerLiteralExpression sizeLiteral) {
    if (type instanceof CPointerType) {
      return sizeLiteral != null ? refineType(((CPointerType) type).getType(), sizeLiteral) :
                                   ((CPointerType) type).getType();
    } else if (type instanceof CArrayType) {
      return sizeLiteral != null ? refineType(type, sizeLiteral) : type;
    } else {
      throw new IllegalArgumentException("Either pointer or array type expected");
    }
  }

  private void handleDeferredAllocationTypeRevelation(final @Nonnull String pointerVariable,
                                                      final @Nonnull CType type)
                                                          throws UnrecognizedCCodeException {
    final DeferredAllocationPool deferredAllocationPool = pts.removeDeferredAllocation(pointerVariable);
    for (final String baseVariable : deferredAllocationPool.getBaseVariables()) {
      makeAllocation(deferredAllocationPool.wasAllocationZeroing(),
                          getAllocationType(type, deferredAllocationPool.getSize()),
                          baseVariable);
    }
  }

  private void handleDeferredAllocationPointerEscape(final String pointerVariable)
      throws UnrecognizedCCodeException {
    final DeferredAllocationPool deferredAllocationPool = pts.removeDeferredAllocation(pointerVariable);
    final CIntegerLiteralExpression size = deferredAllocationPool.getSize() != null ?
                                             deferredAllocationPool.getSize() :
                                             new CIntegerLiteralExpression(
                                                   FileLocation.DUMMY,
                                                   CNumericTypes.SIGNED_CHAR,
                                                   BigInteger.valueOf(conv.options.defaultAllocationSize()));
    conv.logger.logfOnce(Level.WARNING,
                         "The void * pointer %s to a deferred allocation escaped form tracking! " +
                           "Allocating array void[%d]. (in the following line(s):\n %s)",
                         pointerVariable,
                         size.getValue(),
                         edge);
    for (final String baseVariable : deferredAllocationPool.getBaseVariables()) {
      makeAllocation(deferredAllocationPool.wasAllocationZeroing(),
                          new CArrayType(false,
                                         false,
                                         CNumericTypes.VOID,
                                         size),
                          baseVariable);
    }
  }

  void handleDeferredAllocationsInAssignment(final CLeftHandSide lhs, final CRightHandSide rhs,
      final Location lhsLocation, final Expression rhsExpression, final CType lhsType,
      final Map<String, CType> lhsUsedDeferredAllocationPointers,
      final Map<String, CType> rhsUsedDeferredAllocationPointers) throws UnrecognizedCCodeException {
    // Handle allocations: reveal the actual type form the LHS type or defer the allocation until later
    boolean isAllocation = false;
    if ((conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) &&
        rhs instanceof CFunctionCallExpression &&
        !rhsExpression.isNondetValue() && rhsExpression.isValue()) {
      final Set<String> rhsVariables = conv.fmgr.extractVariables(rhsExpression.asValue().getValue());
      // Actually there is always either 1 variable (just address) or 2 variables (nondet + allocation address)
      for (String variable : rhsVariables) {
        if (PointerTargetSet.isBaseName(variable)) {
          variable = PointerTargetSet.getBase(variable);
        }
        if (pts.isTemporaryDeferredAllocationPointer(variable)) {
          if (!isAllocation) {
            // We can reveal the type from the LHS
            if (CExpressionVisitorWithPointerAliasing.isRevealingType(lhsType)) {
              handleDeferredAllocationTypeRevelation(variable, lhsType);
            // We can defer the allocation and start tracking the variable in the LHS
            } else if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
                       // TODO: remove the double-check (?)
                       CExpressionVisitorWithPointerAliasing.isUnaliasedLocation(lhs) &&
                       lhsLocation.isUnaliasedLocation()) {
              final String variableName = lhsLocation.asUnaliasedLocation().getVariableName();
              if (pts.isDeferredAllocationPointer(variableName)) {
                handleDeferredAllocationPointerRemoval(variableName, false);
              }
              pts.addDeferredAllocationPointer(variableName, variable); // Now we track the LHS
              // And not the RHS, because the LHS is its only alias
              handleDeferredAllocationPointerRemoval(variable, false);
            } else {
              handleDeferredAllocationPointerEscape(variable);
            }
            isAllocation = true;
          } else {
            throw new UnrecognizedCCodeException("Can't handle ambiguous allocation", edge, rhs);
          }
        }
      }
    }

    // Track currently deferred allocations
    if (conv.options.deferUntypedAllocations() && !isAllocation) {
      handleDeferredAllocationsInAssignment(lhs,
                                            rhs,
                                            lhsLocation,
                                            rhsExpression,
                                            lhsUsedDeferredAllocationPointers,
                                            rhsUsedDeferredAllocationPointers);
    }
  }

  private void handleDeferredAllocationsInAssignment(final CLeftHandSide lhs,
                                                     final CRightHandSide rhs,
                                                     final Location lhsLocation,
                                                     final Expression rhsExpression,
                                                     final Map<String, CType> lhsUsedDeferredAllocationPointers,
                                                     final Map<String, CType> rhsUsedDeferredAllocationPointers)
                                                         throws UnrecognizedCCodeException {
    boolean passed = false;
    for (final Map.Entry<String, CType> usedPointer : rhsUsedDeferredAllocationPointers.entrySet()) {
      boolean handled = false;
      if (CExpressionVisitorWithPointerAliasing.isRevealingType(usedPointer.getValue())) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
        handled = true;
      } else if (rhs instanceof CExpression &&
                 // TODO: use rhsExpression.isUnaliasedLocation() instead?
                 CExpressionVisitorWithPointerAliasing.isUnaliasedLocation((CExpression) rhs)) {
        assert rhsExpression.isUnaliasedLocation() &&
               rhsExpression.asUnaliasedLocation().getVariableName().equals(usedPointer.getKey()) &&
               rhsUsedDeferredAllocationPointers.size() == 1 :
               "Wrong assumptions on deferred allocations tracking: rhs is not a single pointer";
        final CType lhsType = CTypeUtils.simplifyType(lhs.getExpressionType());
        if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
            // TODO: is the following isUnaliasedLocation() check really needed?
            CExpressionVisitorWithPointerAliasing.isUnaliasedLocation(lhs) &&
            !lhsLocation.isAliased()) {
          final Map.Entry<String, CType> lhsUsedPointer = !lhsUsedDeferredAllocationPointers.isEmpty() ?
                                                       lhsUsedDeferredAllocationPointers.entrySet().iterator().next() :
                                                       null;
          assert lhsUsedDeferredAllocationPointers.size() <= 1 &&
                 rhsExpression.isUnaliasedLocation() &&
                 (lhsUsedPointer == null ||
                  (rhsExpression.asUnaliasedLocation().getVariableName()).equals(lhsUsedPointer.getKey())) :
                 "Wrong assumptions on deferred allocations tracking: unrecognized lhs";
          if (lhsUsedPointer != null) {
            handleDeferredAllocationPointerRemoval(lhsUsedPointer.getKey(), false);
          }
          pts.addDeferredAllocationPointer(lhsLocation.asUnaliased().getVariableName(), usedPointer.getKey());
          passed = true;
          handled = true;
        } else if (CExpressionVisitorWithPointerAliasing.isRevealingType(lhsType)) {
          handleDeferredAllocationTypeRevelation(usedPointer.getKey(), lhsType);
          handled = true;
        }
      }
      if (!handled) {
        handleDeferredAllocationPointerEscape(usedPointer.getKey());
      }
    }
    for (final Map.Entry<String, CType> usedPointer : lhsUsedDeferredAllocationPointers.entrySet()) {
      if (!usedPointer.getValue().equals(CPointerType.POINTER_TO_VOID)) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
      // TODO: use lhsExpression.isUnaliasedLoation() instead (?)
      } else if (CExpressionVisitorWithPointerAliasing.isUnaliasedLocation(lhs)) {
        assert !lhsLocation.isAliased() &&
               lhsLocation.asUnaliased().getVariableName().equals(usedPointer.getKey()) &&
               lhsUsedDeferredAllocationPointers.size() == 1 :
               "Wrong assumptions on deferred allocations tracking: lhs is not a single pointer";
        if (!passed) {
          handleDeferredAllocationPointerRemoval(usedPointer.getKey(), false);
        }
      } else {
        handleDeferredAllocationPointerEscape(usedPointer.getKey());
      }
    }
  }

  void handleDeferredAllocationsInAssume(final CExpression e,
                                                 final Map<String, CType> usedDeferredAllocationPointers)
                                                     throws UnrecognizedCCodeException {
    for (final Map.Entry<String, CType> usedPointer : usedDeferredAllocationPointers.entrySet()) {
      if (!usedPointer.getValue().equals(CPointerType.POINTER_TO_VOID)) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
      } else if (e instanceof CBinaryExpression) {
        final CBinaryExpression binaryExpression = (CBinaryExpression) e;
        switch (binaryExpression.getOperator()) {
        case EQUALS:
        case GREATER_EQUAL:
        case GREATER_THAN:
        case LESS_EQUAL:
        case LESS_THAN:
          final CType operand1Type = CTypeUtils.simplifyType(binaryExpression.getOperand1().getExpressionType());
          final CType operand2Type = CTypeUtils.simplifyType(binaryExpression.getOperand2().getExpressionType());
          CType type = null;
          if (CExpressionVisitorWithPointerAliasing.isRevealingType(operand1Type)) {
            type = operand1Type;
          } else if (CExpressionVisitorWithPointerAliasing.isRevealingType(operand2Type)) {
            type = operand2Type;
          }
          if (type != null) {
            handleDeferredAllocationTypeRevelation(usedPointer.getKey(), type);
          }
          break;
        case NOT_EQUALS:
          // TODO Do something here?
          break;
        default:
          throw new UnrecognizedCCodeException("unexpected binary operator in assume", e);
        }
      }
    }
  }

  private void handleDeferredAllocationPointerRemoval(final String pointerVariable,
      final boolean isReturn) {
    if (pts.removeDeferredAllocatinPointer(pointerVariable)) {
      conv.logger.logfOnce(Level.WARNING,
                           (!isReturn ? "Assignment to the" : "Destroying the") +
                             " void * pointer  %s produces garbage! (in the following line(s):\n %s)",
                           pointerVariable,
                           edge);
    }
  }

  /**
   * The function removes local void * pointers (deferred allocations)
   * declared in current function scope from tracking after returning from the function.
   */
  void handleDeferredAllocationInFunctionExit(final String function) {
    String prefix = function + CtoFormulaConverter.scoped("", function);
    SortedSet<String> localVariables = Collections3.subSetWithPrefix(pts.getDeferredAllocationVariables(), prefix);

    for (final String variable : localVariables) {
      handleDeferredAllocationPointerRemoval(variable, true);
    }
  }
}
