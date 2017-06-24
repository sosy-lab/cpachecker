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

import com.google.common.collect.ImmutableSortedSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * This class is responsible for handling everything related to dynamic memory,
 * e.g. calls to malloc() and free(),
 * and for handling deferred allocations (calls to malloc() where the assumed
 * type of the memory is not yet known).
 */
class DynamicMemoryHandler {

  private static final String CALLOC_FUNCTION = "calloc";

  private static final String MALLOC_INDEX_SEPARATOR = "#";

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final MemoryRegionManager regionMgr;

  /**
   * Creates a new DynamicMemoryHandler
   *
   * @param pConv The C to SMT formula converter.
   * @param pEdge The current edge in the CFA (for logging purposes).
   * @param pSsa The SSA map.
   * @param pPts The underlying pointer target set
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   */
  DynamicMemoryHandler(CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge, SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts, Constraints pConstraints,
      ErrorConditions pErrorConditions, MemoryRegionManager pRegionMgr) {
    conv = pConv;
    typeHandler = pConv.typeHandler;
    edge = pEdge;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    regionMgr = pRegionMgr;
  }

  /**
   * Handles a dynamic memory function and returns its value.
   *
   * @param e The function call expression.
   * @param functionName The name of the function.
   * @param expressionVisitor A visitor to evaluate the expression's value.
   * @return The value of the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  Value handleDynamicMemoryFunction(final CFunctionCallExpression e, final String functionName,
      final CExpressionVisitorWithPointerAliasing expressionVisitor) throws UnrecognizedCCodeException, InterruptedException {

    if ((conv.options.isSuccessfulAllocFunctionName(functionName) ||
        conv.options.isSuccessfulZallocFunctionName(functionName))) {
      return Value.ofValue(handleSuccessfulMemoryAllocation(functionName, e.getParameterExpressions(), e));

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
   *
   * @param e The function call expression.
   * @param functionName The name of the allocation function.
   * @return A formula for the memory allocation.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  private Formula handleMemoryAllocation(final CFunctionCallExpression e,
      final String functionName) throws UnrecognizedCCodeException, InterruptedException {
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
            conv.machineModel, conv.logger).asLong(multiplication.getExpressionType());

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
                                    handleSuccessfulMemoryAllocation(delegateFunctionName, parameters, e),
                                    conv.nullPointer);
    } else {
      return handleSuccessfulMemoryAllocation(delegateFunctionName, parameters, e);
    }
  }

  /**
   * Handle memory allocation functions that cannot fail
   * (i.e., do not return NULL) and do not zero the memory.
   *
   * @param functionName The name of the memory allocation function.
   * @param parameters The list of function parameters.
   * @param e The function call expression.
   * @return A formula for the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  private Formula handleSuccessfulMemoryAllocation(final String functionName,
      List<CExpression> parameters,
      final CFunctionCallExpression e) throws UnrecognizedCCodeException, InterruptedException {
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
    } else if (isSizeofMultiple(parameter)) {
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
        newType = new CArrayType(false, false, CVoidType.VOID, length);
      } else {
        newType = null;
      }
    }
    Formula address;
    if (newType != null) {
      final String newBase = makeAllocVariableName(functionName, newType, ssa, conv);
      address =  makeAllocation(conv.options.isSuccessfulZallocFunctionName(functionName),
                                 newType,
                                 newBase);
    } else {
      final String newBase = makeAllocVariableName(functionName, CVoidType.VOID, ssa, conv);
      pts.addTemporaryDeferredAllocation(
          conv.options.isSuccessfulZallocFunctionName(functionName),
          Optional.ofNullable(size)
              .map(
                  (s) ->
                      new CIntegerLiteralExpression(
                          parameter.getFileLocation(),
                          parameter.getExpressionType(),
                          BigInteger.valueOf(s))),
          newBase);
      address = conv.makeConstant(PointerTargetSet.getBaseName(newBase), CPointerType.POINTER_TO_VOID);
      constraints.addConstraint(
          conv.fmgr.makeGreaterThan(
              address, conv.fmgr.makeNumber(typeHandler.getPointerType(), 0L), true));
    }

    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(address), address));
    }
    return address;
  }

  /**
   * Handles calls to {@code free()}.
   *
   * @param e The parameters of the {@code free()} call.
   * @param expressionVisitor A visitor to evaluate the value of the function call.
   * @return The return value of the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private Value handleMemoryFree(final CFunctionCallExpression e,
      final CExpressionVisitorWithPointerAliasing expressionVisitor) throws UnrecognizedCCodeException {
    final List<CExpression> parameters = e.getParameterExpressions();
    if (parameters.size() != 1) {
      throw new UnrecognizedCCodeException(
          String.format("free() called with %d parameters", parameters.size()), edge, e);
    }

    if (errorConditions.isEnabled()) {
      final Formula operand =
          expressionVisitor.asValueFormula(
              parameters.get(0).accept(expressionVisitor),
              typeHandler.getSimplifiedType(parameters.get(0)));
      BooleanFormula validFree = conv.fmgr.makeEqual(operand, conv.nullPointer);

      for (String base : pts.getAllBases()) {
        Formula baseF = conv.makeConstant(PointerTargetSet.getBaseName(base), CPointerType.POINTER_TO_VOID);
        validFree = conv.bfmgr.or(validFree, conv.fmgr.makeEqual(operand, baseF));
      }
      errorConditions.addInvalidFreeCondition(conv.bfmgr.not(validFree));
    }

    return Value.nondetValue(); // free does not return anything, so nondet is ok
  }

  /**
   * Creates a formula for memory allocations.
   *
   * @param isZeroing A flag indicating if the variable is zeroing.
   * @param type The type.
   * @param base The name of the base.
   * @return A formula for the memory allocation.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution gets interrupted.
   */
  private Formula makeAllocation(final boolean isZeroing, final CType type, final String base)
      throws UnrecognizedCCodeException, InterruptedException {
    final CType baseType = CTypeUtils.getBaseType(type);
    final Formula result = conv.makeConstant(PointerTargetSet.getBaseName(base), baseType);
    if (isZeroing) {
      AssignmentHandler assignmentHandler = new AssignmentHandler(conv, edge, base, ssa, pts, constraints, errorConditions, regionMgr);
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

  /**
   * Creates a name for an allocation.
   *
   * @param functionName The name of the function.
   * @param type The type of the function.
   * @param ssa The SSA map.
   * @param conv The C to SMT formula converter.
   * @return A name for allocations.
   */
  static String makeAllocVariableName(final String functionName, final CType type,
      final SSAMapBuilder ssa, final CtoFormulaConverter conv) {
    return functionName
        + "_"
        + CToFormulaConverterWithPointerAliasing.getPointerAccessNameForType(type)
        + MALLOC_INDEX_SEPARATOR
        + conv.makeFreshIndex(
            CToFormulaConverterWithPointerAliasing.SSAMAP_SYMBOL_WITHOUT_UPDATE_PREFIX
                + functionName,
            type,
            ssa);
  }

  /**
   * Tries to evaluate an expression, i.e. get the value if it is an integer literal.
   *
   * @param e The C expression.
   * @return The value, if the expression is an integer literal, or {@code null}
   */
  private static @Nullable Integer tryEvaluateExpression(CExpression e) {
    if (e instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)e).getValue().intValue();
    }
    return null;
  }

  /**
   * Returns, whether a expression is a {@code sizeof} expression.
   *
   * @param e The C expression.
   * @return True, if the expression is a {@code sizeof} expression, false otherwise.
   */
  private static boolean isSizeof(final CExpression e) {
    return (e instanceof CUnaryExpression
            && ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF)
        || (e instanceof CTypeIdExpression
            && ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF);
  }

  /**
   * Returns, whether the expression is a multiplication of the {@code sizeof} operator.
   *
   * @param e The expression type.
   * @return True, if the expression is a multiplication of the {@code sizeof} operator, false
   * otherwise.
   */
  private static boolean isSizeofMultiple(final CExpression e) {
    return e instanceof CBinaryExpression &&
           ((CBinaryExpression) e).getOperator() == BinaryOperator.MULTIPLY &&
           (isSizeof(((CBinaryExpression) e).getOperand1()) ||
            isSizeof(((CBinaryExpression) e).getOperand2()));
  }

  /**
   * Returns a C type for the size of an expression.
   *
   * @param e The expression type to get the size from.
   * @return The size of the expression.
   */
  private @Nullable CType getSizeofType(CExpression e) {
    if (e instanceof CUnaryExpression &&
        ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF) {
      return typeHandler.getSimplifiedType(((CUnaryExpression) e).getOperand());
    } else if (e instanceof CTypeIdExpression &&
               ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF) {
      return typeHandler.simplifyType(((CTypeIdExpression) e).getType());
    } else {
      return null;
    }
  }


  // Handling of deferred allocations

  /**
   * The function tries to recover dynamically allocated array type from the pointer type it was casted or assigned to.
   *
   * @param type the revealing <em>pointed</em> type (e.g. {@code char} for {@code char *}) of the pointer
   *        to which the void * variable was casted or assigned to.
   * @param sizeLiteral the size specified at the allocation site.
   * @return the recovered array type or the {@code type} parameter in case the type can't be recovered
   */
  private CType refineType(final CType type, final CIntegerLiteralExpression sizeLiteral) {
    assert sizeLiteral.getValue() != null;

    final int size = sizeLiteral.getValue().intValue();
    final int typeSize = conv.getSizeof(type);
    if (type instanceof CArrayType) {
      // An array type is used in the cast or assignment, so its size should likely match the allocated size.
      // Issue a warning if this isn't the case
      if (typeSize != size) {
        conv.logger.logf(Level.WARNING,
                         "Array size of the revealed type differs form the allocation size: %s : %d != %d",
                         type,
                         typeSize,
                         size);
      }
      // The type used is already an array type, nothing to recover
      return type;
    } else {
      // A pointer type is used in the cast or assignment.
      // If the allocated size is the multiple of the usage type size, we can recover the
      // actual array type (with length) of the allocation. Otherwise, just return the usage type.
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
  }

  private static CType unwrapPointers(final CType type) {
    if (type instanceof CPointerType) {
      return unwrapPointers(((CPointerType) type).getType());
    }
    return type;
  }

  /**
   * Returns the type of the allocated dynamic variable by the revealed non-void pointer type of the
   * void * variable and the allocation size
   *
   * @param type The usage pointer type.
   * @param sizeLiteral The allocation size.
   * @return The type of the allocated dynamic variable.
   */
  private CType getAllocationType(
      final CType type, final Optional<CIntegerLiteralExpression> sizeLiteral) {
    if (type instanceof CPointerType) {
      final CType tt = unwrapPointers(type);
      return sizeLiteral.map((s) -> refineType(tt, s)).orElse(tt);
    } else if (type instanceof CArrayType) {
      return sizeLiteral.map((s) -> refineType(type, s)).orElse(type);
    } else {
      throw new IllegalArgumentException("Either pointer or array type expected");
    }
  }

  /**
   * Handles the type revelation of deferred allocations.
   *
   * @param pointer The name of the pointer variable/field.
   * @param type The type of the pointer variable/field.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException if the execution was interrupted.
   */
  private void handleDeferredAllocationTypeRevelation(final String pointer, final CType type)
      throws UnrecognizedCCodeException, InterruptedException {
    for (DeferredAllocation d : pts.removeDeferredAllocations(pointer)) {
      makeAllocation(d.isZeroed(), getAllocationType(type, d.getSize()), d.getBase());
    }
  }

  /**
   * Handles deferred allocations in assignment expressions.
   *
   * @param lhs The left hand side of the C expression.
   * @param rhs The right hand side of the C expression.
   * @param rhsExpression The expression of the right hand side.
   * @param lhsType The type of the left hand side.
   * @param lhsLearnedPointerTypes A map of all used deferred allocation pointers on the left hand side.
   * @param rhsLearnedPointerTypes A map of all used deferred allocation pointers on the right hand side.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  void handleDeferredAllocationsInAssignment(
      final CLeftHandSide lhs,
      final CRightHandSide rhs,
      final Expression rhsExpression,
      final CType lhsType,
      final CExpressionVisitorWithPointerAliasing visitor,
      final Map<String, CType> lhsLearnedPointerTypes,
      final Map<String, CType> rhsLearnedPointerTypes)
      throws UnrecognizedCCodeException, InterruptedException {
    // Handle allocations: reveal the actual type form the LHS type or defer the allocation until later
    boolean isAllocation = false;
    if ((conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) &&
        rhs instanceof CFunctionCallExpression &&
        !rhsExpression.isNondetValue() && rhsExpression.isValue()) {
      final Set<String> rhsVariables = conv.fmgr.extractVariableNames(rhsExpression.asValue().getValue());
      // Actually there is always either 1 variable (just address) or 2 variables (nondet + allocation address)
      for (final String mangledVariable : rhsVariables) {
        final String variable =
            PointerTargetSet.isBaseName(mangledVariable)
                ? PointerTargetSet.getBase(mangledVariable)
                : mangledVariable;
        if (pts.isTemporaryDeferredAllocationPointer(variable)) {
          if (!isAllocation) {
            // We can reveal the type from the LHS
            if (CExpressionVisitorWithPointerAliasing.isRevealingType(lhsType)) {
              handleDeferredAllocationTypeRevelation(variable, lhsType);
            // We can defer the allocation and start tracking the variable in the LHS
            } else {
              final Optional<String> lhsPointer =
                  lhs.accept(visitor.getPointerApproximatingVisitor());
              lhsPointer.ifPresent(
                  (s) -> {
                    pts.removeDeferredAllocationPointer(s)
                        .forEach((_d) -> handleDeferredAllocationPointerRemoval(s));
                    pts.addDeferredAllocationPointer(s, variable); // Now we track the LHS
                    // And not the RHS, it was a dummy, not a code pointer approximation
                    pts.removeDeferredAllocationPointer(variable)
                        .forEach((_d) -> handleDeferredAllocationPointerRemoval(variable));
                  });
              if (!lhsPointer.isPresent()) {
                conv.logger.logfOnce(
                    Level.WARNING,
                    "Can't start tracking deferred allocation -- can't approximate this LHS: %s (here: %s)",
                    lhs,
                    edge);
                pts.removeDeferredAllocationPointer(variable)
                    .forEach((_d) -> handleDeferredAllocationPointerRemoval(variable));
              }
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
      handleDeferredAllocationsInAssignment(
          lhs, rhs, lhsType, visitor, lhsLearnedPointerTypes, rhsLearnedPointerTypes);
    }
  }

  /**
   * Handles deferred allocations in assignment expressions.
   *
   * @param lhs The left hand side of the C expression.
   * @param rhs The right hand side of the C expression.
   * @param lhsLearnedPointerTypes A map of all used deferred allocation pointers on the left hand side.
   * @param rhsLearnedPointerTypes A map of all used deferred allocation pointers on the right hand side.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  private void handleDeferredAllocationsInAssignment(
      final CLeftHandSide lhs,
      final CRightHandSide rhs,
      final CType lhsType,
      final CExpressionVisitorWithPointerAliasing visitor,
      final Map<String, CType> lhsLearnedPointerTypes,
      final Map<String, CType> rhsLearnedPointerTypes)
      throws UnrecognizedCCodeException, InterruptedException {
    if (!(lhsType instanceof CPointerType || lhsType instanceof CArrayType)) {
      return;
    }
    /* The order of handling in the following:
     *   1. Revealing learned types, allocating corresponding pointed objects and removing the objects from
     *   deferred allocations
     *   2. Removing pointer-object relations in case of a variable used in the LHS
     *   3. Propagating aliases according to the assignment
     *
     *   This is the only possible order as if we rearranged 1 with 2 we wouldn't be able to find pointed objects
     *   for allocation as the corresponding relations would have been removed on the first step, if we rearranged
     *   2 with 3 we would remove extra relations arising from the premature propagation.
     */
    final CType lType = typeHandler.simplifyType(lhsType);
    final CType rType =
        rhs != null ? typeHandler.getSimplifiedType(rhs) : CPointerType.POINTER_TO_VOID;
    final Optional<Pair<CRightHandSide, CType>> toHandle;
    if (rhs != null && CExpressionVisitorWithPointerAliasing.isRevealingType(lType)) {
      toHandle = Optional.of(Pair.of(rhs, lType));
    } else if (CExpressionVisitorWithPointerAliasing.isRevealingType(rType)) {
      toHandle = Optional.of(Pair.of(lhs, rType));
    } else {
      toHandle = Optional.empty();
    }

    // Reveal the type from usages (type casts, comparisons) in both sides
    for (Map.Entry<String, CType> entry : lhsLearnedPointerTypes.entrySet()) {
      handleDeferredAllocationTypeRevelation(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, CType> entry : rhsLearnedPointerTypes.entrySet()) {
      handleDeferredAllocationTypeRevelation(entry.getKey(), entry.getValue());
    }

    // Reveal the type from the assignment itself (i.e. lhs from rhs and vice versa)
    if (toHandle.isPresent()) {
      Optional<String> s =
          toHandle.get().getFirst().accept(visitor.getPointerApproximatingVisitor());
      if (s.isPresent()
          && !lhsLearnedPointerTypes.containsKey(s.get())
          && !rhsLearnedPointerTypes.containsKey(s.get())) {
        handleDeferredAllocationTypeRevelation(s.get(), toHandle.get().getSecond());
      }
    }

    if (lhs instanceof CIdExpression) {
      // If LHS is a variable, remove previous points-to bindings containing it
      pts.removeDeferredAllocationPointer(((CIdExpression) lhs).getDeclaration().getQualifiedName())
          .forEach(_d -> handleDeferredAllocationPointerRemoval(lhs));
    } else {
      // Else try to remove bindings and only actually remove if no dangling objects arises
      Optional<String> lhsPointer = lhs.accept(visitor.getPointerApproximatingVisitor());
      if (lhsPointer.isPresent() && pts.canRemoveDeferredAllocationPointer(lhsPointer.get())) {
        pts.removeDeferredAllocationPointer(lhsPointer.get());
      }
    }

    // And now propagate points-to bindings from the RHS to the LHS
    Optional<String> l = lhs.accept(visitor.getPointerApproximatingVisitor());
    if (l.isPresent() && rhs != null) {
      rhs.accept(visitor.getPointerApproximatingVisitor())
          .ifPresent(r -> pts.addDeferredAllocationPointer(l.get(), r));
    }
  }

  /**
   * Handles deferred allocations in assume expressions.
   *
   * @param e The expression in the C code.
   * @param learnedPointerTypes A map of all used deferred allocation pointers.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution gets interrupted.
   */
  void handleDeferredAllocationsInAssume(
      final CExpression e, final Map<String, CType> learnedPointerTypes)
      throws UnrecognizedCCodeException, InterruptedException {
    for (Map.Entry<String, CType> entry : learnedPointerTypes.entrySet()) {
      handleDeferredAllocationTypeRevelation(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Removes a pointer variable from tracking.
   *
   * @param pointer The expression or string corresponding to the pointer.
   */
  private void handleDeferredAllocationPointerRemoval(final Object pointer) {
    conv.logger.logfOnce(
        Level.WARNING,
        "%s: Assignment to the void* pointer %s produces garbage or the memory pointed by it is unused: %s",
        edge.getFileLocation(),
        pointer,
        edge.getDescription());
  }

  /**
   * The function removes local void * pointers (deferred allocations)
   * declared in current function scope from tracking after returning from the function.
   *
   * @param function The name of the function.
   */
  void handleDeferredAllocationInFunctionExit(final String function) {
    for (String v :
        CFAUtils.filterVariablesOfFunction(
            ImmutableSortedSet.copyOf(pts.getDeferredAllocationPointers()), function)) {
      if (!pts.removeDeferredAllocationPointer(v).isEmpty()) {
        conv.logger.logfOnce(
            Level.WARNING,
            "%s: Destroying the void* pointer %s produces garbage or the memory pointed by it is unused: %s",
            edge.getFileLocation(),
            v,
            edge.getDescription());
      }
    }
  }
}
