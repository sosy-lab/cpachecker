// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public final class CEnumerator extends AbstractSimpleDeclaration implements CSimpleDeclaration {

  private static final long serialVersionUID = -2526725372840523651L;

  private final long value;
  private @Nullable CEnumType enumType;
  private final String qualifiedName;

  public CEnumerator(
      final FileLocation pFileLocation,
      final String pName,
      final String pQualifiedName,
      final long pValue) {
    super(pFileLocation, pName);

    checkNotNull(pName);
    value = pValue;
    qualifiedName = checkNotNull(pQualifiedName);
  }

  /** Get the enum that declared this enumerator. */
  public CEnumType getEnum() {
    return enumType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CEnumerator other
        && super.equals(obj)
        && value == other.value
        && qualifiedName.equals(other.qualifiedName);
    // do not compare the enumType, comparing it with == is wrong because types which
    // are the same but not identical would lead to wrong results
    // comparing it with equals is no good choice, too. This would lead to a stack
    // overflow
    //  && (enumType == other.enumType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, enumType, qualifiedName) * 31 + super.hashCode();
  }

  /** This method should be called only during parsing. */
  public void setEnum(CEnumType pEnumType) {
    checkState(enumType == null);
    enumType = checkNotNull(pEnumType);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CSimpleType getType() {
    return enumType.getCompatibleType();
  }

  public long getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    return getQualifiedName().replace("::", "__") + " = " + value;
  }

  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
