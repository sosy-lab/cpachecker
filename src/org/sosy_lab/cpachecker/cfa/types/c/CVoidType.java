// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.InlineMe;

/**
 * This class represents the type "void". It does not allow any modifiers and has only a single
 * instance.
 */
public enum CVoidType implements CType {
  VOID(CTypeQualifiers.NONE),
  CONST_VOID(CTypeQualifiers.CONST),
  VOLATILE_VOID(CTypeQualifiers.VOLATILE),
  CONST_VOLATILE_VOID(CTypeQualifiers.CONST_VOLATILE),
  ;

  private static CVoidType create(boolean pIsConst, boolean pIsVolatile) {
    if (pIsConst) {
      return pIsVolatile ? CONST_VOLATILE_VOID : CONST_VOID;
    } else {
      return pIsVolatile ? VOLATILE_VOID : VOID;
    }
  }

  public static CVoidType create(CTypeQualifiers pQualifiers) {
    return create(pQualifiers.containsConst(), pQualifiers.containsVolatile());
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
