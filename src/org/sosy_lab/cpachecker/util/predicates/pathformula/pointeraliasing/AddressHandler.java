// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.Optional;
import java.util.OptionalLong;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Handles common address-related operations on expressions, e.g., getting the value represented by
 * an {@code Expression}, dereferencing an {@code Expression}, and applying offsets an to {@code
 * AliasedLocation}.
 */
final class AddressHandler {

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final SSAMapBuilder ssa;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final MemoryRegionManager regionMgr;

  AddressHandler(
      CToFormulaConverterWithPointerAliasing pConv,
      SSAMapBuilder pSsa,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
      MemoryRegionManager pRegionMgr) {
    conv = pConv;

    typeHandler = pConv.typeHandler;

    ssa = pSsa;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    regionMgr = pRegionMgr;
  }

  /**
   * Applies offset corresponding to a composite type field to a base aliased location using pointer
   * arithmetic. No dereferencing is needed as composite type aliased location adresses always point
   * at the start of the composite type.
   *
   * @param base The base aliased location.
   * @param field The composite field to apply offset of.
   * @return An expression with applied field offset. Normally an aliased location, but may also be
   *     nondet value if the offset could not be applied.
   */
  Expression applyFieldOffset(final AliasedLocation base, final CompositeField field) {

    final CType fieldOwnerType = typeHandler.simplifyType(field.getOwnerType());
    final String fieldName = field.getFieldName();
    final CType fieldType = typeHandler.getSimplifiedType(field.getFieldDeclaration());

    final OptionalLong fieldOffset = typeHandler.getOffset(field.getOwnerType(), fieldName);
    if (!fieldOffset.isPresent()) {
      // TODO: This loses values of bit fields
      return Value.nondetValue();
    }
    final Formula offset =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, fieldOffset.orElseThrow());
    final Formula address = conv.fmgr.makePlus(base.getAddress(), offset);
    addEqualBaseAddressConstraint(base.getAddress(), address);
    final MemoryRegion region = regionMgr.makeMemoryRegion(fieldOwnerType, fieldType, fieldName);
    return AliasedLocation.ofAddressWithRegion(address, region);
  }

  /**
   * Dereferences base and applies subscript offset using pointer arithmetic.
   *
   * @see #applyDereference(CType, Expression, boolean)
   * @param baseType Type of the base, used for dereferencing.
   * @param base Not-yet-dereferenced base.
   * @param directAddress If true and the base is an aliased location, does not dereference it
   *     further.
   * @param elementType Element type.
   * @param subscript Subscript formula.
   * @return An aliased location with the applied subscript offset.
   */
  AliasedLocation applySubscriptOffset(
      final CType baseType,
      final Expression base,
      final boolean directAddress,
      final CType elementType,
      final Formula subscript) {
    AliasedLocation dereferencedBase = applyDereference(baseType, base, directAddress);
    return applySubscriptOffsetToDereferencedBase(dereferencedBase, elementType, subscript);
  }

  /**
   * Applies subscript offset to already dereferenced aliased location using pointer arithmetic.
   *
   * @param dereferencedBase Already dereferenced base.
   * @param elementType Element type.
   * @param subscript Subscript formula.
   * @return An aliased location with the applied subscript offset.
   */
  AliasedLocation applySubscriptOffsetToDereferencedBase(
      final AliasedLocation dereferencedBase, final CType elementType, final Formula subscript) {

    // perform pointer arithmetic, we have array[base] and want array[base + i]
    // the subscript must be multiplied by the sizeof the element type
    final Formula dereferencedBaseAddress = dereferencedBase.getAddress();
    final Formula sizeOfElement =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));
    final Formula adjustedAddress =
        conv.fmgr.makePlus(
            dereferencedBaseAddress, conv.fmgr.makeMultiply(sizeOfElement, subscript));
    addEqualBaseAddressConstraint(dereferencedBaseAddress, adjustedAddress);
    return AliasedLocation.ofAddress(adjustedAddress);
  }

  /**
   * Used whenever a location corresponding to a pointer dereference is required. Except for aliased
   * location expressions, the dereference is accomplished by {@link #getValueFormula(Expression,
   * CType, boolean)}. In case of aliased location expressions, the situation is trickier, as the
   * stored address may already be the dereferenced address. To resolve this ambiguity, the
   * parameter {@code directAddress} specifies whether, if the expression to dereference is already
   * an aliased location, it is already a direct address that should not be dereferenced further.
   *
   * @see CExpressionVisitorWithPointerAliasing
   * @param type Type of the {@code Expression} to dereference
   * @param toDereference The {@code Expression} to dereference
   * @param directAddress If true and the expression to dereference is an aliased location, does not
   *     dereference it further.
   * @return the result {@code AliasedLocation} of the pointed value
   */
  AliasedLocation applyDereference(
      final CType type, final Expression toDereference, final boolean directAddress) {

    if (toDereference.isAliasedLocation() && directAddress) {
      return toDereference.asAliasedLocation();
    } else {
      return AliasedLocation.ofAddress(
          getValueFormula(toDereference, CTypeUtils.implicitCastToPointer(type), false));
    }
  }

  /**
   * Creates a formula for the value of an expression.
   *
   * <p>As aliased location expression do not store the actual variable, but rather its address, the
   * address is dereferenced in this case. The parameter {@code isSafe} determines whether such a
   * reference is always safe to accomplish.
   *
   * <p>In case the expression is nondeterministic, returns a formula with a new nondeterministic
   * variable of the given type.
   *
   * @param expression The expression.
   * @param type The type of the expression.
   * @param isSafe Whether dereferencing the formula is safe or not.
   * @return A formula for the value.
   */
  Formula getValueFormula(final Expression expression, final CType type, final boolean isSafe) {

    Optional<Formula> optionalFormula = getOptionalValueFormula(expression, type, isSafe);
    if (optionalFormula.isPresent()) {
      return optionalFormula.get();
    }

    // nondet value, make a new nondet variable with the given type
    String nondetName = "__nondet_value_" + CTypeUtils.typeToString(type).replace(' ', '_');
    return conv.makeNondet(nondetName, type, ssa, constraints);
  }

  /**
   * Creates a formula for the value of an expression or returns empty Optional if the value is
   * nondeterministic.
   *
   * <p>As aliased location expression do not store the actual variable, but rather its address, the
   * address is dereferenced in this case. The parameter {@code isSafe} determines whether such a
   * reference is always safe to accomplish.
   *
   * @param expression The expression.
   * @param type The type of the expression.
   * @param isSafe Whether dereferencing the formula is safe or not.
   * @return An optional containing the formula for the value, empty if it is nondeterministic.
   */
  Optional<Formula> getOptionalValueFormula(
      final Expression expression, final CType type, final boolean isSafe) {
    return switch (expression.getKind()) {
      case ALIASED_LOCATION -> {
        final CType adjustedType = CTypes.adjustFunctionOrArrayType(type);

        MemoryRegion region = expression.asAliasedLocation().getMemoryRegion();
        if (region == null) {
          region = regionMgr.makeMemoryRegion(adjustedType);
        }
        if (isSafe) {
          yield Optional.of(
              conv.makeSafeDereference(
                  adjustedType, expression.asAliasedLocation().getAddress(), ssa, region));
        }
        yield Optional.of(
            conv.makeDereference(
                adjustedType,
                expression.asAliasedLocation().getAddress(),
                ssa,
                errorConditions,
                region));
      }
      case UNALIASED_LOCATION -> Optional.of(
          conv.makeVariable(expression.asUnaliasedLocation().getVariableName(), type, ssa));
      case DET_VALUE -> Optional.of(expression.asValue().getValue());
      case NONDET -> Optional.empty();
    };
  }

  /**
   * Adds a constraint that both given formulae have the same base address.
   *
   * @param p1 The first formula.
   * @param p2 The second formula.
   */
  void addEqualBaseAddressConstraint(final Formula p1, final Formula p2) {
    if (errorConditions.isEnabled()) {
      // Constraint is only necessary for correct error conditions
      constraints.addConstraint(
          conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(p1), conv.makeBaseAddressOfTerm(p2)));
    }
  }
}
