// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;

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
final class ArraySliceExpression {

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
  static class ArraySliceIndexVariable {
    private final CExpression size;

    ArraySliceIndexVariable(CExpression pSize) {
      checkNotNull(pSize);
      size = pSize;
    }

    CExpression getSize() {
      return size;
    }
  }

  /**
   * The base CExpression can be modified by multiple modifiers, which are either field access,
   * i.e., {@code base.field}, or subscript, i.e., {@code base[i]}.
   */
  sealed interface ArraySliceModifier
      permits ArraySliceFieldAccessModifier, ArraySliceSubscriptModifier {}

  /** Represents performing a field access on a {@code CExpression}, i.e., {@code base.field}. */
  record ArraySliceFieldAccessModifier(CCompositeTypeMemberDeclaration field)
      implements ArraySliceModifier {}

  /**
   * Represents performing a subscript operation on a {@code CExpression}, i.e., {@code base[i]}.
   */
  record ArraySliceSubscriptModifier(ArraySliceIndexVariable index) implements ArraySliceModifier {}

  private final CExpression base;
  private final ImmutableList<ArraySliceModifier> modifiers;

  /**
   * Construct using a base, with no modifiers.
   *
   * @param pBase The base
   */
  ArraySliceExpression(CExpression pBase) {
    base = checkNotNull(pBase);
    modifiers = ImmutableList.of();
  }

  private ArraySliceExpression(CExpression pBase, ImmutableList<ArraySliceModifier> pModifiers) {
    base = checkNotNull(pBase);
    modifiers = checkNotNull(pModifiers);
  }

  static ArraySliceExpression fromSplit(ArraySliceSplitExpression split) {
    return split.head().withFieldAccesses(split.tail.list);
  }

  /**
   * Return a new {@code ArraySliceExpression} with access of the specified field as the last
   * modifier.
   *
   * @param field The structure member which should be accessed. It is the responsibilty of the
   *     calling code to ensure it can actually be accessed at that point.
   * @return The modified {@code ArraySliceExpression}
   */
  ArraySliceExpression withFieldAccess(CCompositeTypeMemberDeclaration field) {
    checkNotNull(field);
    if (modifiers.isEmpty()) {
      CExpression newBase =
          new CFieldReference(FileLocation.DUMMY, field.getType(), field.getName(), base, false);
      // do not add to modifiers, resolve immediately
      return new ArraySliceExpression(newBase, ImmutableList.of());
    }

    // add to modifiers
    ImmutableList<ArraySliceModifier> newModifiers =
        ImmutableList.<ArraySliceModifier>builder()
            .addAll(modifiers)
            .add(new ArraySliceFieldAccessModifier(field))
            .build();
    return new ArraySliceExpression(base, newModifiers);
  }

  ArraySliceExpression withFieldAccesses(ImmutableList<CCompositeTypeMemberDeclaration> fields) {
    // TODO: make the list version basic
    ArraySliceExpression result = this;
    for (CCompositeTypeMemberDeclaration field : fields) {
      result = result.withFieldAccess(field);
    }
    return result;
  }

  /**
   * Return a new {@code ArraySliceExpression} with indexing by the specified variable as the last
   * modifier.
   *
   * @param index The index variable that should be used for indexing.
   * @return The modified {@code ArraySliceExpression}
   */
  ArraySliceExpression withIndex(ArraySliceIndexVariable index) {
    checkNotNull(index);
    // cannot resolve immediately, add to modifiers
    ImmutableList<ArraySliceModifier> newModifiers =
        ImmutableList.<ArraySliceModifier>builder()
            .addAll(modifiers)
            .add(new ArraySliceSubscriptModifier(index))
            .build();
    return new ArraySliceExpression(base, newModifiers);
  }

  /**
   * Return a new {@code ArraySliceExpression} where the first modifier (which is always an indexing
   * modifier) is resolved by a concrete value of index {@code indexValue}.
   *
   * <p>This is done by dropping the first modifier and auto-resolving field modifiers that follow,
   * so that there are, again, either no modifiers in the returned {@code ArraySliceExpression} or
   * the first modifier is an {@code ArraySliceSubscriptModifier}.
   *
   * @param sizeType Machine pointer-equivalent size type
   * @param indexValue The concrete value of the index which resolves the index variable
   * @return The resolved {@code ArraySliceExpression}
   * @throws IllegalStateException If there were no modifiers.
   */
  public ArraySliceExpression resolveFirstIndex(CType sizeType, long indexValue) {
    checkState(!isResolved());

    // just wrap base in a subscript with the index value
    CExpression indexValueExpression =
        CIntegerLiteralExpression.createDummyLiteral(indexValue, sizeType);
    // the element type must be taken from the base type
    CPointerType basePointerType =
        (CPointerType)
            CTypes.adjustFunctionOrArrayType(base.getExpressionType().getCanonicalType());
    CType elementType = basePointerType.getType().getCanonicalType();
    CExpression newBase =
        new CArraySubscriptExpression(FileLocation.DUMMY, elementType, base, indexValueExpression);
    return resolveFirstIndex(newBase);
  }

  /**
   * Return a new {@code ArraySliceExpression} where the first modifier (which is always an indexing
   * modifier) is resolved by the parameter {@code newBase}.
   *
   * <p>This is done by dropping the first modifier and auto-resolving field modifiers that follow,
   * so that there are, again, either no modifiers in the returned {@code ArraySliceExpression} or
   * the first modifier is an {@code ArraySliceSubscriptModifier}.
   *
   * @param newBase The new base to which the old base with first modifier was resolved
   * @return The resolved {@code ArraySliceExpression}
   * @throws IllegalStateException If there were no modifiers.
   */
  ArraySliceExpression resolveFirstIndex(CExpression newBase) {
    checkNotNull(newBase);
    checkState(!isResolved());

    // we will drop the first (subscript) modifier and auto-resolve all field-access fields after
    // it, retaining the next subscript modifier and fields after it

    ImmutableList.Builder<ArraySliceModifier> builder = ImmutableList.<ArraySliceModifier>builder();

    boolean canResolve = true;
    // skip the first modifier which is already resolved
    for (ArraySliceModifier modifier : Iterables.skip(modifiers, 1)) {
      if (canResolve && (modifier instanceof ArraySliceFieldAccessModifier fieldModifier)) {
        newBase =
            new CFieldReference(
                FileLocation.DUMMY,
                fieldModifier.field.getType(),
                fieldModifier.field.getName(),
                newBase,
                false);
      } else {
        builder.add(modifier);
        canResolve = false;
      }
    }

    return new ArraySliceExpression(newBase, builder.build());
  }

  /**
   * Returns whether the expression is resolved, i.e., there are no modifiers left.
   *
   * @return Whether the expression is resolved.
   */
  boolean isResolved() {
    return modifiers.isEmpty();
  }

  /**
   * Returns a resolved expression, throws if it is unresolved.
   *
   * @return The fully resolved expression.
   * @throws IllegalStateException If the expression is unresolved due to some remaining modifiers.
   */
  CExpression getResolvedExpression() {
    checkState(isResolved());

    return base;
  }

  /**
   * Returns the base expression, which may be still unresolved.
   *
   * @return The base expression.
   */
  CExpression getBaseExpression() {
    return base;
  }

  record ArraySliceTail(ImmutableList<CCompositeTypeMemberDeclaration> list) {}

  record ArraySliceSplitExpression(ArraySliceExpression head, ArraySliceTail tail) {}

  ArraySliceSplitExpression getSplit() {
    if (isResolved()) {

      // resolved, extract the trailing field accesses from the base expression
      // the CExpression is bottom-up, i.e. the rightmost access is first in CExpression, therefore,
      // the list of tail fields will be constructed reversed
      List<CCompositeTypeMemberDeclaration> reversedTailFields = new ArrayList<>();
      CExpression current = base;
      while (current instanceof CFieldReference currentField) {
        if (currentField.isPointerDereference()) {
          // field dereference, we need to stop
          break;
        }
        reversedTailFields.add(
            new CCompositeTypeMemberDeclaration(
                currentField.getExpressionType(), currentField.getFieldName()));
        current = currentField.getFieldOwner();
      }
      // reverse the reversed list of tail fields
      ImmutableList<CCompositeTypeMemberDeclaration> tailFields =
          ImmutableList.copyOf(Lists.reverse(reversedTailFields));
      return new ArraySliceSplitExpression(
          new ArraySliceExpression(current), new ArraySliceTail(tailFields));
    }
    // not resolved, extract the trailing field accesses from modifiers
    // to prevent errors, we do this by adding field modifiers to the tail modifiers, then adding
    // the rest to head modifiers; since we iterate from the back, the resulting lists will be
    // reversed
    List<ArraySliceModifier> reversedHeadModifiers = new ArrayList<>();
    List<CCompositeTypeMemberDeclaration> reversedTailFields = new ArrayList<>();
    boolean addToTail = true;
    for (ArraySliceModifier modifier : Lists.reverse(modifiers)) {
      if (addToTail && (modifier instanceof ArraySliceFieldAccessModifier fieldModifier)) {
        reversedTailFields.add(fieldModifier.field);
      } else {
        addToTail = false;
        reversedHeadModifiers.add(modifier);
      }
    }

    // reverse the reversed lists

    ImmutableList<ArraySliceModifier> headModifiers =
        ImmutableList.copyOf(Lists.reverse(reversedHeadModifiers));
    ImmutableList<CCompositeTypeMemberDeclaration> tailFields =
        ImmutableList.copyOf(Lists.reverse(reversedTailFields));

    return new ArraySliceSplitExpression(
        new ArraySliceExpression(base, headModifiers), new ArraySliceTail(tailFields));
  }

  /**
   * Returns a dummy-resolved expression where each unresolved index is replaced with a zero
   * literal.
   *
   * @param sizeType Machine pointer-equivalent size type
   * @return Dummy-resolved expression.
   */
  CExpression getDummyResolvedExpression(CType sizeType) {
    checkNotNull(sizeType);
    // resolve the expression with dummy zero indices to get the type
    CExpression resolved = base;
    for (ArraySliceModifier modifier : modifiers) {
      if (modifier instanceof ArraySliceFieldAccessModifier fieldAccess) {
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
            (CPointerType) CTypes.adjustFunctionOrArrayType(resolved.getExpressionType());
        CType underlyingType = resolvedPointerType.getType().getCanonicalType();

        resolved =
            new CArraySubscriptExpression(
                FileLocation.DUMMY, underlyingType, resolved, indexLiteral);
      }
    }
    return resolved;
  }

  /**
   * Returns the canonical type of fully resolved expression. Use {@code
   * getBaseExpression().getExpressionType().getCanonicalType()} instead if you want the type of the
   * base which may be still unresolved.
   *
   * @param sizeType Machine pointer-equivalent size type
   * @return The canonical type of expression after it is resolved
   */
  CType getResolvedExpressionType(CType sizeType) {
    return getDummyResolvedExpression(sizeType).getExpressionType().getCanonicalType();
  }

  /**
   * @return The index variable of the first subscript modifier.
   * @throws IllegalStateException If the expression is resolved
   */
  ArraySliceIndexVariable getFirstIndex() {
    checkState(!isResolved());

    return ((ArraySliceSubscriptModifier) modifiers.get(0)).index;
  }

  /**
   * Returns all unresolved modifiers.
   *
   * @return All unresolved modifiers.
   */
  ImmutableList<ArraySliceModifier> getModifiers() {
    return modifiers;
  }

  ImmutableList<ArraySliceIndexVariable> getUnresolvedIndexVariables() {
    ImmutableList.Builder<ArraySliceIndexVariable> builder = ImmutableList.builder();
    for (ArraySliceModifier modifier : modifiers) {
      if (modifier instanceof ArraySliceSubscriptModifier subscriptModifier) {
        builder.add(subscriptModifier.index());
      }
    }
    return builder.build();
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
    ArraySliceExpression other = (ArraySliceExpression) obj;
    return Objects.equals(base, other.base) && Objects.equals(modifiers, other.modifiers);
  }
}
