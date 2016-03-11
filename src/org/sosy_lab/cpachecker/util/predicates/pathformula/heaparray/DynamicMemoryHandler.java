/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.DeferredAllocationPool;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is responsible for handling everything related to dynamic memory, e.g. calls to
 * malloc() and free(), and for handling deferred allocations (calls to malloc() where the assumed
 * type of the memory is not yet known).
 */
class DynamicMemoryHandler {

  private static final String CALLOC_FUNCTION = "calloc";

  private static final String MALLOC_INDEX_SEPARATOR = "#";

  private final CToFormulaConverterWithHeapArray converter;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;

  /**
   * Creates a new DynamicMemoryHandler
   *
   * @param pConverter               The C to SMT formula converter.
   * @param pCFAEdge                 The current edge in the CFA (for logging purposes).
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   */
  DynamicMemoryHandler(
      final CToFormulaConverterWithHeapArray pConverter,
      final CFAEdge pCFAEdge,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions) {
    converter = pConverter;
    edge = pCFAEdge;
    ssa = pSSAMapBuilder;
    pts = pPointerTargetSetBuilder;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
  }

  /**
   * Handles a dynamic memory function and returns its value.
   *
   * @param pExpression        The function call expression.
   * @param pFunctionName      The name of the function.
   * @param pExpressionVisitor A visitor to evaluate the expression's value.
   * @return The value of the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  Value handleDynamicMemoryFunction(
      final CFunctionCallExpression pExpression,
      final String pFunctionName,
      final CExpressionVisitorWithHeapArray pExpressionVisitor)
      throws UnrecognizedCCodeException, InterruptedException {

    if ((converter.options.isSuccessfulAllocFunctionName(pFunctionName)
        || converter.options.isSuccessfulZallocFunctionName(pFunctionName))) {
      return Value.ofValue(handleSuccessfulMemoryAllocation(
          pFunctionName, pExpression.getParameterExpressions(), pExpression));

    } else if ((converter.options.isMemoryAllocationFunction(pFunctionName)
        || converter.options.isMemoryAllocationFunctionWithZeroing(pFunctionName))) {
      return Value.ofValue(handleMemoryAllocation(pExpression, pFunctionName));

    } else if (converter.options.isMemoryFreeFunction(pFunctionName)) {
      return handleMemoryFree(pExpression, pExpressionVisitor);
    } else {
      throw new AssertionError("Unknown memory allocation function " + pFunctionName);
    }
  }

  /**
   * Handle memory allocation functions that may fail (i.e., return null) and that may or may not
   * zero the memory.
   *
   * @param pExpression   The function call expression.
   * @param pFunctionName The name of the allocation function.
   * @return A formula for the memory allocation.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  private Formula handleMemoryAllocation(
      final CFunctionCallExpression pExpression,
      final String pFunctionName)
      throws UnrecognizedCCodeException, InterruptedException {

    final boolean isZeroing =
        converter.options.isMemoryAllocationFunctionWithZeroing(pFunctionName);
    List<CExpression> parameters = pExpression.getParameterExpressions();

    if (pFunctionName.equals(CALLOC_FUNCTION) && parameters.size() == 2) {
      CExpression param0 = parameters.get(0);
      CExpression param1 = parameters.get(1);

      // Build expression for param0 * param1 as new parameter.
      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(
          converter.machineModel, converter.logger);
      CBinaryExpression multiplication = builder.buildBinaryExpression(
          param0, param1, BinaryOperator.MULTIPLY);

      // Try to evaluate the multiplication if possible.
      Integer value0 = tryEvaluateExpression(param0);
      Integer value1 = tryEvaluateExpression(param1);
      if (value0 != null && value1 != null) {
        long result = ExpressionValueVisitor.calculateBinaryOperation(
            new NumericValue(value0.longValue()),
            new NumericValue(value1.longValue()), multiplication,
            converter.machineModel, converter.logger).asLong(multiplication.getExpressionType());

        CExpression newParam = new CIntegerLiteralExpression(
            param0.getFileLocation(), multiplication.getExpressionType(),
            BigInteger.valueOf(result));
        parameters = Collections.singletonList(newParam);

      } else {
        parameters = Collections.<CExpression>singletonList(multiplication);
      }

    } else if (parameters.size() != 1) {
      if (parameters.size() > 1
          && converter.options.hasSuperfluousParameters(pFunctionName)) {
        parameters = Collections.singletonList(parameters.get(0));
      } else {
        throw new UnrecognizedCCodeException(
            String.format("Memory allocation function %s() called with %d parameters instead of 1",
                pFunctionName, parameters.size()), edge, pExpression);
      }
    }

    final String delegateFunctionName = !isZeroing
                                        ? converter.options.getSuccessfulAllocFunctionName()
                                        : converter.options.getSuccessfulZallocFunctionName();

    if (!converter.options.makeMemoryAllocationsAlwaysSucceed()) {
      final Formula nonDet = converter.makeFreshVariable(pFunctionName,
          CPointerType.POINTER_TO_VOID, ssa);
      return converter.bfmgr.ifThenElse(converter.bfmgr.not(
          converter.formulaManager.makeEqual(nonDet, converter.nullPointer)),
          handleSuccessfulMemoryAllocation(delegateFunctionName, parameters,
              pExpression), converter.nullPointer);
    } else {
      return handleSuccessfulMemoryAllocation(delegateFunctionName, parameters,
          pExpression);
    }
  }

  /**
   * Handle memory allocation functions that cannot fail (i.e., do not return NULL) and do not zero
   * the memory.
   *
   * @param pFunctionName The name of the memory allocation function.
   * @param pParameters   The list of function parameters.
   * @param pExpression   The function call expression.
   * @return A formula for the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  private Formula handleSuccessfulMemoryAllocation(
      final String pFunctionName,
      List<CExpression> pParameters,
      final CFunctionCallExpression pExpression)
      throws UnrecognizedCCodeException, InterruptedException {
    // e.getFunctionNameExpression() should not be used
    // as it might refer to another function if this method is called from
    // handleMemoryAllocation()
    if (pParameters.size() != 1) {
      if (pParameters.size() > 1 && converter.options.hasSuperfluousParameters(pFunctionName)) {
        pParameters = Collections.singletonList(pParameters.get(0));
      } else {
        throw new UnrecognizedCCodeException(
            String.format("Memory allocation function %s() called with %d parameters instead of 1",
                pFunctionName, pParameters.size()), edge, pExpression);
      }
    }

    final CExpression parameter = pParameters.get(0);
    Integer size = null;
    final CType newType;
    if (isSizeof(parameter)) {
      newType = getSizeofType(parameter);
    } else if (isSizeofMultiple(parameter)) {
      final CBinaryExpression product = (CBinaryExpression) parameter;
      final CType operand1Type = getSizeofType(product.getOperand1());
      final CType operand2Type = getSizeofType(product.getOperand2());
      if (operand1Type != null) {
        newType = new CArrayType(false, false, operand1Type, product.getOperand2());
      } else if (operand2Type != null) {
        newType = new CArrayType(false, false, operand2Type, product.getOperand1());
      } else {
        throw new UnrecognizedCCodeException("Can't determine type for internal memory allocation",
            edge, pExpression);
      }
    } else {
      size = tryEvaluateExpression(parameter);
      if (!converter.options.revealAllocationTypeFromLHS()
          && !converter.options.deferUntypedAllocations()) {
        final CExpression length;
        if (size == null) {
          size = converter.options.defaultAllocationSize();
          length = new CIntegerLiteralExpression(parameter.getFileLocation(),
              parameter.getExpressionType(), BigInteger.valueOf(size));
        } else {
          length = parameter;
        }
        newType = new CArrayType(false, false, CVoidType.VOID, length);
      } else {
        newType = null;
      }
    }

    Formula address;
    if (newType != null) {
      final String newBase = makeAllocVariableName(pFunctionName, newType, ssa, converter);
      address = makeAllocation(
          converter.options.isSuccessfulZallocFunctionName(pFunctionName), newType, newBase);
    } else {
      final String newBase = makeAllocVariableName(pFunctionName, CVoidType.VOID,
          ssa, converter);
      pts.addTemporaryDeferredAllocation(
          converter.options.isSuccessfulZallocFunctionName(pFunctionName),
          size != null ? new CIntegerLiteralExpression(
              parameter.getFileLocation(), parameter.getExpressionType(),
              BigInteger.valueOf(size)) : null, newBase);
      address = converter.makeConstant(PointerTargetSet.getBaseName(newBase),
          CPointerType.POINTER_TO_VOID);
    }

    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(converter.formulaManager.makeEqual(
          converter.makeBaseAddressOfTerm(address), address));
    }
    return address;
  }

  /**
   * Handles calls to {@code free()}.
   *
   * @param pExpression        The parameters of the {@code free()} call.
   * @param pExpressionVisitor A visitor to evaluate the value of the function call.
   * @return The return value of the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private Value handleMemoryFree(
      final CFunctionCallExpression pExpression,
      final CExpressionVisitorWithHeapArray pExpressionVisitor)
      throws UnrecognizedCCodeException {
    final List<CExpression> parameters = pExpression.getParameterExpressions();
    if (parameters.size() != 1) {
      throw new UnrecognizedCCodeException(String.format("free() called with %d parameters",
          parameters.size()), edge, pExpression);
    }

    if (errorConditions.isEnabled()) {
      final Formula operand = pExpressionVisitor.asValueFormula(
          parameters.get(0).accept(pExpressionVisitor),
          CTypeUtils.simplifyType(parameters.get(0).getExpressionType()));
      BooleanFormula validFree = converter.formulaManager.makeEqual(operand, converter.nullPointer);

      for (String base : pts.getAllBases()) {
        Formula baseF = converter.makeConstant(
            PointerTargetSet.getBaseName(base), CPointerType.POINTER_TO_VOID);
        validFree = converter.bfmgr.or(validFree,
            converter.formulaManager.makeEqual(operand, baseF));
      }
      errorConditions.addInvalidFreeCondition(converter.bfmgr.not(validFree));
    }

    return Value.nondetValue(); // free does not return anything, so nondet is ok
  }

  /**
   * Creates a formula for memory allocations.
   *
   * @param pIsZeroing A flag indicating if the variable is zeroing.
   * @param pType      The type.
   * @param pBase      The name of the base.
   * @return A formula for the memory allocation.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution gets interrupted.
   */
  private Formula makeAllocation(
      final boolean pIsZeroing,
      final CType pType,
      final String pBase)
      throws UnrecognizedCCodeException, InterruptedException {
    final CType baseType = CTypeUtils.getBaseType(pType);
    final Formula result = converter.makeConstant(PointerTargetSet.getBaseName(pBase), baseType);
    if (pIsZeroing) {
      AssignmentHandler assignmentHandler = new AssignmentHandler(converter, edge, pBase, ssa,
          pts, constraints, errorConditions);
      final BooleanFormula initialization =
          assignmentHandler.makeAssignment(
              pType,
              CNumericTypes.SIGNED_CHAR,
              AliasedLocation.ofAddress(result),
              Value.ofValue(
                  converter.formulaManager.makeNumber(
                      converter.getFormulaTypeFromCType(CNumericTypes.SIGNED_CHAR), 0)),
              true,
              null);
      constraints.addConstraint(initialization);
    }
    converter.addPreFilledBase(pBase, pType, false, pIsZeroing, constraints, pts);
    return result;
  }

  /**
   * Creates a name for an allocation.
   *
   * @param pFunctionName  The name of the function.
   * @param pType          The type of the function.
   * @param pSSAMapBuilder The SSA map.
   * @param pConverter     The C to SMT formula converter.
   * @return A name for allocations.
   */
  static String makeAllocVariableName(
      final String pFunctionName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder,
      final CtoFormulaConverter pConverter) {
    return pFunctionName
        + "_"
        + CToFormulaConverterWithHeapArray.getArrayName(pType)
        + MALLOC_INDEX_SEPARATOR
        + pConverter.makeFreshIndex(pFunctionName, pType, pSSAMapBuilder);
  }

  /**
   * Tries to evaluate an expression, i.e. get the value if it is an integer literal.
   *
   * @param pExpression The C expression.
   * @return The value, if the expression is an integer literal, or {@code null}
   */
  private static Integer tryEvaluateExpression(CExpression pExpression) {
    if (pExpression instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) pExpression).getValue().intValue();
    }
    return null;
  }

  /**
   * Returns, whether a expression is a {@code sizeof} expression.
   *
   * @param pExpression The C expression.
   * @return True, if the expression is a {@code sizeof} expression, false otherwise.
   */
  private static boolean isSizeof(final CExpression pExpression) {
    return pExpression instanceof CUnaryExpression
        && ((CUnaryExpression) pExpression).getOperator() == UnaryOperator.SIZEOF
        || pExpression instanceof CTypeIdExpression
        && ((CTypeIdExpression) pExpression).getOperator() == TypeIdOperator.SIZEOF;
  }

  /**
   * Returns, whether the expression is a multiplication of the {@code sizeof} operator.
   *
   * @param pExpression The expression type.
   * @return True, if the expression is a multiplication of the {@code sizeof} operator, false
   * otherwise.
   */
  private static boolean isSizeofMultiple(final CExpression pExpression) {
    return pExpression instanceof CBinaryExpression
        && ((CBinaryExpression) pExpression).getOperator() == BinaryOperator.MULTIPLY
        && (isSizeof(((CBinaryExpression) pExpression).getOperand1())
        || isSizeof(((CBinaryExpression) pExpression).getOperand2()));
  }

  /**
   * Returns a C type for the size of an expression.
   *
   * @param pExpression The expression type to get the size from.
   * @return The size of the expression.
   */
  private static CType getSizeofType(CExpression pExpression) {
    if (pExpression instanceof CUnaryExpression
        && ((CUnaryExpression) pExpression).getOperator() == UnaryOperator.SIZEOF) {
      return CTypeUtils.simplifyType(
          ((CUnaryExpression) pExpression).getOperand().getExpressionType());
    } else if (pExpression instanceof CTypeIdExpression
        && ((CTypeIdExpression) pExpression).getOperator() == TypeIdOperator.SIZEOF) {
      return CTypeUtils.simplifyType(((CTypeIdExpression) pExpression).getType());
    } else {
      return null;
    }
  }


  // Handling of deferred allocations

  /**
   * The function tries to recover dynamically allocated array type from the pointer type it was
   * casted or assigned to.
   *
   * @param pType        the revealing <em>pointed</em> type (e.g. {@code char} for {@code char *})
   *                     of the pointer to which the void * variable was casted or assigned to.
   * @param pSizeLiteral the size specified at the allocation site.
   * @return the recovered array type or the {@code type} parameter in case the type can't be
   * recovered
   */
  private CType refineType(
      final @Nonnull CType pType,
      final @Nonnull CIntegerLiteralExpression pSizeLiteral) {
    assert pSizeLiteral.getValue() != null;

    final int size = pSizeLiteral.getValue().intValue();
    final int typeSize = converter.getSizeof(pType);
    if (pType instanceof CArrayType) {
      // An array type is used in the cast or assignment, so its size should
      // likely match the allocated size. Issue a warning if this isn't the case
      if (typeSize != size) {
        converter.logger.logf(Level.WARNING, "Array size of the revealed type differs form the " +
            "allocation size: %s : %d != %d", pType, typeSize, size);
      }
      // The type used is already an array type, nothing to recover
      return pType;

    } else {
      // A pointer type is used in the cast or assignment.
      // If the allocated size is the multiple of the usage type size, we can
      // recover the actual array type (with length) of the allocation.
      // Otherwise, just return the usage type.
      final int n = size / typeSize;
      final int remainder = size % typeSize;
      if (n == 0 || remainder != 0) {
        converter.logger.logf(Level.WARNING, "Can't refine allocation type, but the sizes " +
            "differ: %s : %d != %d", pType, typeSize, size);
        return pType;
      }

      return new CArrayType(false, false, pType,
          new CIntegerLiteralExpression(pSizeLiteral.getFileLocation(),
              pSizeLiteral.getExpressionType(), BigInteger.valueOf(n)));
    }
  }

  /**
   * Returns the type of the allocated dynamic variable by the usage pointer type of the void *
   * variable and the allocation size
   *
   * @param pType        The usage pointer type.
   * @param pSizeLiteral The allocation size.
   * @return The type of the allocated dynamic variable.
   */
  private CType getAllocationType(
      final @Nonnull CType pType,
      final @Nullable CIntegerLiteralExpression pSizeLiteral) {
    if (pType instanceof CPointerType) {
      return pSizeLiteral != null
             ? refineType(((CPointerType) pType).getType(), pSizeLiteral)
             : ((CPointerType) pType).getType();
    } else if (pType instanceof CArrayType) {
      return pSizeLiteral != null ? refineType(pType, pSizeLiteral) : pType;
    } else {
      throw new IllegalArgumentException("Either pointer or array type expected");
    }
  }

  /**
   * Handles the type revelation of deferred allocations.
   *
   * @param pPointerVariable The name of the pointer variable.
   * @param pType            The type of the pointer variable.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       if the execution was interrupted.
   */
  private void handleDeferredAllocationTypeRevelation(
      final @Nonnull String pPointerVariable,
      final @Nonnull CType pType)
      throws UnrecognizedCCodeException, InterruptedException {
    final DeferredAllocationPool deferredAllocationPool =
        pts.removeDeferredAllocation(pPointerVariable);
    for (final String baseVariable : deferredAllocationPool.getBaseVariables()) {
      makeAllocation(deferredAllocationPool.wasAllocationZeroing(),
          getAllocationType(pType, deferredAllocationPool.getSize()),
          baseVariable);
    }
  }

  /**
   * Handles the escape of a deferred allocation from tracking.
   *
   * @param pPointerVariable The name of the pointer variable.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  private void handleDeferredAllocationPointerEscape(
      final String pPointerVariable)
      throws UnrecognizedCCodeException, InterruptedException {
    final DeferredAllocationPool deferredAllocationPool =
        pts.removeDeferredAllocation(pPointerVariable);
    final CIntegerLiteralExpression size =
        deferredAllocationPool.getSize() != null
        ? deferredAllocationPool.getSize()
        : new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.SIGNED_CHAR,
            BigInteger.valueOf(converter.options.defaultAllocationSize()));

    converter.logger.logfOnce(Level.WARNING, "The void * pointer %s to a deferred allocation " +
        "escaped form tracking! Allocating array void[%d]. (in the following line(s):\n %s)",
        pPointerVariable, size.getValue(), edge);

    for (final String baseVariable : deferredAllocationPool.getBaseVariables()) {
      makeAllocation(deferredAllocationPool.wasAllocationZeroing(),
          new CArrayType(false, false, CVoidType.VOID, size), baseVariable);
    }
  }

  /**
   * Handles deferred allocations in assignment expressions.
   *
   * @param pLhs                               The left hand side of the C expression.
   * @param pRhs                               The right hand side of the C expression.
   * @param pLhsLocation                       The location of the left hand side in the source
   *                                           file.
   * @param pRhsExpression                     The expression of the right hand side.
   * @param pLhsType                           The type of the left hand side.
   * @param pLhsUsedDeferredAllocationPointers A map of all used deferred allocation pointers on the
   *                                           left hand side.
   * @param pRhsUsedDeferredAllocationPointers A map of all used deferred allocation pointers on the
   *                                           right hand side.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  void handleDeferredAllocationsInAssignment(
      final CLeftHandSide pLhs,
      final CRightHandSide pRhs,
      final Location pLhsLocation,
      final Expression pRhsExpression,
      final CType pLhsType,
      final Map<String, CType> pLhsUsedDeferredAllocationPointers,
      final Map<String, CType> pRhsUsedDeferredAllocationPointers)
      throws UnrecognizedCCodeException, InterruptedException {

    // Handle allocations: reveal the actual type form the LHS type or defer the allocation until later
    boolean isAllocation = false;
    if ((converter.options.revealAllocationTypeFromLHS()
        || converter.options.deferUntypedAllocations())
        && pRhs instanceof CFunctionCallExpression
        && !pRhsExpression.isNondetValue()
        && pRhsExpression.isValue()) {
      final Set<String> rhsVariables = converter.formulaManager.extractVariableNames(
          pRhsExpression.asValue().getValue());
      // Actually there is always either 1 variable (just address) or
      // 2 variables (nondet + allocation address)
      for (String variable : rhsVariables) {
        if (PointerTargetSet.isBaseName(variable)) {
          variable = PointerTargetSet.getBase(variable);
        }

        if (pts.isTemporaryDeferredAllocationPointer(variable)) {
          if (!isAllocation) {
            // We can reveal the type from the LHS
            if (CExpressionVisitorWithHeapArray.isRevealingType(pLhsType)) {
              handleDeferredAllocationTypeRevelation(variable, pLhsType);
              // We can defer the allocation and start tracking the variable in the LHS
            } else if (pLhsType.equals(CPointerType.POINTER_TO_VOID)
                && // TODO: remove the double-check (?)
                CExpressionVisitorWithHeapArray.isUnaliasedLocation(pLhs)
                && pLhsLocation.isUnaliasedLocation()) {
              final String variableName = pLhsLocation.asUnaliasedLocation().getVariableName();
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
            throw new UnrecognizedCCodeException("Can't handle ambiguous allocation", edge, pRhs);
          }
        }
      }
    }

    // Track currently deferred allocations
    if (converter.options.deferUntypedAllocations() && !isAllocation) {
      handleDeferredAllocationsInAssignment(pLhs, pRhs, pLhsLocation, pRhsExpression,
          pLhsUsedDeferredAllocationPointers, pRhsUsedDeferredAllocationPointers);
    }
  }

  /**
   * Handles deferred allocations in assignment expressions.
   *
   * @param pLhs                               The left hand side of the C expression.
   * @param pRhs                               The right hand side of the C expression.
   * @param pLhsLocation                       The location of the left hand side in the source
   *                                           file.
   * @param pRhsExpression                     The expression of the right hand side.
   * @param pLhsUsedDeferredAllocationPointers A map of all used deferred allocation pointers on the
   *                                           left hand side.
   * @param pRhsUsedDeferredAllocationPointers A map of all used deferred allocation pointers on the
   *                                           right hand side.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  private void handleDeferredAllocationsInAssignment(
      final CLeftHandSide pLhs,
      final CRightHandSide pRhs,
      final Location pLhsLocation,
      final Expression pRhsExpression,
      final Map<String, CType> pLhsUsedDeferredAllocationPointers,
      final Map<String, CType> pRhsUsedDeferredAllocationPointers)
      throws UnrecognizedCCodeException, InterruptedException {

    boolean passed = false;
    // Iterate over the void * variables used in the RHS of the assignment
    // The keys of the entries contain variable names while the values specify
    // the corresponding cast types, if present, (or void *, if not)
    // e.g. in `ps = (struct s *) tmp;' that'd be {"tmp" -> struct s *}
    for (final Map.Entry<String, CType> usedPointer
        : pRhsUsedDeferredAllocationPointers.entrySet()) {
      boolean handled = false;

      if (CExpressionVisitorWithHeapArray.isRevealingType(usedPointer.getValue())) {
        // The cast type is pointer or array type different from void *, so it
        // can be used to reveal the type of the allocation
        // e.g. return (struct s *)__tmp;
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
        handled = true;

        // If we still can't reveal the type from a cast so we'll try to reveal
        // it from the type of the LHS, or start tracking the variable in the
        // LHS as it's now an alias for the same dynamic variable.

        // Both outcomes are only possible if the RHS is encoded as a variable.
      } else if (pRhs instanceof CExpression
          && // This checks if the RHS is encoded as a variable rather than an
          // UF application rhsExpression.isUnaliasedLocation() returns just
          // this
          pRhsExpression.isUnaliasedLocation()) {
        // An older criterion for the same condition is used for double-checking
        assert CExpressionVisitorWithHeapArray.isUnaliasedLocation((CExpression) pRhs)
            : "Wrong assumptions on deferred allocations tracking: rhs " + pRhsExpression
            + " is not unaliased";
        // Check if the only variable is the rhs is the one we're currently
        // dealing with during the iteration
        // (i.e. rhsUsedDeferredAllocationPointers doesn't contain any extra
        // variables)
        assert pRhsExpression.asUnaliasedLocation().getVariableName().equals(usedPointer.getKey())
            : "Wrong assumptions on deferred allocations tracking: rhs " + pRhsExpression
            + " does not match " +  usedPointer;
        assert pRhsUsedDeferredAllocationPointers.size() == 1 : "Wrong assumptions on deferred "
            + "allocations tracking: rhs is not a single pointer, "
            + "rhsUsedDeferredAllocationPointers.size() is "
            + pRhsUsedDeferredAllocationPointers.size();

        final CType lhsType = CTypeUtils.simplifyType(pLhs.getExpressionType());
        // The worse case -- LHS has type void *
        if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
            // Check if the LHS is encoded as a variable
            !pLhsLocation.isAliased()) {
          // Double-check
          assert CExpressionVisitorWithHeapArray.isUnaliasedLocation(pLhs)
              : "Wrong assumptions on deferred allocations tracking: lhs "
              + pLhsLocation + " is not unaliased";
          // Now check that lhsUsedDeferredAllocationPointers is filled correctly.
          // It should either contain the only pointer that was previously
          // tracked and is now gone (rewritten with the new value) or be empty.
          final Map.Entry<String, CType> lhsUsedPointer =
              !pLhsUsedDeferredAllocationPointers.isEmpty()
              ? pLhsUsedDeferredAllocationPointers.entrySet().iterator().next()
              : null;

          assert pLhsUsedDeferredAllocationPointers.size() <= 1 : "Wrong "
              + "assumptions on deferred allocations tracking: "
              + "lhsUsedDeferredAllocationPointers.size() is "
              + pLhsUsedDeferredAllocationPointers.size();
          assert (lhsUsedPointer == null
              || (pLhsLocation.asUnaliased().getVariableName()).equals(
              lhsUsedPointer.getKey())) : "Wrong assumptions on deferred "
              + "allocations tracking: lhs " + lhsUsedPointer + " does not "
              + "match rhs " + pRhsExpression;

          if (lhsUsedPointer != null) {
            // The assignment rewrites a void pointer that was previously
            // tracked, so remove it from the pool e.g.
            // __tmp = malloc(...); // allocation #1
            // __tmp2 = malloc(...); // allocation #2
            // __tmp = __tmp2; // __tmp doesn't point to the allocation #1 anymore
            handleDeferredAllocationPointerRemoval(lhsUsedPointer.getKey(), false);
          }
          // Start tracking the void pointer variable in the LHS.
          // e.g. __tmp = __tmp2; // __tmp now also points to the allocation #2
          pts.addDeferredAllocationPointer(
              pLhsLocation.asUnaliased().getVariableName(), usedPointer.getKey());
          passed = true;
          handled = true;
        } else if (CExpressionVisitorWithHeapArray.isRevealingType(
            lhsType)) {
          // The better case -- LHS has some pointer or array type different
          // from void *, e.g.
          // struct s *ps;
          // ps = __tmp;
          handleDeferredAllocationTypeRevelation(usedPointer.getKey(), lhsType);
          handled = true;
        }
      }

      if (!handled) {
        // The worst case -- either the LHS has type void *, but is not encoded
        // as a variable so we can't follow up its value changes, or RHS is too
        // complicated to reveal the types of the variables in it. e.g.:
        // ps->pf = __tmp;
        // *pps = __tmp;
        // ps = __tmp + 4; // 4 can be a char array index, but can also be a
        // field offset

        // So we give up and allocate a dummy.
        handleDeferredAllocationPointerEscape(usedPointer.getKey());
      }
    }

    // Now iterate over the variable used in the LHS
    for (final Map.Entry<String, CType> usedPointer
        : pLhsUsedDeferredAllocationPointers.entrySet()) {
      // Don't consider deferred allocation pointers that were already handled
      // in the RHS
      if (pRhsUsedDeferredAllocationPointers.containsKey(usedPointer.getKey())) {
        continue;
      }

      if (CExpressionVisitorWithHeapArray.isRevealingType(
          usedPointer.getValue())) {
        // *((int *)__tmp) = 5;
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
      } else if (!pLhsLocation.isAliased()) {
        assert CExpressionVisitorWithHeapArray.isUnaliasedLocation(pLhs)
            : "Wrong assumptions on deferred allocations tracking: lhs " + pLhsLocation
            + " is aliased";
        assert pLhsLocation.asUnaliased().getVariableName().equals(usedPointer.getKey())
            : "Wrong assumptions on deferred allocations tracking: lhs " + pLhsLocation
            + " does not match " + usedPointer;
        assert pLhsUsedDeferredAllocationPointers.size() == 1 : "Wrong assumptions on deferred "
            + "allocations tracking: lhs is not a single pointer, "
            + "lhsUsedDeferredAllocationPointers.size() is "
            + pLhsUsedDeferredAllocationPointers.size();

        if (!passed) {
          // e.g. __tmp = 0;
          handleDeferredAllocationPointerRemoval(usedPointer.getKey(), false);
        }
        // e.g. __tmp = __tmp2;
      } else {
        // LHS is aliased, but pointer type can't be revealed, e.g.
        // a[__tmp - __tmp] = 0;
        // (char *)(__tmp + 1) = 0;

        // So this includes worst cases and we'd better give up
        handleDeferredAllocationPointerEscape(usedPointer.getKey());
      }
    }
  }

  /**
   * Handles deferred allocations in assume expressions.
   *
   * @param pExpression                     The expression in the C code.
   * @param pUsedDeferredAllocationPointers A map of all used deferred allocation pointers.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution gets interrupted.
   */
  void handleDeferredAllocationsInAssume(
      final CExpression pExpression,
      final Map<String, CType> pUsedDeferredAllocationPointers)
      throws UnrecognizedCCodeException, InterruptedException {

    for (final Map.Entry<String, CType> usedPointer
        : pUsedDeferredAllocationPointers.entrySet()) {

      if (!usedPointer.getValue().equals(CPointerType.POINTER_TO_VOID)) {
        handleDeferredAllocationTypeRevelation(
            usedPointer.getKey(), usedPointer.getValue());

      } else if (pExpression instanceof CBinaryExpression) {
        final CBinaryExpression binaryExpression = (CBinaryExpression) pExpression;
        switch (binaryExpression.getOperator()) {
          case EQUALS:
          case NOT_EQUALS:
          case GREATER_EQUAL:
          case GREATER_THAN:
          case LESS_EQUAL:
          case LESS_THAN:
            final CType operand1Type = CTypeUtils.simplifyType(
                binaryExpression.getOperand1().getExpressionType());
            final CType operand2Type = CTypeUtils.simplifyType(
                binaryExpression.getOperand2().getExpressionType());
            CType type = null;

            if (CExpressionVisitorWithHeapArray.isRevealingType(operand1Type)) {
              type = operand1Type;
            } else if (CExpressionVisitorWithHeapArray.isRevealingType(operand2Type)) {
              type = operand2Type;
            }
            if (type != null) {
              handleDeferredAllocationTypeRevelation(usedPointer.getKey(), type);
            }
            break;
          default:
            throw new UnrecognizedCCodeException("unexpected binary operator in assume",
                pExpression);
        }
      }
    }
  }

  /**
   * Removes a pointer variable from tracking.
   *
   * @param pPointerVariable The name of the pointer variable.
   * @param pIsReturn        A flag indicating if the variable is a return variable.
   */
  private void handleDeferredAllocationPointerRemoval(
      final String pPointerVariable,
      final boolean pIsReturn) {
    if (pts.removeDeferredAllocatinPointer(pPointerVariable)) {
      converter.logger.logfOnce(Level.WARNING, (!pIsReturn ? "Assignment to the" : "Destroying "
          + "the") + " void * pointer  %s produces garbage! (in the following line(s):\n %s)",
          pPointerVariable, edge);
    }
  }

  /**
   * The function removes local void * pointers (deferred allocations) declared in current function
   * scope from tracking after returning from the function.
   *
   * @param pFunction THe name of the function.
   */
  void handleDeferredAllocationInFunctionExit(final String pFunction) {
    SortedSet<String> localVariables = CFAUtils.filterVariablesOfFunction(
        pts.getDeferredAllocationVariables(), pFunction);

    for (final String variable : localVariables) {
      handleDeferredAllocationPointerRemoval(variable, true);
    }
  }
}
