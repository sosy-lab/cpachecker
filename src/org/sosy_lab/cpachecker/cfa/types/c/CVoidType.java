// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.InlineMe;
import java.util.Arrays;

/**
 * This class represents the type "void". It does not allow any modifiers and has only a single
 * instance.
 */
public enum CVoidType implements CType {
  VOID(CTypeQualifiers.NONE),
  ATOMIC_VOID(CTypeQualifiers.ATOMIC),
  CONST_VOID(CTypeQualifiers.CONST),
  VOLATILE_VOID(CTypeQualifiers.VOLATILE),
  ATOMIC_CONST_VOID(CTypeQualifiers.ATOMIC_CONST),
  ATOMIC_VOLATILE_VOID(CTypeQualifiers.ATOMIC_VOLATILE),
  CONST_VOLATILE_VOID(CTypeQualifiers.CONST_VOLATILE),
  ATOMIC_CONST_VOLATILE_VOID(CTypeQualifiers.ATOMIC_CONST_VOLATILE),
  ;

  private static ImmutableMap<CTypeQualifiers, CVoidType> LOOKUP =
      Arrays.stream(CVoidType.values())
          .collect(Maps.toImmutableEnumMap(t -> t.getQualifiers(), t -> t));

  public static CVoidType create(CTypeQualifiers qualifiers) {
    return checkNotNull(LOOKUP.get(qualifiers));
  }

  private final CTypeQualifiers qualifiers;

  private CVoidType(CTypeQualifiers pQualifiers) {
    qualifiers = checkNotNull(pQualifiers);
  }

  @Override
  public CTypeQualifiers getQualifiers() {
    return qualifiers;
  }

  @Override
  public boolean isIncomplete() {
    return true; // C standard § 6.2.5 (19)
  }

  @Override
  public boolean hasKnownConstantSize() {
    // C standards says "false" because it is incomplete, but GCC allows sizeof(void) as an
    // extension, so we return "true" to signal that its size can be computed.
    return true;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return toASTString("");
  }

  @Override
  public String toASTString(String pDeclarator) {
    return qualifiers.toASTStringPrefix()
        + "void"
        + (pDeclarator.isEmpty() ? "" : " " + pDeclarator);
  }

  @Override
  public CVoidType getCanonicalType() {
    return this;
  }

  @Override
  public CVoidType getCanonicalType(CTypeQualifiers pQualifiersToAdd) {
    return create(CTypeQualifiers.union(qualifiers, pQualifiersToAdd));
  }

  @Override
  @InlineMe(
      replacement = "CVoidType.create(pNewQualifiers)",
      imports = "org.sosy_lab.cpachecker.cfa.types.c.CVoidType")
  public CVoidType withQualifiersSetTo(CTypeQualifiers pNewQualifiers) {
    return create(pNewQualifiers);
  }
}
