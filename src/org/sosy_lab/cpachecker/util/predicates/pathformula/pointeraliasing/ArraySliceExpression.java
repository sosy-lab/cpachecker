// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.Immutable;
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
@Immutable
final class ArraySliceExpression {

  /**
   * A helper record for indexing by quantified variables, standing for an index {@code i} which can
   * take values from {@code 0 <= i < size}. Indexing the left-hand side and right-hand side by the
   * same index will result in them sharing the same quantifier (or quantifier replacement).
   *
   * <p>In other words, in {@code a[i] = b[i]}, both instances of {@code
   * ArraySliceSubscriptModifier} should point to the same {@code ArraySliceIndex}. Otherwise, it
   * would mean {@code a[i] = b[j]} where {@code i} and {@code j} are different quantified
   * variables.
   */
  record ArraySliceIndexVariable(CExpression size) {}

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

  /**
   * Return a new {@code ArraySliceExpression} with access of the specified field as the last modifier.
   *
   * @param field The structure member which should be accessed. It is the responsibilty of the calling code to ensure it can actually be accessed at that point.
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
   * modifier) is resolved by the parameter {@code newBase}.
   *
   * <p>This is done by dropping the first modifier and auto-resolving field modifiers that follow,
   * so that there are, again, either no modifiers in the returned {@code ArraySliceExpression} or
   * the first modifier is an {@code AraySliceSubscriptModifier}.
   *
   * @param newBase The new base to which the old base with first modifier was resolved
   * @return The resolved {@code ArraySliceExpression}
   * @throws IllegalStateException If there were no modifiers.
   */
  ArraySliceExpression resolveFirstIndex(CExpression newBase) {
    checkNotNull(newBase);

    // we will drop the first modifier and resolve all fields after it

    UnmodifiableIterator<ArraySliceModifier> it = modifiers.iterator();

    if (!it.hasNext()) {
      throw new IllegalStateException("Cannot resolve first index as there is none");
    }
    it.next();

    ImmutableList.Builder<ArraySliceModifier> builder = ImmutableList.<ArraySliceModifier>builder();

    boolean canResolve = true;
    // skip the first modifier which is being resolved
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
      }
    }

    return new ArraySliceExpression(newBase, builder.build());
  }

  /** @return Whether the expression is resolved, i.e., there are no modifiers left. */
  boolean isResolved() {
    return modifiers.isEmpty();
  }

  /**
   * @return The fully resolved expression.
   * @throws IllegalStateException If the expression is unresolved due to some remaining modifiers.
   */
  CExpression getResolvedExpression() {
    if (!modifiers.isEmpty()) {
      throw new IllegalStateException(
          "Cannot get resolved expression as there are still some modifiers");
    }

    return base;
  }

  /** @return The base expression, which may be still unresolved. */
  CExpression getBaseExpression() {
    return base;
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
                base,
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
    return resolved.getExpressionType().getCanonicalType();
  }

  /**
   * @return The index variable of the first subscript modifier.
   * @throws IllegalStateException If the expression is resolved
   */
  ArraySliceIndexVariable getFirstIndex() {
    if (modifiers.isEmpty()) {
      throw new IllegalStateException("Cannot get first index as there is none");
    }

    return ((ArraySliceSubscriptModifier) modifiers.get(0)).index;
  }

  /** @return All remaining modifiers. */
  ImmutableList<ArraySliceModifier> getModifiers() {
    return modifiers;
  }

  @Override
  public String toString() {
    return "ArraySliceExpression [base=" + base + ", modifiers=" + modifiers + "]";
  }
}
