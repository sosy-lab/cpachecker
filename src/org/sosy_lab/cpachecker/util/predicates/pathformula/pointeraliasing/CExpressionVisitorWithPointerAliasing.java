// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.OverflowAssumptionManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/** A visitor the handle C expressions with the support for pointer aliasing. */
class CExpressionVisitorWithPointerAliasing
    extends DefaultCExpressionVisitor<Expression, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Expression, UnrecognizedCodeException> {

  /** A simple expression to formula visitor. */
  private static class AdaptingExpressionToFormulaVisitor
      extends AdaptingCExpressionVisitor<Formula, Expression, UnrecognizedCodeException>
      implements CRightHandSideVisitor<Formula, UnrecognizedCodeException> {

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
     * @throws UnrecognizedCodeException If the C code was unrecognizable.
     */
    @Override
    protected Formula convert(Expression value, CExpression rhs) throws UnrecognizedCodeException {
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
     * @throws UnrecognizedCodeException If the C code was unrecognizable.
     */
    @Override
    public Formula visit(CFunctionCallExpression e) throws UnrecognizedCodeException {
      return convert0(((CExpressionVisitorWithPointerAliasing) delegate).visit(e), e);
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
  CExpressionVisitorWithPointerAliasing(
      final CToFormulaConverterWithPointerAliasing cToFormulaConverter,
      final CFAEdge cfaEdge,
      final String function,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions,
      final PointerTargetSetBuilder pts,
      final MemoryRegionManager regionMgr) {

    delegate =
        new ExpressionToFormulaVisitor(
            cToFormulaConverter, cToFormulaConverter.fmgr, cfaEdge, function, ssa, constraints) {
          @Override
          protected Formula toFormula(CExpression e) throws UnrecognizedCodeException {
            // recursive application of pointer-aliasing.
            return asValueFormula(
                e.accept(CExpressionVisitorWithPointerAliasing.this),
                typeHandler.getSimplifiedType(e));
          }
        };

    conv = cToFormulaConverter;
    typeHandler = cToFormulaConverter.typeHandler;
    ofmgr = new OverflowAssumptionManager(conv.machineModel, conv.logger);
    edge = cfaEdge;
    this.ssa = ssa;
    this.constraints = constraints;
    this.errorConditions = errorConditions;
    this.pts = pts;
    this.regionMgr = regionMgr;
    this.function = function;
  }

  /**
   * Returns a formula visitor for right hand side expressions.
   *
   * @return A formula visitor for right hand side expressions.
   */
  CRightHandSideVisitor<Formula, UnrecognizedCodeException> asFormulaVisitor() {
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
      constraints.addConstraint(
          conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(p1), conv.makeBaseAddressOfTerm(p2)));
    }
  }

  /**
   * Creates a formula for the value of an expression.
   *
   * @param e The expression.
   * @param type The type of the expression.
   * @param isSafe A flag, if the formula is safe or not.
   * @return A formula for the value.
   */
  private Formula asValueFormula(final Expression e, final CType type, final boolean isSafe) {
    if (e.isNondetValue()) {
      // should happen only because of bit fields that we currently do not handle
      String nondetName = "__nondet_value_" + CTypeUtils.typeToString(type).replace(' ', '_');
      return conv.makeNondet(nondetName, type, ssa, constraints);
    } else if (e.isValue()) {
      return e.asValue().getValue();
    } else if (e.isAliasedLocation()) {
      MemoryRegion region = e.asAliasedLocation().getMemoryRegion();
      if (region == null) {
        region = regionMgr.makeMemoryRegion(type);
      }
      return !isSafe
          ? conv.makeDereference(
              type, e.asAliasedLocation().getAddress(), ssa, errorConditions, region)
          : conv.makeSafeDereference(type, e.asAliasedLocation().getAddress(), ssa, region);
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
   * Should be used whenever a location corresponding to a pointer dereference is required. This
   * function properly handles the ambiguity arising from arrays vs. pointers. Consider {@code int
   * *pa, a[]; int **ppa = & pa; *pa = 5; *a = 5;}. Here {@code *pa} should be encoded as <i>int
   * (int*(ADDRESS_OF_pa))</i>, but {@code *a} should be encoded as <i>int (ADDRESS_OF_a)</i> while
   * both {@code pa} and {@code a} will result in AliasedLocation with <i>ADDRESS_OF_pa</i> and
   * <i>ADDRESS_OF_a</i> respectively. So this function will add the additional dereference if
   * necessary.
   *
   * @param pE the source C expression form which the resulting {@code Expression} was obtained
   * @param pResult the {@code Expression} resulting from visiting the C expression {@code pE},
   *     should normally be a Location, but in case of a value the corresponding location is
   *     returned nontheless (e.g. *((int *)0) -- explicit access violation, may be used for
   *     debugging in some cases)
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public AliasedLocation visit(final CArraySubscriptExpression e) throws UnrecognizedCodeException {
    // There are two distinct kinds of arrays in C:
    // -- fixed-length arrays for which the aliased location of the first element is returned here
    // -- pointers implicitly converted to arrays for which either the aliased or unaliased location
    // of the *pointer*
    //    is returned (arrays as function parameters also fall into this category)
    // So we use #dereference() to resolve the ambiguity
    final CExpression arrayExpression = e.getArrayExpression();
    final Expression base = dereference(arrayExpression, arrayExpression.accept(this));

    // Now we should always have the aliased location of the first array element
    assert base.isAliasedLocation() : "Not the location of the first array element";

    final CType elementType = typeHandler.getSimplifiedType(e);
    final CExpression subscript = e.getSubscriptExpression();
    final CType subscriptType = typeHandler.getSimplifiedType(subscript);
    final Formula index =
        conv.makeCast(
            subscriptType,
            CPointerType.POINTER_TO_VOID,
            asValueFormula(subscript.accept(this), subscriptType),
            constraints,
            edge);

    final Formula coeff =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(CFieldReference e) throws UnrecognizedCodeException {
    e = e.withExplicitPointerDereference();

    BaseVisitor baseVisitor = new BaseVisitor(edge, pts, typeHandler);
    final Variable variable = e.accept(baseVisitor);
    if (variable != null) {
      final String variableName = variable.getName();
      return UnaliasedLocation.ofVariableName(variableName);
    } else {
      final CType fieldOwnerType = typeHandler.getSimplifiedType(e.getFieldOwner());
      if (fieldOwnerType instanceof CCompositeType) {
        final AliasedLocation base = e.getFieldOwner().accept(this).asAliasedLocation();

        final String fieldName = e.getFieldName();
        usedFields.add(CompositeField.of((CCompositeType) fieldOwnerType, fieldName));
        final OptionalLong fieldOffset =
            typeHandler.getOffset((CCompositeType) fieldOwnerType, fieldName);
        if (!fieldOffset.isPresent()) {
          // TODO This looses values of bit fields.
          // If fixed remove the condition in asValueFormula and AssignmentHandler.handleAssignment
          return Value.nondetValue();
        }
        final Formula offset =
            conv.fmgr.makeNumber(conv.voidPointerFormulaType, fieldOffset.orElseThrow());
        final Formula address = conv.fmgr.makePlus(base.getAddress(), offset);
        addEqualBaseAddressConstraint(base.getAddress(), address);
        final CType fieldType = typeHandler.simplifyType(e.getExpressionType());
        final MemoryRegion region =
            regionMgr.makeMemoryRegion(fieldOwnerType, fieldType, fieldName);
        return AliasedLocation.ofAddressWithRegion(address, region);
      } else {
        throw new UnrecognizedCodeException("Field owner of a non-composite type", edge, e);
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
    return (type instanceof CPointerType || type instanceof CArrayType)
        && !type.equals(CPointerType.POINTER_TO_VOID);
  }

  private PointerApproximatingVisitor getPointerApproximatingVisitor() {
    return new PointerApproximatingVisitor(typeHandler, edge);
  }

  /**
   * Evaluates the expression of a cast expression.
   *
   * @param e The C cast expression.
   * @return The expression representing the cast.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(final CCastExpression e) throws UnrecognizedCodeException {
    final CType resultType = typeHandler.getSimplifiedType(e);
    final CExpression operand =
        conv.makeCastFromArrayToPointerIfNecessary(e.getOperand(), resultType);

    final Expression result = operand.accept(this);

    if (isRevealingType(resultType)) {
      operand
          .accept(getPointerApproximatingVisitor())
          .ifPresent((s) -> learnedPointerTypes.put(s, resultType));
    }

    final CType operandType = typeHandler.getSimplifiedType(operand);
    if (CTypeUtils.isSimpleType(resultType)) {
      return Value.ofValue(
          conv.makeCast(
              operandType, resultType, asValueFormula(result, operandType), constraints, edge));
    } else if (CTypes.withoutConst(resultType).equals(CTypes.withoutConst(operandType))) {
      // Special case: conversion of non-scalar type to itself is allowed (and ignored)
      // Change of const modifier is ignored, too.
      return result;
    } else {
      throw new UnrecognizedCodeException("Conversion to non-scalar type requested", edge, e);
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public Expression visit(final CIdExpression e) throws UnrecognizedCodeException {
    final CType resultType = typeHandler.getSimplifiedType(e);

    final String variableName = e.getDeclaration().getQualifiedName();
    if (!pts.isActualBase(variableName)
        && !CTypeUtils.containsArray(resultType, e.getDeclaration())) {
      if (!(e.getDeclaration() instanceof CFunctionDeclaration)) {
        return UnaliasedLocation.ofVariableName(variableName);
      } else {
        return Value.ofValue(conv.makeConstant(variableName, resultType));
      }
    } else {
      final Formula address = conv.makeBaseAddress(variableName, resultType);
      return AliasedLocation.ofAddress(address);
    }
  }

  /**
   * Evaluates the value of an unary expression in C.
   *
   * @param e The C expression.
   * @return The value of the expression.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CUnaryExpression e) throws UnrecognizedCodeException {
    if (e.getOperator() == UnaryOperator.AMPER) {
      final CExpression operand = e.getOperand();

      BaseVisitor baseVisitor = new BaseVisitor(edge, pts, typeHandler);
      final Variable baseVariable = operand.accept(baseVisitor);
      // Whether the addressed location was previously aliased (tracked with UFs)
      // If it was, there was no base variable/prefix used to hold its value and we simply return
      // the
      // aliased location
      // Otherwise, we should make it aliased by importing the value into the UF
      // There is an exception, though: arrays in function parameters are tracked as variables
      // (unaliased locations),
      // because they are actually pointers and can be assigned (in function calls)
      // See also see § 6.7.5.3 (7) of the C99 standard
      // But here they should be treated as if they are normal arrays and e.g. &a for int a[] should
      // have the
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
        // it's known that their values remain undefined and only some other outer structure field
        // is assigned.
        // But in
        // p2 = &(s2.ss1);
        // p2->f1 = 0;
        // the fields f1 and f2 along with the field s{1,2}.ss1 should be tracked from the first
        // line onwards, because
        // it's too hard to determine (without the help of some alias analysis)
        // whether f1 assigned in the second line is an outer or inner structure field.
        final List<CompositeField> alreadyUsedFields = ImmutableList.copyOf(usedFields);
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
                (CPointerType) typeHandler.getSimplifiedType(fieldOwner);
            final Formula base = asSafeValueFormula(fieldOwner.accept(this), pointerType);
            final String fieldName = field.getFieldName();
            final CCompositeType compositeType =
                (CCompositeType) CTypeUtils.checkIsSimplified(pointerType.getType());
            usedFields.add(CompositeField.of(compositeType, fieldName));
            final long fieldOffset =
                typeHandler
                    .getOffset(compositeType, fieldName)
                    .orElseThrow(
                        () ->
                            new UnrecognizedCodeException(
                                "Taking address of bit fields is not allowed", e));
            final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType, fieldOffset);
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
        final Formula baseAddress = conv.makeBaseAddress(base.getName(), base.getType());
        conv.addValueImportConstraints(
            baseAddress, base.getName(), base.getType(), initializedFields, ssa, constraints, null);
        if (pts.isPreparedBase(base.getName())) {
          pts.shareBase(base.getName(), base.getType());
        } else {
          Formula size =
              conv.fmgr.makeNumber(
                  conv.voidPointerFormulaType, typeHandler.getSizeof(base.getType()));
          pts.addBase(base.getName(), base.getType(), size, constraints);
        }
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public AliasedLocation visit(final CPointerExpression e) throws UnrecognizedCodeException {
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CBinaryExpression exp) throws UnrecognizedCodeException {
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  protected Value visitDefault(final CExpression e) throws UnrecognizedCodeException {
    return Value.ofValue(e.accept(delegate));
  }

  /**
   * Evaluates the return value of a function call expression.
   *
   * @param e The function call expression.
   * @return The value of the expression.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Override
  public Value visit(final CFunctionCallExpression e) throws UnrecognizedCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();

    // First let's handle special cases such as allocations
    if (functionNameExpression instanceof CIdExpression) {
      final String functionName = ((CIdExpression) functionNameExpression).getName();

      if (conv.options.isDynamicMemoryFunction(functionName)) {
        DynamicMemoryHandler memoryHandler =
            new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
        try {
          return memoryHandler.handleDynamicMemoryFunction(e, functionName, this);
        } catch (InterruptedException exc) {
          throw CtoFormulaConverter.propagateInterruptedException(exc);
        }
      }

      // modf, modff, and modfl raise a side-effect by writing
      // the integral part of their first parameter into the
      // pointer-address given as the second parameter,
      // which is handled here
      if (BuiltinFloatFunctions.matchesModf(functionName)) {
        final CType returnType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
        final List<CExpression> parameters = e.getParameterExpressions();

        final String trunc = BuiltinFloatFunctions.getAppropriateTruncName(returnType);
        final FileLocation dummy = FileLocation.DUMMY;

        CLeftHandSide lhs = new CPointerExpression(dummy, returnType, parameters.get(1));

        CFunctionDeclaration functionDecl =
            new CFunctionDeclaration(
                dummy,
                CFunctionType.functionTypeWithReturnType(returnType),
                trunc,
                ImmutableList.of(
                    new CParameterDeclaration(dummy, returnType, "irrelevant_parameter_name")),
                ImmutableSet.of());

        CRightHandSide rhs =
            new CFunctionCallExpression(
                dummy,
                returnType,
                new CIdExpression(dummy, functionDecl),
                Collections.singletonList(parameters.get(0)),
                functionDecl);

        BooleanFormula form = null;
        try {
          form =
              conv.makeAssignment(
                  lhs, lhs, rhs, edge, function, ssa, pts, constraints, errorConditions);
        } catch (InterruptedException e1) {
          CtoFormulaConverter.propagateInterruptedException(e1);
        }

        constraints.addConstraint(checkNotNull(form));
      }

      // check strlen up to specific index maxIndex and return nondet otherwise
      if (BuiltinFunctions.matchesStrlen(functionName)) {
        // This is not an off-by-one error, we can set maxIndex to the maximal size because of the
        // terminating 0 of a string.
        final int maxIndex = conv.options.maxPreciseStrFunctionSize();
        List<CExpression> parameters = e.getParameterExpressions();
        assert parameters.size() == 1;
        CExpression parameter = parameters.get(0);
        final CType returnType = e.getExpressionType();

        Formula f = conv.makeNondet(functionName, returnType, ssa, constraints);
        // for maxIndex=1, after the loop f has the form
        // parameter[0]==0?0:(parameter[1]==0?1:nondet())
        for (long i = maxIndex; i >= 0; i--) {
          f = stringEndsAtIndexOrOtherwise(parameter, i, f, returnType);
        }
        return Value.ofValue(f);

      } else if (functionName.equals("memcmp")
          || functionName.equals("strcmp")
          || functionName.equals("strncmp")) {
        return handleCmpFunction(functionName, e);
      }

      if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(functionName)) {
        List<CExpression> parameters = e.getParameterExpressions();
        assert parameters.size() == 3;
        CExpression var1 = parameters.get(0);
        CExpression var2 = parameters.get(1);
        CExpression var3 = parameters.get(2);
        Expression overflows =
            BuiltinOverflowFunctions.handleOverflow(ofmgr, var1, var2, var3, functionName)
                .accept(this);
        Formula f = asValueFormula(overflows, CNumericTypes.BOOL);

        if (!BuiltinOverflowFunctions.isFunctionWithoutSideEffect(functionName)) {
          CLeftHandSide lhs =
              new CPointerExpression(
                  FileLocation.DUMMY, ((CPointerType) var3.getExpressionType()).getType(), var3);
          CRightHandSide rhs =
              BuiltinOverflowFunctions.handleOverflowSideeffects(
                  ofmgr, var1, var2, var3, functionName);

          BooleanFormula form = null;
          try {
            form =
                conv.makeAssignment(
                    lhs, lhs, rhs, edge, function, ssa, pts, constraints, errorConditions);
          } catch (InterruptedException e1) {
            CtoFormulaConverter.propagateInterruptedException(e1);
          }

          constraints.addConstraint(checkNotNull(form));
        }
        return Value.ofValue(f);
      }
    }

    // Pure functions returning composites are unsupported, return a nondet value
    final CType resultType = conv.getReturnType(e, edge);
    if (resultType instanceof CCompositeType
        || CTypeUtils.containsArrayOutsideFunctionParameter(resultType)) {
      conv.logger.logfOnce(
          Level.WARNING, "Extern function %s returning a composite is treated as nondet.", e);
      return Value.nondetValue();
    }

    // Delegate
    return Value.ofValue(delegate.visit(e));
  }

  /** This method provides approximations of the builtin functions memcmp, strcmp, and strncmp. */
  private Value handleCmpFunction(final String functionName, final CFunctionCallExpression e)
      throws UnrecognizedCodeException {
    final List<CExpression> parameters = e.getParameterExpressions();
    assert parameters.size() == 2 || parameters.size() == 3;
    final CExpression s1 = parameters.get(0);
    final CExpression s2 = parameters.get(1);
    final CSimpleType returnType = (CSimpleType) e.getExpressionType().getCanonicalType();

    // These functions treat all chars in s1 and s2 as unsigned.
    final boolean signed = false;
    final CSimpleType uChar = CNumericTypes.UNSIGNED_CHAR;

    // Prepare for variants (with bounds check, with string-end check, or both)
    final boolean checkStringEnd = functionName.startsWith("str");
    final boolean hasBounds = parameters.size() == 3;
    final CType sizeType;
    Formula sizeFormula;
    long maxIndex = conv.options.maxPreciseStrFunctionSize();
    if (hasBounds) {
      CExpression size = parameters.get(2);
      if (size instanceof CIntegerLiteralExpression) {
        maxIndex = Math.min(maxIndex, ((CIntegerLiteralExpression) size).asLong());
      }
      sizeType = e.getDeclaration().getParameters().get(2).getType().getCanonicalType();
      sizeFormula = asValueFormula(size.accept(this), sizeType);
      sizeFormula =
          conv.makeCast(size.getExpressionType(), sizeType, sizeFormula, constraints, edge);
    } else {
      sizeType = conv.machineModel.getPointerEquivalentSimpleType(); // should be size_t actually
      sizeFormula = null;
    }
    final Formula stringTerminator =
        delegate.visit(CIntegerLiteralExpression.createDummyLiteral(0, uChar));

    // As result, we use a nondeterministic value. Furthermore, we add constraints that force this
    // value to be >0 or <0 depending on whether s1>s2 or s1<s2 (and implicitly, force the value to
    // be 0 if s1==s2.
    // These constraints have recursive form and simulate "if comparison at first char determines
    // the result, then ... else (if comparison at second char determines the result, then ... else
    // ...)". Furthermore, additional clauses at each recursive step check for the string end
    // (in case of strcmp/strncmp) or "cut off" the chain if the bound is reached (in case of
    // strncmp/memcmp).
    // Creating these constraints is done starting with the inner-most term (with highest index).
    // We also need a base case for the recursive constraints, and this needs to make our bounded
    // approximation sound: if the strings are longer than our approximation bound and equal up to
    // the approximation bound, we need to make both constraints have nondeterministic value.
    // We do this by initializing them with resultIsPos and resultIsNeg, respectively, so there is
    // a cyclic condition and the solver can choose nondeterministic values for
    // resultIsPos/resultIsNeg as long as the other constraints do not force a value.
    // Note that our approximation bound and the strncmp/memcmp bound are different, but we can
    // simply use the minimum of both (which is maxIndex).

    final Formula result = conv.makeNondet(functionName, returnType, ssa, constraints);
    final Formula zero =
        delegate.visit(CIntegerLiteralExpression.createDummyLiteral(0, returnType));
    final BooleanFormula resultIsPos = conv.fmgr.makeGreaterThan(result, zero, true);
    final BooleanFormula resultIsNeg = conv.fmgr.makeLessThan(result, zero, true);

    BooleanFormula isGreater = resultIsPos; // constraint for s1 > s2
    BooleanFormula isLess = resultIsNeg; // constraint for s1 < s2

    for (long index = maxIndex; index >= 0; index--) {
      CIntegerLiteralExpression indexLiteral =
          CIntegerLiteralExpression.createDummyLiteral(index, sizeType);

      CArraySubscriptExpression s1AtIndexExp =
          new CArraySubscriptExpression(FileLocation.DUMMY, uChar, s1, indexLiteral);
      CArraySubscriptExpression s2AtIndexExp =
          new CArraySubscriptExpression(FileLocation.DUMMY, uChar, s2, indexLiteral);
      Formula s1AtIndex = asValueFormula(this.visit(s1AtIndexExp), uChar);
      Formula s2AtIndex = asValueFormula(this.visit(s2AtIndexExp), uChar);

      BooleanFormula isEqualAtIndex = conv.fmgr.makeEqual(s1AtIndex, s2AtIndex);
      BooleanFormula isLessAtIndex = conv.fmgr.makeLessThan(s1AtIndex, s2AtIndex, signed);
      BooleanFormula isGreaterAtIndex = conv.fmgr.makeGreaterThan(s1AtIndex, s2AtIndex, signed);

      if (checkStringEnd) {
        // Equality is only relevant if not at string end.
        // Because this check becomes part of the recursive conjunctions below,
        // it also "cuts off" any comparisons beyond the string end.
        BooleanFormula isStringEnd = conv.fmgr.makeEqual(s1AtIndex, stringTerminator);
        // need to check only s1 because it gets conjuncted with s1AtIndex==s2AtIndex
        isEqualAtIndex = conv.bfmgr.and(isEqualAtIndex, conv.bfmgr.not(isStringEnd));
      }

      isLess = conv.bfmgr.or(isLessAtIndex, conv.bfmgr.and(isEqualAtIndex, isLess));
      isGreater = conv.bfmgr.or(isGreaterAtIndex, conv.bfmgr.and(isEqualAtIndex, isGreater));

      if (hasBounds) {
        // Comparisons are only relevant if not beyond bound.
        Formula indexFormula = asValueFormula(this.visit(indexLiteral), sizeType);
        BooleanFormula boundNotReached = conv.fmgr.makeLessThan(indexFormula, sizeFormula, false);
        isLess = conv.bfmgr.and(isLess, boundNotReached);
        isGreater = conv.bfmgr.and(isGreater, boundNotReached);
      }
    }

    constraints.addConstraint(conv.bfmgr.equivalence(resultIsPos, isGreater));
    constraints.addConstraint(conv.bfmgr.equivalence(resultIsNeg, isLess));

    return Value.ofValue(result);
  }

  /**
   * This method is used to approximate the built-in function strlen.
   *
   * @param parameter the string to be checked
   * @param index the index to be checked
   * @param otherwise a formula returned in case of a negative match
   * @param returnType the type of the index returned on positive match (strlen return type)
   * @return the formula parameter[index]==0?index:otherwise
   */
  private Formula stringEndsAtIndexOrOtherwise(
      CExpression parameter, Long index, Formula otherwise, CType returnType)
      throws UnrecognizedCodeException {
    AliasedLocation parameterAtIndex =
        this.visit(
            new CArraySubscriptExpression(
                FileLocation.DUMMY,
                CNumericTypes.CHAR,
                parameter,
                CIntegerLiteralExpression.createDummyLiteral(index, CNumericTypes.UNSIGNED_INT)));
    Formula nullTerminator =
        delegate.visit(CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.CHAR));
    BooleanFormula condition =
        conv.fmgr.makeEqual(asValueFormula(parameterAtIndex, CNumericTypes.CHAR), nullTerminator);
    return conv.bfmgr.ifThenElse(
        condition,
        conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(returnType), index),
        otherwise);
  }

  /**
   * Returns a list of the used fields of composite types.
   *
   * @return A list of the used fields.
   */
  List<CompositeField> getUsedFields() {
    return Collections.unmodifiableList(usedFields);
  }

  /**
   * Returns a list of the initialized fields of composite types.
   *
   * @return A list of the initialized fields.
   */
  List<CompositeField> getInitializedFields() {
    return Collections.unmodifiableList(initializedFields);
  }

  /**
   * Returns a list of the addressed fields of composite types.
   *
   * @return A list of the addressed fields.
   */
  List<CompositeField> getAddressedFields() {
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

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final OverflowAssumptionManager ofmgr;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final PointerTargetSetBuilder pts;
  private final MemoryRegionManager regionMgr;
  private String function;

  private final ExpressionToFormulaVisitor delegate;

  private final List<CompositeField> usedFields = new ArrayList<>(1);
  private final List<CompositeField> initializedFields = new ArrayList<>();
  private final List<CompositeField> addressedFields = new ArrayList<>();
  private final Map<String, CType> learnedPointerTypes = Maps.newHashMapWithExpectedSize(1);
}
