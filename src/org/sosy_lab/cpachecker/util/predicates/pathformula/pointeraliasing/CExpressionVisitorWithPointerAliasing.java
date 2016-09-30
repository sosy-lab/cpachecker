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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.java_smt.api.Formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * A visitor the handle C expressions with the support for pointer aliasing.
 */
class CExpressionVisitorWithPointerAliasing extends DefaultCExpressionVisitor<Expression, UnrecognizedCCodeException>
                                       implements CRightHandSideVisitor<Expression, UnrecognizedCCodeException> {

  /**
   * A simple expression to formula visitor.
   */
  private static class AdaptingExpressionToFormulaVisitor extends AdaptingCExpressionVisitor<Formula, Expression, UnrecognizedCCodeException>
                                                             implements CRightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    /**
     * Creates a new expression to formula visitor.
     *
     * @param pDelegate The delegate.
     */
    private AdaptingExpressionToFormulaVisitor(CExpressionVisitorWithPointerAliasing pDelegate) {
      super(pDelegate);
    }

    /**
     * Returns a formula for an expression value on a right hand side.
     *
     * @param value The expression value.
     * @param rhs The right hand side.
     * @return A formula for the expression value.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    protected Formula convert(Expression value, CExpression rhs) throws UnrecognizedCCodeException {
      return convert0(value, rhs);
    }

    /**
     * Returns a formula for an expression value on a right hand side.
     *
     * @param value The expression value.
     * @param rhs The right hand side.
     * @return A formula for the expression value.
     */
    private Formula convert0(Expression value, CRightHandSide rhs) {
      CExpressionVisitorWithPointerAliasing v = (CExpressionVisitorWithPointerAliasing) delegate;
      CType type = v.typeHandler.getSimplifiedType(rhs);
      return v.asValueFormula(value, type);
    }

    /**
     * Evaluates a formula for a function call expression.
     *
     * @param e The function call expression.
     * @return A formula for the function call expression.
     * @throws UnrecognizedCCodeException If the C code was unrecognizable.
     */
    @Override
    public Formula visit(CFunctionCallExpression e) throws UnrecognizedCCodeException {
      return convert0(((CExpressionVisitorWithPointerAliasing)delegate).visit(e), e);
    }
  }

  /**
   * Creates a new visitor for C expression supporting pointer aliasing.
   *
   * @param cToFormulaConverter The C to SMT formula converter.
   * @param cfaEdge The current edge in the CFA (for logging purposes).
   * @param function The name of the current function (for logging purposes).
   * @param ssa The SSA map.
   * @param constraints Additional constraints from CFA parsing.
   * @param errorConditions Additional error conditions.
   * @param pts The underlying set of pointer targets.
   */
  CExpressionVisitorWithPointerAliasing(final CToFormulaConverterWithPointerAliasing cToFormulaConverter,
                                          final CFAEdge cfaEdge,
                                          final String function,
                                          final SSAMapBuilder ssa,
                                          final Constraints constraints,
                                          final ErrorConditions errorConditions,
                                          final PointerTargetSetBuilder pts,
                                          final MemoryRegionManager regionMgr) {

    delegate =
        new ExpressionToFormulaVisitor(
            cToFormulaConverter,
            cToFormulaConverter.fmgr,
            cfaEdge,
            function,
            ssa,
            constraints) {
          @Override
          protected Formula toFormula(CExpression e) throws UnrecognizedCCodeException {
            // recursive application of pointer-aliasing.
            return asValueFormula(
                e.accept(CExpressionVisitorWithPointerAliasing.this),
                typeHandler.getSimplifiedType(e));
          }
        };

    this.conv = cToFormulaConverter;
    this.typeHandler = cToFormulaConverter.typeHandler;
    this.edge = cfaEdge;
    this.ssa = ssa;
    this.constraints = constraints;
    this.errorConditions = errorConditions;
    this.pts = pts;
    this.regionMgr = regionMgr;
    this.baseVisitor = new BaseVisitor(cfaEdge, pts, typeHandler);
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
   * @param p1 The first formula.
   * @param p2 The second formula.
   */
  private void addEqualBaseAddressConstraint(final Formula p1, final Formula p2) {
    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(p1),
                                                    conv.makeBaseAddressOfTerm(p2)));
    }
  }

  /**
   * Creates a formula for the value of an expression.
   *
   * @param e The expression.
   * @param type       The type of the expression.
   * @param isSafe     A flag, if the formula is safe or not.
   * @return A formula for the value.
   */
  private Formula asValueFormula(final Expression e, final CType type, final boolean isSafe) {
    if (e.isValue()) {
      return e.asValue().getValue();
    } else if (e.isAliasedLocation()) {
      MemoryRegion region = e.asAliasedLocation().getMemoryRegion();
      if(region == null) {
        region = regionMgr.makeMemoryRegion(type);
      }
      return !isSafe ? conv.makeDereference(type, e.asAliasedLocation().getAddress(), ssa, errorConditions, region) :
                       conv.makeSafeDereference(type, e.asAliasedLocation().getAddress(), ssa, region);
    } else { // Unaliased location
      return conv.makeVariable(e.asUnaliasedLocation().getVariableName(), type, ssa);
    }
  }

  /**
   * Creates a formula for the value of an expression.
   *
   * @param e The expression.
   * @param type The type of the expression.
   * @return A formula for the value.
   */
  Formula asValueFormula(final Expression e, final CType type) {
    return asValueFormula(e, type, false);
  }

  /**
   * Creates a safe formula for the value of an expression.
   *
   * @param e The expression.
   * @param type The type of the expression.
   * @return A safe formula for the value.
   */
  private Formula asSafeValueFormula(final Expression e, final CType type) {
    return asValueFormula(e, type, true);
  }

  /**
   * Should be used whenever a location corresponding to a pointer dereference is required.
   * This function properly handles the ambiguity arising from arrays vs. pointers.
   * Consider {@code int *pa, a[]; int **ppa = &pa; *pa = 5; *a = 5;}.
   * Here {@code *pa} should be encoded as <i>int (int*(ADDRESS_OF_pa))</i>, but
   * {@code *a} should be encoded as <i>int (ADDRESS_OF_a)</i> while both {@code pa} and {@code a}
   * will result in AliasedLocation with <i>ADDRESS_OF_pa</i> and <i>ADDRESS_OF_a</i> respectively.
   * So this function will add the additional dereference if necessary.
   * @param pE the source C expression form which the resulting {@code Expression} was obtained
   * @param pResult the {@code Expression} resulting from visiting the C expression {@code pE},
   *        should normally be a Location, but in case of a value the corresponding location is
   *        returned nontheless (e.g. *((int *)0) -- explicit access violation, may be used for
   *        debugging in some cases)
   * @return the result AliasedLocation of the pointed value
   */
  private AliasedLocation dereference(final CExpression pE, final Expression pResult) {
    final CType type = typeHandler.getSimplifiedType(pE);
    // Filter out composites and proper (non-funcion-argument) arrays, for them the result
    // already contains the location of the first field/element.
    if (pResult.isAliasedLocation()
        && (type instanceof CCompositeType
            || (type instanceof CArrayType
                && (!(pE instanceof CIdExpression)
                    || !(((CIdExpression) pE).getDeclaration()
                        instanceof CParameterDeclaration))))) {
      return pResult.asAliasedLocation();
    } else {
      return AliasedLocation.ofAddress(
          asValueFormula(pResult, CTypeUtils.implicitCastToPointer(type)));
    }
  }

  /**
   * Evaluates the aliased location of an array subscript expression.
   *
   * @param e The array expression.
   * @return The location of the array expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public AliasedLocation visit(final CArraySubscriptExpression e)
      throws UnrecognizedCCodeException {
    // There are two distinct kinds of arrays in C:
    // -- fixed-length arrays for which the aliased location of the first element is returned here
    // -- pointers implicitly converted to arrays for which either the aliased or unaliased location of the *pointer*
    //    is returned (arrays as function parameters also fall into this category)
    // So we use #dereference() to resolve the ambiguity
    final CExpression arrayExpression = e.getArrayExpression();
    final Expression base = dereference(arrayExpression, arrayExpression.accept(this));

    // Now we should always have the aliased location of the first array element
    assert base.isAliasedLocation() : "Not the location of the first array element";

    final CType elementType = typeHandler.getSimplifiedType(e);
    final CExpression subscript = e.getSubscriptExpression();
    final CType subscriptType = typeHandler.getSimplifiedType(subscript);
    final Formula index = conv.makeCast(subscriptType,
                                        CPointerType.POINTER_TO_VOID,
                                        asValueFormula(subscript.accept(this), subscriptType),
                                        constraints,
                                        edge);

    final Formula coeff = conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));
    final Formula baseAddress = base.asAliasedLocation().getAddress();
    final Formula address = conv.fmgr.makePlus(baseAddress, conv.fmgr.makeMultiply(coeff, index));
    addEqualBaseAddressConstraint(baseAddress, address);
    return AliasedLocation.ofAddress(address);
  }

  /**
   * Evaluates the location of the reference to a field of a composite type.
   *
   * @param e The reference to a field.
   * @return The location of the reference.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Location visit(CFieldReference e) throws UnrecognizedCCodeException {
    e = e.withExplicitPointerDereference();

    final Variable variable = e.accept(baseVisitor);
    if (variable != null) {
      final String variableName = variable.getName();
      return UnaliasedLocation.ofVariableName(variableName);
    } else {
      final CType fieldOwnerType = typeHandler.getSimplifiedType(e.getFieldOwner());
      if (fieldOwnerType instanceof CCompositeType) {
        final AliasedLocation base = e.getFieldOwner().accept(this).asAliasedLocation();

        final String fieldName = e.getFieldName();
        usedFields.add(Pair.of((CCompositeType) fieldOwnerType, fieldName));
        final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType,
                                                    typeHandler.getOffset((CCompositeType) fieldOwnerType, fieldName));

        final Formula address = conv.fmgr.makePlus(base.getAddress(), offset);
        addEqualBaseAddressConstraint(base.getAddress(), address);
        final CType fieldType = typeHandler.simplifyType(e.getExpressionType());
        final MemoryRegion region = regionMgr.makeMemoryRegion(fieldOwnerType, fieldType, fieldName);
        return AliasedLocation.ofAddressWithRegion(address, region);
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
      }
    }
  }

  /**
   * Checks, whether the given type is revealing or not.
   *
   * @param type The type to check.
   * @return Whether the given type is revealing or not.
   */
  static boolean isRevealingType(final CType type) {
    return (type instanceof CPointerType || type instanceof CArrayType) &&
           !type.equals(CPointerType.POINTER_TO_VOID);
  }

  public PointerApproximatingVisitor getPointerApproximatingVisitor() {
    return pointerApproximatingVisitorInstance;
  }

  /**
   * Evaluates the expression of a cast expression.
   *
   * @param e The C cast expression.
   * @return The expression representing the cast.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(final CCastExpression e) throws UnrecognizedCCodeException {
    final CType resultType = typeHandler.getSimplifiedType(e);
    final CExpression operand = conv.makeCastFromArrayToPointerIfNecessary(e.getOperand(), resultType);

    final Expression result = operand.accept(this);

    if (isRevealingType(resultType)) {
      operand
          .accept(getPointerApproximatingVisitor())
          .ifPresent((s) -> learnedPointerTypes.put(s, resultType));
    }

    final CType operandType = typeHandler.getSimplifiedType(operand);
    if (CTypeUtils.isSimpleType(resultType)) {
      return Value.ofValue(conv.makeCast(operandType, resultType, asValueFormula(result, operandType), constraints, edge));
    } else if (CTypes.withoutConst(resultType).equals(CTypes.withoutConst(operandType))) {
      // Special case: conversion of non-scalar type to itself is allowed (and ignored)
      // Change of const modifier is ignored, too.
      return result;
    } else {
      throw new UnrecognizedCCodeException("Conversion to non-scalar type requested", edge, e);
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
   * @param e The C id expression.
   * @return The expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(final CIdExpression e) throws UnrecognizedCCodeException {
    final CType resultType = typeHandler.getSimplifiedType(e);

    if (!pts.isActualBase(e.getDeclaration().getQualifiedName())
        && !CTypeUtils.containsArray(resultType, e.getDeclaration())) {
      Variable variable = Variable.create(e.getDeclaration().getQualifiedName(), resultType);
      if (!(e.getDeclaration() instanceof CFunctionDeclaration)) {
        final String variableName = variable.getName();
        return UnaliasedLocation.ofVariableName(variableName);
      } else {
        return Value.ofValue(conv.makeConstant(variable.getName(), variable.getType()));
      }
    } else {
      final Formula address = conv.makeConstant(PointerTargetSet.getBaseName(e.getDeclaration().getQualifiedName()),
                                                CTypeUtils.getBaseType(resultType));
      return AliasedLocation.ofAddress(address);
    }
  }

  /**
   * Evaluates the value of an unary expression in C.
   *
   * @param e The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    if (e.getOperator() == UnaryOperator.AMPER) {
      final CExpression operand = e.getOperand();

      final Variable baseVariable = operand.accept(baseVisitor);
      // Whether the addressed location was previously aliased (tracked with UFs)
      // If it was, there was no base variable/prefix used to hold its value and we simply return the
      // aliased location
      // Otherwise, we should make it aliased by importing the value into the UF
      // There is an exception, though: arrays in function parameters are tracked as variables (unaliased locations),
      // because they are actually pointers and can be assigned (in function calls)
      // See also see § 6.7.5.3 (7) of the C99 standard
      // But here they should be treated as if they are normal arrays and e.g. &a for int a[] should have the
      // same semantics as &a[0] rather than the address of the pointer variable
      // (imagine &a for int *a parameter)
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
        final List<Pair<CCompositeType, String>> alreadyUsedFields = ImmutableList.copyOf(usedFields);
        usedFields.clear();

        if (errorConditions.isEnabled() && operand instanceof CFieldReference) {
          // for &(s->f) and &((*s).f) do special case because the pointer is
          // not actually dereferenced and thus we don't want to add error conditions
          // for invalid-deref
          final CFieldReference field = (CFieldReference) operand;
          CExpression fieldOwner = field.getFieldOwner();
          boolean isDeref = field.isPointerDereference();
          if (!isDeref && fieldOwner instanceof CPointerExpression) {
            fieldOwner = ((CPointerExpression)fieldOwner).getOperand();
            isDeref = true;
          }
          if (isDeref) {
            final CPointerType pointerType =
                (CPointerType) typeHandler.getSimplifiedType(fieldOwner);
            final Formula base = asSafeValueFormula(fieldOwner.accept(this), pointerType);
            final String fieldName = field.getFieldName();
            final CCompositeType compositeType =
                (CCompositeType) CTypeUtils.checkIsSimplified(pointerType.getType());
            usedFields.add(Pair.of(compositeType, fieldName));
            final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType,
                                                        typeHandler.getOffset(compositeType, fieldName));
            addressExpression = AliasedLocation.ofAddress(conv.fmgr.makePlus(base, offset));
            addEqualBaseAddressConstraint(base, addressExpression.getAddress());
          }
        }

        if (addressExpression == null) {
          addressExpression = operand.accept(this).asAliasedLocation();
        }

        addressedFields.addAll(usedFields);
        usedFields.addAll(alreadyUsedFields);

        return Value.ofValue(addressExpression.getAddress());
      } else if (operand instanceof CIdExpression
          && typeHandler.simplifyType(operand.getExpressionType()) instanceof CArrayType
          && ((CIdExpression) operand).getDeclaration() instanceof CParameterDeclaration) {
        return Value.ofValue(dereference(operand, operand.accept(this)).getAddress());
      } else {
        final Variable base = baseVisitor.getLastBase();
        final Formula baseAddress = conv.makeConstant(PointerTargetSet.getBaseName(base.getName()),
                                                      CTypeUtils.getBaseType(base.getType()));
        conv.addValueImportConstraints(edge,
                                       baseAddress,
                                       base,
                                       initializedFields,
                                       ssa,
                                       constraints,
                                       null);
        if (conv.hasIndex(base.getName(), base.getType(), ssa)) {
          ssa.deleteVariable(base.getName());
        }
        conv.addPreFilledBase(base.getName(),
                              base.getType(),
                              pts.isPreparedBase(base.getName()),
                              false,
                              constraints,
                              pts);
        return visit(e);
      }
    } else {
      return visitDefault(e);
    }
  }

  /**
   * Evaluates the aliased location of a pointer expression.
   *
   * @param e The C pointer expression.
   * @return The aliased location of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public AliasedLocation visit(final CPointerExpression e) throws UnrecognizedCCodeException {
    // Dereferencing a stand-alone array leaves the result of visiting the operand unchanged
    // Other cases should trigger additional dereference, so we use
    // #dereference() to resolve the ambiguity
    final CExpression operand = e.getOperand();
    return dereference(operand, operand.accept(this));
  }

  /**
   * Evaluates the value of a binary expression.
   *
   * @param exp The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CBinaryExpression exp) throws UnrecognizedCCodeException {
    final CType returnType = exp.getExpressionType();
    final CType calculationType = exp.getCalculationType();
    final Formula f1 = delegate.processOperand(exp.getOperand1(), calculationType, returnType);
    final Formula f2 = delegate.processOperand(exp.getOperand2(), calculationType, returnType);

    final Formula result = delegate.handleBinaryExpression(exp, f1, f2);

    final CType t1 = typeHandler.getSimplifiedType(exp.getOperand1());
    final CType t2 = typeHandler.getSimplifiedType(exp.getOperand2());

    if (t1.equals(CPointerType.POINTER_TO_VOID) || t2.equals(CPointerType.POINTER_TO_VOID)) {
      if (isRevealingType(t1)) {
        exp.getOperand2()
            .accept(getPointerApproximatingVisitor())
            .ifPresent((s) -> learnedPointerTypes.put(s, t1));
      } else if (isRevealingType(t2)) {
        exp.getOperand1()
            .accept(getPointerApproximatingVisitor())
            .ifPresent((s) -> learnedPointerTypes.put(s, t2));
      }
    }

    final BinaryOperator op = exp.getOperator();

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
   * @param e The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  protected Value visitDefault(final CExpression e) throws UnrecognizedCCodeException {
    return Value.ofValue(e.accept(delegate));
  }

  /**
   * Evaluates the return value of a function call expression.
   *
   * @param e The function call expression.
   * @return The value of the expression.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();

    // First let's handle special cases such as allocations
    if (functionNameExpression instanceof CIdExpression) {
      final String functionName = ((CIdExpression)functionNameExpression).getName();

      if (conv.options.isDynamicMemoryFunction(functionName)) {
        DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
        try {
          return memoryHandler.handleDynamicMemoryFunction(e, functionName, this);
        } catch (InterruptedException exc) {
          throw CtoFormulaConverter.propagateInterruptedException(exc);
        }
      }
    }

    // Pure functions returning composites are unsupported, return a nondet value
    final CType resultType = conv.getReturnType(e, edge);
    if (resultType instanceof CCompositeType
        || CTypeUtils.containsArrayOutsideFunctionParameter(resultType)) {
      conv.logger.logfOnce(Level.WARNING,
                           "Extern function %s returning a composite is treated as nondet.", e);
      return Value.nondetValue();
    }

    // Delegate
    return Value.ofValue(delegate.visit(e));
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
  Map<String, CType> getLearnedPointerTypes() {
    return Collections.unmodifiableMap(learnedPointerTypes);
  }

  class PointerApproximatingVisitor
      extends DefaultCExpressionVisitor<Optional<String>, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Optional<String>, UnrecognizedCCodeException> {

    private PointerApproximatingVisitor() {}

    @Override
    public Optional<String> visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
      return e.getArrayExpression().accept(this);
    }

    @Override
    public Optional<String> visit(CBinaryExpression e) throws UnrecognizedCCodeException {
      final CType t = typeHandler.getSimplifiedType(e);
      if (t instanceof CPointerType || t instanceof CArrayType) {
        return e.getOperand1().accept(this);
      }
      return Optional.empty();
    }

    @Override
    public Optional<String> visit(CCastExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    public Optional<String> visit(final CFieldReference e) throws UnrecognizedCCodeException {
      CType t = typeHandler.getSimplifiedType(e.withExplicitPointerDereference().getFieldOwner());
      if (t instanceof CCompositeType) {
        return Optional.of(
            ((CCompositeType) t).getQualifiedName()
                + CToFormulaConverterWithPointerAliasing.FIELD_NAME_SEPARATOR
                + e.getFieldName());
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
      }
    }

    @Override
    public Optional<String> visit(CIdExpression e) throws UnrecognizedCCodeException {
      return Optional.of(e.getDeclaration().getQualifiedName());
    }

    @Override
    public Optional<String> visit(CPointerExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    public Optional<String> visit(CUnaryExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    protected Optional<String> visitDefault(CExpression pExp) throws RuntimeException {
      return Optional.empty();
    }

    @Override
    public Optional<String> visit(CFunctionCallExpression call) throws UnrecognizedCCodeException {
      return Optional.empty();
    }
  }

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final PointerTargetSetBuilder pts;
  private final MemoryRegionManager regionMgr;

  private final BaseVisitor baseVisitor;
  private final ExpressionToFormulaVisitor delegate;

  private final PointerApproximatingVisitor pointerApproximatingVisitorInstance =
      new PointerApproximatingVisitor();

  private final List<Pair<CCompositeType, String>> usedFields = new ArrayList<>(1);
  private final List<Pair<CCompositeType, String>> initializedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> addressedFields = new ArrayList<>();
  private final Map<String, CType> learnedPointerTypes = Maps.newHashMapWithExpectedSize(1);
}
