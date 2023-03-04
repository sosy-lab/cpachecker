// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.UnmodifiableIterator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

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
  interface ArraySliceModifier {}

  protected final class ArraySliceFieldAccessModifier implements ArraySliceModifier {
    final CCompositeTypeMemberDeclaration field;

    ArraySliceFieldAccessModifier(CCompositeTypeMemberDeclaration pField) {
      field = pField;
    }
  }

  protected final class ArraySliceSubscriptModifier implements ArraySliceModifier {
    final ArraySliceIndex index;

    ArraySliceSubscriptModifier(ArraySliceIndex pIndex) {
      index = pIndex;
    }
  }

  private final CExpression base;
  private final ImmutableList<ArraySliceModifier> modifiers;

  ArraySliceExpression resolveFirstIndex(CExpression newBase) {

    // we will drop the first modifier and resolve all fields after it

    UnmodifiableIterator<ArraySliceModifier> it = modifiers.iterator();

    if (!it.hasNext()) {
      throw new IllegalStateException("Cannot resolve first index as there is none");
    }
    it.next();

    Builder<ArraySliceModifier> builder = ImmutableList.<ArraySliceModifier>builder();

    boolean canResolve = true;
    while (it.hasNext()) {
      ArraySliceModifier modifier = it.next();
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

  boolean isResolved() {
    return modifiers.isEmpty();
  }

  CExpression getResolvedExpression() {
    if (!modifiers.isEmpty()) {
      throw new IllegalStateException(
          "Cannot get resolved expression as there are still modifiers");
    }

    return base;
  }

  CExpression getBaseExpression() {
    return base;
  }

  CType getExpressionType(CType sizeType) {
    // resolve the expression with dummy zero indices to get the type
    CExpression dummyResolved = base;
    for (ArraySliceModifier modifier : modifiers) {
      if (modifier instanceof ArraySliceFieldAccessModifier fieldAccess) {
        dummyResolved =
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
        dummyResolved =
            new CArraySubscriptExpression(
                FileLocation.DUMMY, sizeType, dummyResolved, indexLiteral);
      }
    }
    return dummyResolved.getExpressionType().getCanonicalType();
  }

  ArraySliceIndex getFirstIndex() {
    if (modifiers.isEmpty()) {
      throw new IllegalStateException("Cannot get first index as there is none");
    }

    return ((ArraySliceSubscriptModifier) modifiers.get(0)).index;
  }

  ArraySliceExpression(CExpression pBase) {
    base = pBase;
    modifiers = ImmutableList.of();
  }

  private ArraySliceExpression(CExpression pBase, ImmutableList<ArraySliceModifier> pModifiers) {
    base = pBase;
    modifiers = pModifiers;
  }

  ArraySliceExpression withFieldAccess(CCompositeTypeMemberDeclaration field) {
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

  ArraySliceExpression withIndex(ArraySliceIndex field) {
    // cannot resolve immediately, add to modifiers
    ImmutableList<ArraySliceModifier> newModifiers =
        ImmutableList.<ArraySliceModifier>builder()
            .addAll(modifiers)
            .add(new ArraySliceSubscriptModifier(field))
            .build();
    return new ArraySliceExpression(base, newModifiers);
  }
}
