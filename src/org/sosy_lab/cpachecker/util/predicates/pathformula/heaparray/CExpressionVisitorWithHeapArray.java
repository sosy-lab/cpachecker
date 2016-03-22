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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.ast.c.AdaptingCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.BaseVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Variable;
import org.sosy_lab.solver.api.Formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * A visitor the handle C expressions with the support for pointer aliasing by using SMT arrays
 * to encode heap accesses.
 */
class CExpressionVisitorWithHeapArray
    extends DefaultCExpressionVisitor<Expression, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Expression, UnrecognizedCCodeException> {

  /**
   * A simple expression to formula visitor.
   */
  private static class AdaptingExpressionToFormulaVisitor
      extends AdaptingCExpressionVisitor<Formula, Expression, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    /**
     * Creates a new expression to formula visitor.
     *
     * @param pDelegate The delegate.
     */
    private AdaptingExpressionToFormulaVisitor(
        final CExpressionVisitorWithHeapArray pDelegate) {
      super(pDelegate);
    }

    /**
     * Returns a formula for an expression value on a right hand side.
     *
     * @param pValue The expression value.
     * @param pRhs   The right hand side.
     * @return A formula for the expression value.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    protected Formula convert(final Expression pValue, final CExpression pRhs)
        throws UnrecognizedCCodeException {
      return convert0(pValue, pRhs);
    }

    /**
     * Returns a formula for an expression value on a right hand side.
     *
     * @param pValue The expression value.
     * @param pRhs   The right hand side.
     * @return A formula for the expression value.
     */
    private Formula convert0(
        final Expression pValue,
        final CRightHandSide pRhs) {
      CType type = CTypeUtils.simplifyType(pRhs.getExpressionType());
      return ((CExpressionVisitorWithHeapArray) delegate).asValueFormula(pValue, type);
    }

    /**
     * Evaluates a formula for a function call expression.
     *
     * @param pExpression The function call expression.
     * @return A formula for the function call expression.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public Formula visit(final CFunctionCallExpression pExpression)
        throws UnrecognizedCCodeException {
      return convert0(((CExpressionVisitorWithHeapArray) delegate).visit(pExpression),
          pExpression);
    }
  }

  private final CToFormulaConverterWithHeapArray converter;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final PointerTargetSetBuilder pts;

  private final BaseVisitor baseVisitor;
  private final ExpressionToFormulaVisitor delegate;

  private final List<Pair<CCompositeType, String>> usedFields = new ArrayList<>(1);
  private final List<Pair<CCompositeType, String>> initializedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> addressedFields = new ArrayList<>();
  private final Map<String, CType> usedDeferredAllocationPointers =
      Maps.newHashMapWithExpectedSize(1);

  /**
   * Creates a new visitor for C expression supporting pointer aliasing.
   *
   * @param pConverter               The C to SMT formula converter.
   * @param pCFAEdge                 The current edge in the CFA (for logging purposes).
   * @param pFunction                The name of the current function (for logging purposes).
   * @param pSSAMapBuilder           The SSA map.
   * @param pConstraints             Additional constraints from CFA parsing.
   * @param pErrorConditions         Additional error conditions.
   * @param pPointerTargetSetBuilder The underlying set of pointer targets.
   */
  CExpressionVisitorWithHeapArray(
      final CToFormulaConverterWithHeapArray pConverter,
      final CFAEdge pCFAEdge,
      final String pFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions,
      final PointerTargetSetBuilder pPointerTargetSetBuilder) {

    delegate = new ExpressionToFormulaVisitor(pConverter, pConverter.formulaManager, pCFAEdge,
        pFunction, pSSAMapBuilder, pConstraints) {
      @Override
      protected Formula toFormula(CExpression pExpression) throws UnrecognizedCCodeException {
        // recursive application of pointer-aliasing.
        return asValueFormula(pExpression.accept(CExpressionVisitorWithHeapArray.this),
            CTypeUtils.simplifyType(pExpression.getExpressionType()));
      }
    };

    converter = pConverter;
    edge = pCFAEdge;
    ssa = pSSAMapBuilder;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    pts = pPointerTargetSetBuilder;

    baseVisitor = new BaseVisitor(pCFAEdge, pPointerTargetSetBuilder);
  }

  /**
   * Returns a formula visitor for right hand side expressions.
   *
   * @return A formula visitor for right hand side expressions.
   */
  CRightHandSideVisitor<Formula, UnrecognizedCCodeException> asFormulaVisitor() {
    return new AdaptingExpressionToFormulaVisitor(this);
  }

  /**
   * Adds a constraint that both given formulae have the same base address.
   *
   * @param pFormulaTerm1 The first formula.
   * @param pFormulaTerm2 The second formula.
   */
  private void addEqualBaseAddressConstraint(
      final Formula pFormulaTerm1,
      final Formula pFormulaTerm2) {
    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(converter.formulaManager.makeEqual(
          converter.makeBaseAddressOfTerm(pFormulaTerm1),
          converter.makeBaseAddressOfTerm(pFormulaTerm2)));
    }
  }

  /**
   * Creates a formula for the value of an expression.
   *
   * @param pExpression The expression.
   * @param pType       The type of the expression.
   * @param pIsSafe     A flag, if the formula is safe or not.
   * @return A formula for the value.
   */
  private Formula asValueFormula(
      final Expression pExpression,
      final CType pType,
      final boolean pIsSafe) {
    if (pExpression.isValue()) {
      return pExpression.asValue().getValue();
    } else if (pExpression.asLocation().isAliased()) {
      return !pIsSafe
             ? converter.makeDereference(pType, pExpression.asLocation().asAliased().getAddress(),
                 ssa, errorConditions)
             : converter.makeSafeDereference(pType,
                 pExpression.asLocation().asAliased().getAddress(), ssa);
    } else { // Unaliased location
      return converter.makeVariable(pExpression.asLocation().asUnaliased().getVariableName(),
          pType, ssa);
    }
  }

  /**
   * Creates a formula for the value of an expression.
   *
   * @param pExpression The expression.
   * @param pType       The type of the expression.
   * @return A formula for the value.
   */
  Formula asValueFormula(final Expression pExpression, final CType pType) {
    return asValueFormula(pExpression, pType, false);
  }

  /**
   * Creates a safe formula for the value of an expression.
   *
   * @param pExpression The expression.
   * @param pType       The type of the expression.
   * @return A safe formula for the value.
   */
  private Formula asSafeValueFormula(final Expression pExpression, final CType pType) {
    return asValueFormula(pExpression, pType, true);
  }

  /**
   * Evaluates the aliased location of an array subscript expression.
   *
   * @param pExpression The array expression.
   * @return The location of the array expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public AliasedLocation visit(final CArraySubscriptExpression pExpression)
      throws UnrecognizedCCodeException {
    Expression base = pExpression.getArrayExpression().accept(this);
    // There are two distinct kinds of arrays in C:
    // -- fixed-length arrays for which the aliased location of the first
    //    element is returned here
    // -- pointers implicitly converted to arrays for which either the aliased
    //    or unaliased location of the *pointer* is returned
    final CType baseType = CTypeUtils.simplifyType(
        pExpression.getArrayExpression().getExpressionType());
    // Fixed-length arrays
    // TODO: Check if fixed-sized arrays and pointers can be clearly distinguished this way
    if (baseType instanceof CArrayType && ((CArrayType) baseType).getLength() != null) {
      assert base.isAliasedLocation();
    } else {
      // The address of the first element is needed i.e. the value of the
      // pointer in the array expression
      base = AliasedLocation.ofAddress(asValueFormula(base,
          CTypeUtils.implicitCastToPointer(baseType)));
    }
    // Now we should always have the aliased location of the first array element
    assert base.isAliasedLocation();

    final CType elementType = CTypeUtils.simplifyType(pExpression.getExpressionType());
    final CExpression subscript = pExpression.getSubscriptExpression();
    final CType subscriptType = CTypeUtils.simplifyType(subscript.getExpressionType());
    final Formula index = converter.makeCast(subscriptType,
        CPointerType.POINTER_TO_VOID, asValueFormula(subscript.accept(this),
            subscriptType), constraints, edge);

    final Formula coeff = converter.formulaManager.makeNumber(
        converter.voidPointerFormulaType, converter.getSizeof(elementType));
    final Formula baseAddress = base.asAliasedLocation().getAddress();
    final Formula address = converter.formulaManager.makePlus(baseAddress,
        converter.formulaManager.makeMultiply(coeff, index));
    addEqualBaseAddressConstraint(baseAddress, address);
    return AliasedLocation.ofAddress(address);
  }

  /**
   * Evaluates the location of the reference to a field of a composite type.
   *
   * @param pExpression The reference to a field.
   * @return The location of the reference.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Location visit(CFieldReference pExpression)
      throws UnrecognizedCCodeException {

    pExpression = CToFormulaConverterWithHeapArray.eliminateArrow(pExpression, edge);

    final Variable variable = pExpression.accept(baseVisitor);
    if (variable != null) {
      final String variableName = variable.getName();
      if (pts.isDeferredAllocationPointer(variableName)) {
        usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
      }
      return UnaliasedLocation.ofVariableName(variableName);
    } else {
      final CType fieldOwnerType = CTypeUtils.simplifyType(
          pExpression.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CCompositeType) {
        final AliasedLocation base = pExpression.getFieldOwner().accept(this).asAliasedLocation();

        final String fieldName = pExpression.getFieldName();
        usedFields.add(Pair.of((CCompositeType) fieldOwnerType, fieldName));
        final Formula offset = converter.formulaManager.makeNumber(
            converter.voidPointerFormulaType,
            converter.ptsMgr.getOffset((CCompositeType) fieldOwnerType,
                fieldName));

        final Formula address = converter.formulaManager.makePlus(base.getAddress(), offset);
        addEqualBaseAddressConstraint(base.getAddress(), address);
        return AliasedLocation.ofAddress(address);
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type",
            edge, pExpression);
      }
    }
  }

  /**
   * Checks, whether a C expression is an unaliased location or not.
   *
   * @param pExpression The C expression to check.
   * @return Whether the expression is an unaliased location or not.
   */
  static boolean isUnaliasedLocation(final CExpression pExpression) {
    if (pExpression instanceof CIdExpression) {
      return true;
    } else if (pExpression instanceof CFieldReference
        && !((CFieldReference) pExpression).isPointerDereference()) {
      return isUnaliasedLocation(((CFieldReference) pExpression).getFieldOwner());
    } else {
      return false;
    }
  }

  /**
   * Checks, whether the given type is revealing or not.
   *
   * @param pType The type to check.
   * @return Whether the given type is revealing or not.
   */
  static boolean isRevealingType(final CType pType) {
    return (pType instanceof CPointerType || pType instanceof CArrayType)
        && !pType.equals(CPointerType.POINTER_TO_VOID);
  }

  /**
   * Evaluates the expression of a cast expression.
   *
   * @param pExpression The C cast expression.
   * @return The expression representing the cast.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(final CCastExpression pExpression)
      throws UnrecognizedCCodeException {
    final CType resultType = CTypeUtils.simplifyType(pExpression.getExpressionType());
    final CExpression operand = converter.makeCastFromArrayToPointerIfNecessary(
        pExpression.getOperand(), resultType);

    final Expression result = operand.accept(this);

    // TODO: is the second isUnaliasedLocation() check really needed?
    if (isRevealingType(resultType)
        && isUnaliasedLocation(operand)
        && result.isUnaliasedLocation()) {
      final String variableName = result.asLocation().asUnaliased().getVariableName();
      if (pts.isDeferredAllocationPointer(variableName)) {
        assert usedDeferredAllocationPointers.containsKey(variableName)
            && usedDeferredAllocationPointers.get(variableName).equals(
            CPointerType.POINTER_TO_VOID) : "Wrong assumptions on deferred "
            + "allocations tracking: unknown pointer encountered";
        usedDeferredAllocationPointers.put(variableName, resultType);
      }
    }

    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    if (CTypeUtils.isSimpleType(resultType)) {
      return Value.ofValue(converter.makeCast(operandType, resultType,
          asValueFormula(result, operandType), constraints, edge));
    } else if (CTypes.withoutConst(resultType).equals(CTypes.withoutConst(operandType))) {
      // Special case: conversion of non-scalar type to itself is allowed
      // (and ignored) Change of const modifier is ignored, too.
      return result;
    } else {
      throw new UnrecognizedCCodeException("Conversion to non-scalar type requested",
          edge, pExpression);
    }
// TODO: The following heuristic should be implemented in more generally in the assignment to p
//    if (operand instanceof CPointerExpression
//        && !(resultType instanceof CFunctionType)) {
//      // Heuristic:
//      // When there is (t)*p, we treat it like *((*t)p)
//      // This means the UF for type t get's used instead of the UF for actual type of p.
//    }
  }

  /**
   * Evaluates the expression of a identification expression.
   *
   * @param pExpression The C id expression.
   * @return The expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(final CIdExpression pExpression)
      throws UnrecognizedCCodeException {
    Variable variable = pExpression.accept(baseVisitor);
    final CType resultType = CTypeUtils.simplifyType(pExpression.getExpressionType());
    if (variable != null) {
      if (!(pExpression.getDeclaration() instanceof CFunctionDeclaration)) {
        final String variableName = variable.getName();
        if (pts.isDeferredAllocationPointer(variableName)) {
          usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
        }

        return UnaliasedLocation.ofVariableName(variableName);
      } else {
        return Value.ofValue(converter.makeConstant(variable.getName(), variable.getType()));
      }
    } else {
      final Formula address = converter.makeConstant(
          PointerTargetSet.getBaseName(pExpression.getDeclaration().getQualifiedName()),
          CTypeUtils.getBaseType(resultType));

      return AliasedLocation.ofAddress(address);
    }
  }

  /**
   * Evaluates the value of an unary expression in C.
   *
   * @param pExpression The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CUnaryExpression pExpression)
      throws UnrecognizedCCodeException {
    if (pExpression.getOperator() == UnaryOperator.AMPER) {
      final CExpression operand = pExpression.getOperand();

      final Variable baseVariable = operand.accept(baseVisitor);
      if (baseVariable == null) {
        AliasedLocation addressExpression = null;

        // addressedFields is used to treat structure assignment and field addressing separately:
        // assuming s1 and s2 both have substructure ss1, which in its turn has fields f1 and f2,
        // and there is also outer structure ss2 of the same type as ss1, in
        // s1.ss1 = s2.ss1;
        // ss2.f1 = 0;
        // it isn't necessary to start tracking
        // either s1.ss1.f{1,2} or s2.ss1.f{1,2}, because as s{1,2}.ss1 itself is not tracked,
        // it's known that their values remain undefined and only some other outer structure field is assigned.
        // But in
        // p2 = &(s2.ss1);
        // p2->f1 = 0;
        // the fields f1 and f2 along with the field s{1,2}.ss1 should be tracked from the first line onwards, because
        // it's too hard to determine (without the help of some alias analysis)
        // whether f1 assigned in the second line is an outer or inner structure field.
        final List<Pair<CCompositeType, String>> alreadyUsedFields =
            ImmutableList.copyOf(usedFields);
        usedFields.clear();

        if (errorConditions.isEnabled() && operand instanceof CFieldReference) {
          // for &(s->f) and &((*s).f) do special case because the pointer is
          // not actually dereferenced and thus we don't want to add error conditions
          // for invalid-deref
          final CFieldReference field = (CFieldReference) operand;
          CExpression fieldOwner = field.getFieldOwner();
          boolean isDeref = field.isPointerDereference();
          if (!isDeref && fieldOwner instanceof CPointerExpression) {
            fieldOwner = ((CPointerExpression) fieldOwner).getOperand();
            isDeref = true;
          }
          if (isDeref) {
            final CPointerType pointerType =
                (CPointerType) CTypeUtils.simplifyType(fieldOwner.getExpressionType());
            final Formula base = asSafeValueFormula(fieldOwner.accept(this), pointerType);
            final String fieldName = field.getFieldName();
            final CCompositeType compositeType =
                (CCompositeType) CTypeUtils.simplifyType(pointerType.getType());
            usedFields.add(Pair.of(compositeType, fieldName));
            final Formula offset = converter.formulaManager.makeNumber(
                converter.voidPointerFormulaType,
                converter.ptsMgr.getOffset(compositeType, fieldName));
            addressExpression = AliasedLocation.ofAddress(
                converter.formulaManager.makePlus(base, offset));
            addEqualBaseAddressConstraint(base, addressExpression.getAddress());
          }
        }

        if (addressExpression == null) {
          addressExpression = operand.accept(this).asAliasedLocation();
        }

        addressedFields.addAll(usedFields);
        usedFields.addAll(alreadyUsedFields);

        return Value.ofValue(addressExpression.getAddress());

      } else {
        final Variable base = baseVisitor.getLastBase();
        final Formula baseAddress = converter.makeConstant(
            PointerTargetSet.getBaseName(base.getName()),
            CTypeUtils.getBaseType(base.getType()));
        converter.addValueImportConstraints(edge, baseAddress, base, initializedFields, ssa,
            constraints);

        if (converter.hasIndex(base.getName(), base.getType(), ssa)) {
          ssa.deleteVariable(base.getName());
        }

        converter.addPreFilledBase(base.getName(), base.getType(),
            pts.isPreparedBase(base.getName()), false, constraints, pts);
        return visit(pExpression);
      }
    } else {
      return visitDefault(pExpression);
    }
  }

  /**
   * Evaluates the aliased location of a pointer expression.
   *
   * @param pExpression The C pointer expression.
   * @return The aliased location of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public AliasedLocation visit(final CPointerExpression pExpression)
      throws UnrecognizedCCodeException {
    final CExpression operand = pExpression.getOperand();
    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    final Expression operandExpression = operand.accept(this);
    if (operandType instanceof CArrayType && ((CArrayType) operandType).getLength() != null) {
      return operandExpression.asAliasedLocation();
    } else {
      return AliasedLocation.ofAddress(asValueFormula(operandExpression, operandType));
    }
  }

  /**
   * Evaluates the value of a binary expression.
   *
   * @param pExpression The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CBinaryExpression pExpression)
      throws UnrecognizedCCodeException {
    final CType returnType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();
    final Formula f1 = delegate.processOperand(
        pExpression.getOperand1(), calculationType, returnType);
    final Formula f2 = delegate.processOperand(
        pExpression.getOperand2(), calculationType, returnType);

    final Formula result = delegate.handleBinaryExpression(pExpression, f1, f2);

    final CType t1 = CTypeUtils.simplifyType(pExpression.getOperand1().getExpressionType());
    final CType t2 = CTypeUtils.simplifyType(pExpression.getOperand2().getExpressionType());
    final BinaryOperator op = pExpression.getOperator();

    switch (op) {
      case PLUS:
        if (t1 instanceof CPointerType) {
          addEqualBaseAddressConstraint(result, f1);
        }
        if (t2 instanceof CPointerType) {
          addEqualBaseAddressConstraint(result, f2);
        }
        break;
      case MINUS:
        // TODO addEqualBaseAddressConstraints here, too?
      default:
        // Does not occur for pointers
        break;
    }

    return Value.ofValue(result);
  }

  /**
   * Evaluates the return value of a C expression.
   *
   * @param pExpression The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  protected Value visitDefault(final CExpression pExpression)
      throws UnrecognizedCCodeException {
    return Value.ofValue(pExpression.accept(delegate));
  }

  /**
   * Evaluates the return value of a function call expression.
   *
   * @param pExpression The function call expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @SuppressWarnings("deprecation")
  @Override
  public Value visit(final CFunctionCallExpression pExpression)
      throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = pExpression.getFunctionNameExpression();

    // First let's handle special cases such as allocations
    if (functionNameExpression instanceof CIdExpression) {
      final String functionName = ((CIdExpression) functionNameExpression).getName();

      if (converter.options.isDynamicMemoryFunction(functionName)) {
        DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(converter, edge, ssa, pts,
            constraints, errorConditions);
        try {
          return memoryHandler.handleDynamicMemoryFunction(pExpression, functionName, this);
        } catch (InterruptedException exc) {
          // Throwing two checked exception from this visitor is not possible directly.
          // The following does the same although it is not recommended to do so.
          // However, we are sure that an InterrupedException from this visitor
          // will be handled correctly outside.
          Thread.currentThread().stop(exc);
        }
      }
    }

    // Pure functions returning composites are unsupported, return a nondet value
    final CType resultType = CTypeUtils.simplifyType(converter.getReturnType(pExpression, edge));
    if (resultType instanceof CCompositeType || CTypeUtils.containsArray(resultType)) {
      converter.logger.logfOnce(Level.WARNING, "Extern function %s returning a composite is "
          + "treated as nondet.", pExpression);
      return Value.nondetValue();
    }

    // Delegate
    return Value.ofValue(delegate.visit(pExpression));
  }

  /**
   * Returns a list of the used fields of composite types.
   *
   * @return A list of the used fields.
   */
  List<Pair<CCompositeType, String>> getUsedFields() {
    return Collections.unmodifiableList(usedFields);
  }

  /**
   * Returns a list of the initialized fields of composite types.
   *
   * @return A list of the initialized fields.
   */
  List<Pair<CCompositeType, String>> getInitializedFields() {
    return Collections.unmodifiableList(initializedFields);
  }

  /**
   * Returns a list of the addressed fields of composite types.
   *
   * @return A list of the addressed fields.
   */
  List<Pair<CCompositeType, String>> getAddressedFields() {
    return Collections.unmodifiableList(addressedFields);
  }

  /**
   * Returns a map of the used deferred allocation pointers.
   *
   * @return A map of the used deferred allocation pointers.
   */
  Map<String, CType> getUsedDeferredAllocationPointers() {
    return Collections.unmodifiableMap(usedDeferredAllocationPointers);
  }

}
