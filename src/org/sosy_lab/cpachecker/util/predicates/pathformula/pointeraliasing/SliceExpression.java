// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Represents a left-hand side or right-hand side of a slice assignment, allowing for quantified
 * indexing. For example, the function {@code memcpy(&b, &a, size * sizeof(int))} can be encoded as
 * {@code b[i] = a[i]} with slice variable {@code 0 <= i < size}. The slice variable is unrolled or
 * encoded during the assignment as needed.
 *
 * <p>Since an AST node cannot include array slicing directly, this class starts with {@link
 * CRightHandSide} as {@link #base} and allows adding modifiers after it. The modifiers are either
 * indexing or field access.
 *
 * <p>During the assignment handling, the quantifiers are resolved first by {@link
 * AssignmentQuantifierHandler}, which turns each unresolved {@link SliceVariableIndexModifier} into
 * a resolved {@link SliceFormulaIndexModifier} which contains the actual variable formula (fixed
 * index in case of quantifier unrolling, encoded variable name in case of quantifier encoding).
 * After the quantifiers are resolved, the whole modifier chain is applied to the base which was
 * already turned to {@link ResolvedSlice} previously.
 *
 * <p>Note that the base type is {@link CRightHandSide} even if we are representing the left-hand
 * side. This is so that {@link CRightHandSide} function parameters do not need to be formally
 * turned into {@link CLeftHandSide} by introducing formal assignment of (RHS) argument to (LHS)
 * parameter. It is the responsibility of user of this class to ensure that the left-hand sides
 * correspond to actual locations.
 */
final class SliceExpression {

  /** A helper class that tracks a resolved expression together with its corresponding C type. */
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
   * take values from {@code 0 <= i < sliceSize}. Indexing the left-hand side and right-hand side by
   * the same index will result in them sharing the same quantifier (or quantifier replacement).
   *
   * <p>In other words, in {@code a[i] = b[i]}, both instances of {@link SliceVariableIndexModifier}
   * should point to the same {@link SliceVariable}. Otherwise, it would mean {@code a[i] = b[j]}
   * where {@code i} and {@code j} are different quantified variables.
   *
   * <p>This is a class and not a record as records override equals to provide equality between
   * objects with equal fields. Here, an instance stands for a unique slice variable.
   */
  static class SliceVariable {
    private final CExpression sliceSize;

    SliceVariable(CExpression pSliceSize) {
      checkNotNull(pSliceSize);
      sliceSize = pSliceSize;
    }

    CExpression getSliceSize() {
      return sliceSize;
    }

    @Override
    public String toString() {
      // make sure toString() of distinct variables is distinct
      return super.toString() + " [size=" + sliceSize + "]";
    }
  }

  /** Represents a modification of the location represented by base. */
  sealed interface SliceModifier permits SliceFieldAccessModifier, SliceIndexModifier {}

  /** Represents performing indexing on a {@code CExpression}, i.e., {@code base[i]}. */
  sealed interface SliceIndexModifier extends SliceModifier
      permits SliceVariableIndexModifier, SliceFormulaIndexModifier {}

  /** Represents performing a field access on a {@code CExpression}, i.e., {@code base.field}. */
  record SliceFieldAccessModifier(CCompositeTypeMemberDeclaration field)
      implements SliceModifier {
    SliceFieldAccessModifier(CCompositeTypeMemberDeclaration field) {
      checkNotNull(field);
      this.field = field;
    }
  }

  /**
   * Represents performing indexing on a {@code CExpression}, i.e., {@code base[i]}, where {@code i}
   * is a slice variable.
   */
  record SliceVariableIndexModifier(SliceVariable index) implements SliceIndexModifier {
    SliceVariableIndexModifier(SliceVariable index) {
      checkNotNull(index);
      this.index = index;
    }
  }

  /**
   * Represents performing indexing on a {@code CExpression}, i.e., {@code base[f]}, where {@code f}
   * is a resolved formula.
   */
  record SliceFormulaIndexModifier(Formula encodedVariable) implements SliceIndexModifier {
    SliceFormulaIndexModifier(Formula encodedVariable) {
      checkNotNull(encodedVariable);
      this.encodedVariable = encodedVariable;
    }
  }

  /**
   * The base of slice expression, which is a {@link CRightHandSide} so that both right-hand sides
   * and left-hand sides can be retained ({@link CLeftHandSide} extends {@link CRightHandSide}).
   */
  private final CRightHandSide base;

  /**
   * The base can be modified by multiple modifiers, which are either field access, i.e., {@code
   * base.field}, or indexing, i.e., {@code base[i]}. The modifiers are stored left-to-right.
   */
  private final ImmutableList<SliceModifier> modifiers;

  /** Construct the slice expression using a base, with no modifiers. */
  SliceExpression(CRightHandSide base) {
    this.base = checkNotNull(base);
    this.modifiers = ImmutableList.of();
  }

  /** Construct the slice expression using both the base and modifiers. */
  SliceExpression(CRightHandSide base, ImmutableList<SliceModifier> pModifiers) {
    this.base = checkNotNull(base);
    this.modifiers = checkNotNull(pModifiers);
  }

  /**
   * Return a new {@link SliceExpression} with access of the specified field as the last modifier.
   *
   * @param field The structure member which should be accessed. It is the responsibilty of the
   *     calling code to ensure it can actually be accessed at that point.
   * @return The modified {@link SliceExpression}
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
   * Return a new {@link SliceExpression} with indexing by the given slice variable as the last
   * modifier.
   *
   * @param index The index variable that should be used for indexing.
   * @return The modified {@link SliceExpression}
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
   * Return a new {@code ArraySliceExpression} where indexing by the given quantified variable is
   * replaced with indexing by the given formula.
   *
   * @param sliceVariable The slice variable to resolve
   * @param replacementFormula The formula to resolve the variable with
   * @return The resolved {@link SliceExpression}
   */
  SliceExpression resolveVariable(SliceVariable sliceVariable, Formula replacementFormula) {

    // replace variable index modifiers on the given variable by formula index modifiers
    List<SliceModifier> newModifiers =
        modifiers.stream()
            .map(
                modifier ->
                    modifier instanceof SliceVariableIndexModifier quantifiedModifier
                            && quantifiedModifier.index.equals(sliceVariable)
                        ? new SliceFormulaIndexModifier(replacementFormula)
                        : modifier)
            .toList();

    return new SliceExpression(base, ImmutableList.copyOf(newModifiers));
  }

  /**
   * Returns the canonical type of full expression. Use {@code
   * getBase().getExpressionType().getCanonicalType()} instead if you want the type of the base.
   *
   * @return The canonical type of expression after it is resolved
   */
  CType getFullExpressionType() {
    // start with base type
    CType type = base.getExpressionType().getCanonicalType();

    for (SliceModifier modifier : modifiers) {
      if (modifier instanceof SliceFieldAccessModifier fieldModifier) {
        // replace type with field type
        type = fieldModifier.field.getType().getCanonicalType();
      } else {
        assert (modifier instanceof SliceVariableIndexModifier);
        // replace type with the type of its element
        CPointerType adjustedPointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(type);
        type = adjustedPointerType.getType().getCanonicalType();
      }
    }

    return type;
  }

  /**
   * Makes a canonical {@link SliceExpression} out of this one. A canonical expression has all outer
   * field accesses in base moved to the start of modifiers.
   *
   * @return A canonical version of this expression.
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
        // pointer dereference, convert to explicit pointer dereference
        outerFieldReference = outerFieldReference.withExplicitPointerDereference();
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

  /** Returns all slice variables present in the modifiers. */
  ImmutableList<SliceVariable> getPresentVariables() {
    ImmutableList.Builder<SliceVariable> builder = ImmutableList.builder();
    for (SliceModifier modifier : modifiers) {
      if (modifier instanceof SliceVariableIndexModifier subscriptModifier) {
        builder.add(subscriptModifier.index());
      }
    }
    return builder.build();
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

  /** Returns whether there are any resolved modifiers, i.e. slice formula index modifiers. */
  boolean containsResolvedModifiers() {
    return modifiers.stream().anyMatch(modifier -> modifier instanceof SliceFormulaIndexModifier);
  }

  /** Returns the base. */
  CRightHandSide getBase() {
    return base;
  }

  /** Returns all modifiers. */
  ImmutableList<SliceModifier> getModifiers() {
    return modifiers;
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
