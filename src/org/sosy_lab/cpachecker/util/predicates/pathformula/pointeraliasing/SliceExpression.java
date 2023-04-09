// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.java_smt.api.Formula;

/**
 * A helper class for realization of assignments to array slices, used for indexing by quantified
 * variables. Since a {@code CExpression} cannot include array slicing (in this context meaning
 * indexing by a quantified variable), this class wraps {@code CExpression} and allows adding
 * modifiers, which consist of either slice-indexing or accessing fields.
 *
 * <p>If field modifiers are added before the first slicing, they are immediately applied to the
 * {@code CExpression}. For example, {@code foo.a[i].b} results in the base {@code foo.a} and two
 * modifiers representing {@code [i]} and {@code .b} respectively.
 */
final class SliceExpression {

  record ResolvedSlice(Expression expression, CType type) {

    ResolvedSlice(Expression expression, CType type) {
      checkNotNull(expression);
      checkIsSimplified(type);
      this.expression = expression;
      this.type = type;
    }
  }

  /**
   * A helper class for indexing by quantified variables, standing for an index {@code i} which can
   * take values from {@code 0 <= i < size}. Indexing the left-hand side and right-hand side by the
   * same index will result in them sharing the same quantifier (or quantifier replacement).
   *
   * <p>In other words, in {@code a[i] = b[i]}, both instances of {@code
   * ArraySliceSubscriptModifier} should point to the same {@code ArraySliceIndex}. Otherwise, it
   * would mean {@code a[i] = b[j]} where {@code i} and {@code j} are different quantified
   * variables.
   *
   * <p>This is a class and not a record as records override equals to provide equality between
   * objects with equal fields. Here, an instance of the index stands for a unique quantified
   * variable.
   */
  static class SliceVariable {
    private final CExpression size;

    SliceVariable(CExpression pSize) {
      checkNotNull(pSize);
      size = pSize;
    }

    CExpression getSize() {
      return size;
    }

    @Override
    public String toString() {
      return "ArraySliceIndexVariable [size=" + size + "]";
    }
  }

  /**
   * The base CExpression can be modified by multiple modifiers, which are either field access,
   * i.e., {@code base.field}, or subscript, i.e., {@code base[i]}.
   */
  sealed interface SliceModifier
      permits SliceFieldAccessModifier, SliceIndexModifier {}

  sealed interface SliceIndexModifier extends SliceModifier
      permits SliceVariableIndexModifier, SliceResolvedIndexModifier {}

  /** Represents performing a field access on a {@code CExpression}, i.e., {@code base.field}. */
  record SliceFieldAccessModifier(CCompositeTypeMemberDeclaration field)
      implements SliceModifier {
    SliceFieldAccessModifier(CCompositeTypeMemberDeclaration field) {
      checkNotNull(field);
      this.field = field;
    }
  }

  /**
   * Represents performing a subscript operation on a {@code CExpression}, i.e., {@code base[i]}.
   */
  record SliceVariableIndexModifier(SliceVariable index)
      implements SliceIndexModifier {
    SliceVariableIndexModifier(SliceVariable index) {
      checkNotNull(index);
      this.index = index;
    }
  }

  /** Represents performing a subscript operation with a resolved formula. */
  record SliceResolvedIndexModifier(Formula encodedVariable)
      implements SliceIndexModifier {
    SliceResolvedIndexModifier(Formula encodedVariable) {
      checkNotNull(encodedVariable);
      this.encodedVariable = encodedVariable;
    }
  }

  private final CRightHandSide base;
  private final ImmutableList<SliceModifier> modifiers;

  /**
   * Construct using a base, with no modifiers.
   *
   * @param base The base
   */
  SliceExpression(CRightHandSide base) {
    this.base = checkNotNull(base);
    this.modifiers = ImmutableList.of();
  }

  SliceExpression(CRightHandSide base, ImmutableList<SliceModifier> pModifiers) {
    this.base = checkNotNull(base);
    this.modifiers = checkNotNull(pModifiers);
  }

  /**
   * Return a new {@code ArraySliceExpression} with access of the specified field as the last
   * modifier.
   *
   * @param field The structure member which should be accessed. It is the responsibilty of the
   *     calling code to ensure it can actually be accessed at that point.
   * @return The modified {@code ArraySliceExpression}
   */
  SliceExpression withFieldAccess(CCompositeTypeMemberDeclaration field) {
    checkNotNull(field);
    // add to modifiers
    ImmutableList<SliceModifier> newModifiers =
        ImmutableList.<SliceModifier>builder()
            .addAll(modifiers)
            .add(new SliceFieldAccessModifier(field))
            .build();
    return new SliceExpression(base, newModifiers);
  }

  /**
   * Return a new {@code ArraySliceExpression} with indexing by the specified variable as the last
   * modifier.
   *
   * @param index The index variable that should be used for indexing.
   * @return The modified {@code ArraySliceExpression}
   */
  SliceExpression withIndex(SliceVariable index) {
    checkNotNull(index);
    // add to modifiers
    ImmutableList<SliceModifier> newModifiers =
        ImmutableList.<SliceModifier>builder()
            .addAll(modifiers)
            .add(new SliceVariableIndexModifier(index))
            .build();
    return new SliceExpression(base, newModifiers);
  }

  /**
   * Return a new {@code ArraySliceExpression} where the quantified modifiers with a given
   * quantified variable are resolved by a given formula.
   *
   * @param quantifiedVariable The quantified variable to resolve
   * @param encodedVariable The formula to resolve the variable with
   * @return The resolved {@code ArraySliceExpression}
   * @throws IllegalStateException If there were no modifiers.
   */
  SliceExpression resolveVariable(
      SliceVariable quantifiedVariable, Formula encodedVariable) {

    // replace quantified subscript modifiers on the given variable by fixed subscripts
    // with the provided encoded variable subscript modifiers
    List<SliceModifier> newModifiers =
        modifiers.stream()
            .map(
                modifier ->
                    modifier instanceof SliceVariableIndexModifier quantifiedModifier
                            && quantifiedModifier.index.equals(quantifiedVariable)
                        ? new SliceResolvedIndexModifier(encodedVariable)
                        : modifier)
            .toList();

    return new SliceExpression(base, ImmutableList.copyOf(newModifiers));
  }

  /**
   * Returns whether the expression is resolved, i.e., there are only field access and fixed
   * subscript modifiers.
   *
   * @return Whether the expression is resolved.
   */
  boolean isResolved() {
    return modifiers.stream()
        .allMatch(
            modifier ->
                modifier instanceof SliceFieldAccessModifier
                    || modifier instanceof SliceResolvedIndexModifier);
  }

  /**
   * Returns the base.
   *
   * @return The base.
   */
  CRightHandSide getBase() {
    return base;
  }

  /**
   * Returns the canonical type of full expression. Use {@code
   * getBase().getExpressionType().getCanonicalType()} instead if you want the type of the base.
   *
   * @return The canonical type of expression after it is resolved
   */
  CType getFullExpressionType() {
    CType type = base.getExpressionType().getCanonicalType();

    for (SliceModifier modifier : modifiers) {
      if (modifier instanceof SliceFieldAccessModifier fieldModifier) {
        // replace type with field type
        type = fieldModifier.field.getType().getCanonicalType();
      } else {
        assert (modifier instanceof SliceVariableIndexModifier);
        // replace type with its element type
        CPointerType adjustedPointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(type);
        type = adjustedPointerType.getType().getCanonicalType();
      }
    }

    return type;
  }

  /**
   * Makes a canonical ArraySliceExpression out of this one. A canonical expression has all outer
   * field accesses in base (that do not dereference) moved to the start of modifiers.
   *
   * @return A canonical version of this.
   */
  SliceExpression constructCanonical() {
    // only CExpression can have outer field accesses
    if (!(base instanceof CExpression)) {
      return this;
    }
    CExpression currentBase = (CExpression) base;
    List<SliceModifier> canonicalModifiers = new ArrayList<>(modifiers);
    while (currentBase instanceof CFieldReference outerFieldReference) {
      if (outerFieldReference.isPointerDereference()) {
        // pointer dereference, stop canonizing
        break;
      }
      // the outer reference must be added in front of all previous modifiers
      canonicalModifiers.add(
          0,
          new SliceFieldAccessModifier(
              new CCompositeTypeMemberDeclaration(
                  outerFieldReference.getExpressionType(), outerFieldReference.getFieldName())));
      currentBase = outerFieldReference.getFieldOwner();
    }
    return new SliceExpression(currentBase, ImmutableList.copyOf(canonicalModifiers));
  }

  /**
   * Returns all modifiers.
   *
   * @return All modifiers.
   */
  ImmutableList<SliceModifier> getModifiers() {
    return modifiers;
  }

  ImmutableList<SliceVariable> getUnresolvedIndexVariables() {
    ImmutableList.Builder<SliceVariable> builder = ImmutableList.builder();
    for (SliceModifier modifier : modifiers) {
      if (modifier instanceof SliceVariableIndexModifier subscriptModifier) {
        builder.add(subscriptModifier.index());
      }
    }
    return builder.build();
  }

  ResolvedSlice resolveModifiers(
      ResolvedSlice resolvedBase,
      CToFormulaConverterWithPointerAliasing conv,
      SSAMapBuilder ssa,
      ErrorConditions errorConditions,
      MemoryRegionManager regionMgr) {

    // we have resolved the base
    ResolvedSlice resolved = resolvedBase;

    boolean wasParameterId = (base instanceof CIdExpression idBase) &&
         idBase.getDeclaration() instanceof CParameterDeclaration;

    // resolve the modifiers now
    for (SliceModifier modifier : modifiers) {
      if (modifier instanceof SliceIndexModifier subscriptModifier) {
        resolved =
            convertSubscriptModifier(
                conv, ssa, errorConditions, regionMgr, resolved, subscriptModifier, wasParameterId);
      } else {
        resolved =
            convertFieldAccessModifier(
                conv, regionMgr, resolved, (SliceFieldAccessModifier) modifier);
      }
    }

    return resolved;
  }

  private Formula asValueFormula(
      CToFormulaConverterWithPointerAliasing conv,
      SSAMapBuilder ssa,
      ErrorConditions errorConditions,
      MemoryRegionManager regionMgr,
      final Expression e,
      final CType type,
      final boolean isSafe) {
    // TODO: deduplicate with CExpressionVisitorWithPointerAliasing.asValueFormula
    if (e.isNondetValue()) {
      throw new IllegalStateException();
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

  private ResolvedSlice convertSubscriptModifier(
      CToFormulaConverterWithPointerAliasing conv,
      SSAMapBuilder ssa,
      ErrorConditions errorConditions,
      MemoryRegionManager regionMgr,
      ResolvedSlice resolved,
      SliceIndexModifier modifier,
      boolean wasParameterId) {

    final AliasedLocation dereferenced;

    // dereference resolved
    // TODO: deduplicate with CExpressionVisitorWithPointerAliasing.dereference
    boolean shouldTreatAsDirectAccess = resolved.expression.isAliasedLocation()
        && (resolved.type instanceof CCompositeType
            || (resolved.type instanceof CArrayType
                && !wasParameterId));
    if (shouldTreatAsDirectAccess) {
      dereferenced = resolved.expression.asAliasedLocation();
    } else {
      dereferenced =
          AliasedLocation.ofAddress(
              asValueFormula(
                  conv,
                  ssa,
                  errorConditions,
                  regionMgr,
                  resolved.expression,
                  CTypeUtils.implicitCastToPointer(resolved.type),
                  shouldTreatAsDirectAccess));
    }

    // all subscript modifiers must be already resolved here
    SliceResolvedIndexModifier resolvedModifier =
        (SliceResolvedIndexModifier) modifier;

    // get the array element type
    CPointerType basePointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(resolved.type());
    final CType elementType = conv.typeHandler.simplifyType(basePointerType.getType());

    // get base array address, arrays must be always aliased
    Formula baseAddress = dereferenced.getAddress();

    // get size of array element
    final Formula sizeofElement =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));

    // perform pointer arithmetic, we have array[base] and want array[base + i]
    // the quantified variable i must be multiplied by the sizeof the element type
    final Formula adjustedAddress =
        conv.fmgr.makePlus(
            baseAddress, conv.fmgr.makeMultiply(resolvedModifier.encodedVariable(), sizeofElement));

    // return the resolved formula with adjusted address and array element type
    return new ResolvedSlice(AliasedLocation.ofAddress(adjustedAddress), elementType);
  }

  private ResolvedSlice convertFieldAccessModifier(
      CToFormulaConverterWithPointerAliasing conv,
      MemoryRegionManager regionMgr,
      ResolvedSlice resolved,
      SliceFieldAccessModifier modifier) {

    // the base type must be a composite type to have fields
    CCompositeType baseType = (CCompositeType) resolved.type();
    final String fieldName = modifier.field().getName();
    CType fieldType = conv.typeHandler.getSimplifiedType(modifier.field());

    // composite types may be aliased or unaliased, resolve in both cases
    if (resolved.expression().isUnaliasedLocation()) {
      UnaliasedLocation resultLocation =
          UnaliasedLocation.ofVariableName(
              getFieldAccessName(
                  resolved.expression().asUnaliasedLocation().getVariableName(), modifier.field()));
      return new ResolvedSlice(resultLocation, fieldType);
    }

    // aliased location
    // we will increase the base address by field offset
    Formula baseAddress = resolved.expression().asAliasedLocation().getAddress();

    // we must create a memory region for access
    final MemoryRegion region = regionMgr.makeMemoryRegion(baseType, modifier.field());

    final OptionalLong offset = conv.typeHandler.getOffset(baseType, fieldName);
    if (!offset.isPresent()) {
      // this loses assignments from/to aliased bitfields
      // TODO: implement aliased bitfields
      return new ResolvedSlice(Value.nondetValue(), fieldType);
    }

    final Formula offsetFormula =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, offset.orElseThrow());
    final Formula adjustedAdress = conv.fmgr.makePlus(baseAddress, offsetFormula);

    AliasedLocation adjustedLocation = AliasedLocation.ofAddressWithRegion(adjustedAdress, region);
    return new ResolvedSlice(adjustedLocation, fieldType);
  }

  /**
   * Returns a dummy-resolved expression where each unresolved index is replaced with a zero
   * literal.
   *
   * @param sizeType Machine pointer-equivalent size type
   * @return Dummy-resolved expression.
   * @throws IllegalStateException If the combination of base and modifiers cannot be expressed as
   *     dummy-resolved expression
   */
  CRightHandSide getDummyResolvedExpression(CType sizeType) {
    checkNotNull(sizeType);

    if (modifiers.isEmpty()) {
      return base;
    }

    if (!(base instanceof CExpression)) {
      throw new IllegalStateException(
          "Cannot get dummy resolved expression, base is not CExpression and has modifiers");
    }

    // resolve the expression with dummy zero indices to get the type
    CExpression resolved = (CExpression) base;
    for (SliceModifier modifier : modifiers) {
      if (modifier instanceof SliceFieldAccessModifier fieldAccess) {
        resolved =
            new CFieldReference(
                FileLocation.DUMMY,
                fieldAccess.field.getType(),
                fieldAccess.field.getName(),
                resolved,
                false);
      } else {
        // subscript
        CIntegerLiteralExpression indexLiteral =
            CIntegerLiteralExpression.createDummyLiteral(0, sizeType);

        CPointerType resolvedPointerType =
            (CPointerType)
                CTypes.adjustFunctionOrArrayType(resolved.getExpressionType().getCanonicalType());
        CType underlyingType = resolvedPointerType.getType().getCanonicalType();

        resolved =
            new CArraySubscriptExpression(
                FileLocation.DUMMY, underlyingType, resolved, indexLiteral);
      }
    }
    return resolved;
  }

  boolean containsUnresolvedIndexModifiers() {
    return modifiers.stream()
        .anyMatch(modifier -> modifier instanceof SliceVariableIndexModifier);
  }

  boolean containsResolvedIndexModifiers() {
    return modifiers.stream()
        .anyMatch(modifier -> modifier instanceof SliceResolvedIndexModifier);
  }

  @Override
  public String toString() {
    return "ArraySliceExpression [base=" + base + ", modifiers=" + modifiers + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(base, modifiers);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SliceExpression other = (SliceExpression) obj;
    return Objects.equals(base, other.base) && Objects.equals(modifiers, other.modifiers);
  }


}
